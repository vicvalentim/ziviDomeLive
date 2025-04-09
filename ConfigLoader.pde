import java.util.concurrent.locks.ReentrantReadWriteLock;

class ConfigLoader {
  PApplet pApplet;
  TextureManager textureManager;
  private PShape skySphere;
  private PImage skyTexture;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  private final HashMap<String, String> planetTextureMap = new HashMap<>();

  ConfigLoader(PApplet pApplet, TextureManager textureManager) {
    this.pApplet = pApplet;
    this.textureManager = textureManager;
    initializeTextureMap();
  }

  private void initializeTextureMap() {
    planetTextureMap.put("Sun", "2k_sun.jpg");
    planetTextureMap.put("Mercury", "2k_mercury.jpg");
    planetTextureMap.put("Venus", "2k_venus_surface.jpg");
    planetTextureMap.put("Earth", "2k_earth_daymap.jpg");
    planetTextureMap.put("Mars", "2k_mars.jpg");
    planetTextureMap.put("Jupiter", "2k_jupiter.jpg");
    planetTextureMap.put("Saturn", "2k_saturn.jpg");
    planetTextureMap.put("Uranus", "2k_uranus.jpg");
    planetTextureMap.put("Neptune", "2k_neptune.jpg");
    planetTextureMap.put("Moon", "2k_moon.jpg");
  }

  // Carrega o Sol a partir do arquivo JSON
  Sun loadSun() {
    lock.readLock().lock();
    try {
      JSONObject config = pApplet.loadJSONObject("solar.json");
      JSONObject sunObj = config.getJSONObject("sun");

      String name = sunObj.getString("name");
      float mass = sunObj.getFloat("mass"); // ← Valor da massa do JSON
      JSONArray colArray = sunObj.getJSONArray("color");
      color col = pApplet.color(colArray.getInt(0), colArray.getInt(1), colArray.getInt(2));
      float ratio = sunObj.getFloat("ratio");
      float rotationPeriod = sunObj.getFloat("rotationPeriod");
      float axisTilt = radians(sunObj.getFloat("axisTilt"));

      float radius = SUN_VISUAL_RADIUS * ratio;

      PImage texture = null;
      if (planetTextureMap.containsKey(name)) {
        texture = textureManager.getTexture(planetTextureMap.get(name));
      }

      // Usa o novo construtor com massa
      return new Sun(pApplet, radius, mass, new PVector(0, 0, 0), col, texture);

    } finally {
      lock.readLock().unlock();
    }
  }

