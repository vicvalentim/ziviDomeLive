class ConfigLoader {
  PApplet pApplet;
  TextureManager textureManager;
  private PShape skySphere;
  private PImage skyTexture;

  ConfigLoader(PApplet pApplet, TextureManager textureManager) {
    this.pApplet = pApplet;
    this.textureManager = textureManager;
  }
  
  // Carrega a configuração a partir do arquivo "solar.json" (na pasta data)
  ArrayList<Planet> loadConfiguration() {
    ArrayList<Planet> planets = new ArrayList<Planet>();
    JSONObject config = pApplet.loadJSONObject("solar.json");
    
    // Carrega os planetas
    JSONArray planetsArray = config.getJSONArray("planets");
    pApplet.println("Loading " + planetsArray.size() + " planets from JSON.");
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
      
      if (name.equals("Sol")) {
        // Cria o Sol com a textura
        PImage sunTexture = textureManager.getTexture("2k_sun.jpg");
        Planet sol = new Planet(pApplet, SOL_MASS, SUN_VISUAL_RADIUS,
                                new PVector(), new PVector(), pApplet.color(255, 255, 0), "Sol",
                                rotationPeriod, orbitInclination, 0, sunTexture);
        planets.add(sol);
        pApplet.println("Created Sol.");
      } else {
        Planet p = createPlanet(mass, distance, col, name, ratio, rotationPeriod, orbitInclination, axisTilt);
        planets.add(p);
        pApplet.println("Created planet: " + name + " with distance (in px): " + (distance * PIXELS_PER_AU));
        pApplet.println("  Initial draw position: " + p.getDrawPosition());
      }
    }
    
    // Carrega os dados das luas
    JSONArray moonsArray = config.getJSONArray("moons");
    pApplet.println("Loading " + moonsArray.size() + " moons from JSON.");
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
      
      Planet parentPlanet = getPlanetByName(planetName, planets);
      if (parentPlanet != null) {
        addMoonToPlanet(parentPlanet, moonName, moonSizeRatio, orbitFactor, inclination, eccentricity, argumentPeriapsis, alignWithPlanetAxis);
        pApplet.println("Added moon \"" + moonName + "\" to planet \"" + planetName + "\".");
      } else {
        pApplet.println("Warning: Planet \"" + planetName + "\" not found for moon \"" + moonName + "\".");
      }
    }
    
    // Configura o sky sphere com a textura de fundo
    skyTexture = textureManager.getTexture("eso0932a.jpg");
    skySphere = pApplet.createShape(PConstants.SPHERE, 1);
    skySphere.setTexture(skyTexture);
    skySphere.setStroke(false);
    skySphere.setFill(pApplet.color(255));
    pApplet.println("Sky sphere created.");
    
    return planets;
  }
  
  private Planet createPlanet(float mass, float distanceAU, color col, String name, float planetRatio, float rotationPeriod, float orbitInclination, float axisTilt) {
    float r_px = distanceAU * PIXELS_PER_AU;
    float v_AU_per_year = pApplet.sqrt(G_AU / distanceAU);
    float v_AU_per_day  = v_AU_per_year / 365.25f;
    float orbitalVelocity = v_AU_per_day * PIXELS_PER_AU;
    
    PVector pos = new PVector(r_px, 0, 0);
    PVector vel = new PVector(0, 0, -orbitalVelocity);
    
    PMatrix3D rotationMatrix = new PMatrix3D();
    rotationMatrix.rotateX(orbitInclination);
    pos = rotationMatrix.mult(pos, null);
    vel = rotationMatrix.mult(vel, null);
    
    float baseRadius = SUN_VISUAL_RADIUS * planetRatio;
    float drawRadius = baseRadius;  // Pode incluir amplificação se necessário
    
    Planet planet = new Planet(pApplet, mass, drawRadius, pos, vel, col, name, rotationPeriod, orbitInclination, axisTilt, null);
    planet.orbitRadius = r_px;
    planet.orbitInclination = orbitInclination;
    planet.anomaly = 0;
    return planet;
  }
  
  private void addMoonToPlanet(Planet planet, String moonName, float moonSizeRatio, float orbitFactor, float inclination, float eccentricity, float argumentPeriapsis, boolean alignWithPlanetAxis) {
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
    
    planet.addMoon(new Moon(
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
    ));
  }
  
  private Planet getPlanetByName(String name, ArrayList<Planet> planets) {
    for (Planet p : planets) {
      if (p.name.equals(name)) return p;
    }
    return null;
  }
  
  // Getter para o sky sphere, para ser utilizado pelo Renderer
  public PShape getSkySphere() {
    return skySphere;
  }

  public void dispose() {
    // Limpa referências ao skySphere e à skyTexture
    skySphere = null;
    skyTexture = null;
  }
}