  ArrayList<Planet> loadConfiguration() {
    lock.writeLock().lock();
    try {
      ArrayList<Planet> planets = new ArrayList<Planet>();
      JSONObject config = pApplet.loadJSONObject("solar.json");
      JSONArray planetsArray = config.getJSONArray("planets");

      for (int i = 0; i < planetsArray.size(); i++) {
        JSONObject pd = planetsArray.getJSONObject(i);
        float mass = pd.getFloat("mass");
        float distance = pd.getFloat("distance");
        JSONArray colArray = pd.getJSONArray("color");
        color col = pApplet.color(colArray.getInt(0), colArray.getInt(1), colArray.getInt(2));
        String name = pd.getString("name");
        float ratio = pd.getFloat("ratio");
        float rotationPeriod = pd.getFloat("rotationPeriod");
        float orbitInclination = radians(pd.getFloat("orbitInclination"));
        float axisTilt = radians(pd.getFloat("axisTilt"));

        PImage texture = null;
        if (planetTextureMap.containsKey(name)) {
          texture = textureManager.getTexture(planetTextureMap.get(name));
        }

        PImage ringTexture = null;
        if (name.equals("Saturn")) {
          ringTexture = textureManager.getTexture("2k_saturn_ring_alpha.png");
        }

        // --- Correção: aplica apenas a inclinação orbital (sem rotateX(HALF_PI)) ---
        PVector pos = new PVector(distance * PIXELS_PER_AU, 0, 0);
        float v_AU = pApplet.sqrt(G_AU / distance);
        PVector vel = new PVector(0, 0, -v_AU * PIXELS_PER_AU / 365.25f);

        PMatrix3D rotationMatrix = new PMatrix3D();
        rotationMatrix.rotateX(orbitInclination);  // aplica só a inclinação orbital
        pos = rotationMatrix.mult(pos, null);
        vel = rotationMatrix.mult(vel, null);

        Planet p = new Planet(
          pApplet,
          mass,
          SUN_VISUAL_RADIUS * ratio,
          pos,
          vel,
          col,
          name,
          rotationPeriod,
          orbitInclination,
          axisTilt,
          texture,
          ringTexture
        );
        planets.add(p);
      }

      // --- Carrega luas e aplica órbitas ---
      JSONArray moonsArray = config.getJSONArray("moons");
      for (int i = 0; i < moonsArray.size(); i++) {
        JSONObject md = moonsArray.getJSONObject(i);
        String planetName = md.getString("planetName");
        String moonName = md.getString("moonName");
        float moonSizeRatio = md.getFloat("moonSizeRatio");
        float orbitFactor = md.getFloat("orbitFactor");
        float inclination = radians(md.getFloat("inclination"));
        float eccentricity = md.getFloat("eccentricity");
        float argumentPeriapsis = radians(md.getFloat("argumentPeriapsis"));
        boolean alignWithPlanetAxis = md.getBoolean("alignWithPlanetAxis");

        Planet parent = getPlanetByName(planetName, planets);
        if (parent != null) {
          addMoonToPlanet(parent, moonName, moonSizeRatio, orbitFactor,
                          inclination, eccentricity, argumentPeriapsis, alignWithPlanetAxis);
        }
      }

      // --- Cria sky sphere ---
      skyTexture = textureManager.getTexture("eso0932a.jpg");
      skySphere = pApplet.createShape(PConstants.SPHERE, 1);
      skySphere.setTexture(skyTexture);
      skySphere.setStroke(false);
      skySphere.setFill(pApplet.color(255));

      return planets;
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Adiciona uma lua a um planeta
  private void addMoonToPlanet(Planet planet, String moonName, float moonSizeRatio, float orbitFactor,
                                float inclination, float eccentricity, float argumentPeriapsis,
                                boolean alignWithPlanetAxis) {
    float factorMultiplier;
    if (orbitFactor <= 7.0f) {
      factorMultiplier = 1.5f;
    } else if (orbitFactor <= 25.0f) {
      factorMultiplier = 1.2f;
    } else if (orbitFactor <= 70.0f) {
      factorMultiplier = 0.65f;
    } else {
      factorMultiplier = 0.15f;
    }
    orbitFactor *= factorMultiplier;

    float orbitDistance = planet.radius * (1 + (orbitFactor / MOON_ORBIT_CALIBRATION));
    float r_AU = orbitDistance / PIXELS_PER_AU;
    float v_AU = pApplet.sqrt(G_AU * planet.mass / r_AU);
    float v_pixels = v_AU / 365.25f * PIXELS_PER_AU;

    PVector moonPos = new PVector(orbitDistance, 0, 0);
    PVector moonVel = new PVector(0, 0, -v_pixels);

    Moon moon = new Moon(
        pApplet,
        1e-7f, moonSizeRatio, orbitFactor,
        moonPos, moonVel,
        pApplet.color(200, 200, 200),
        moonName, planet,
        inclination, eccentricity, argumentPeriapsis,
        alignWithPlanetAxis,
        PIXELS_PER_AU,
        G_AU,
        MOON_ORBIT_CALIBRATION
    );

    planet.addMoon(moon);
  }

  private Planet getPlanetByName(String name, ArrayList<Planet> planets) {
    for (Planet p : planets) {
      if (p.name.equals(name)) return p;
    }
    return null;
  }

  public PShape getSkySphere() {
    lock.readLock().lock();
    try {
      return skySphere;
    } finally {
      lock.readLock().unlock();
    }
  }

  public void dispose() {
    lock.writeLock().lock();
    try {
      skySphere = null;
      skyTexture = null;
    } finally {
      lock.writeLock().unlock();
    }
  }
}
