import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ConfigLoader {
  private final PApplet pApplet;
  private final TextureManager textureManager;
  private final SimParams simParams;
  private PShape skySphere;
  private PImage skyTexture;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final HashMap<String,String> planetTextureMap = new HashMap<>();

  private float sunRadiusAU = 1.0f;

  ConfigLoader(PApplet pApplet, TextureManager textureManager, SimParams simParams) {
    this.pApplet = pApplet;
    this.textureManager = textureManager;
    this.simParams = simParams;
    initializeTextureMap();
  }

  private void initializeTextureMap() {
    planetTextureMap.put("Sun",     "2k_sun.jpg");
    planetTextureMap.put("Mercury", "2k_mercury.jpg");
    planetTextureMap.put("Venus",   "2k_venus_surface.jpg");
    planetTextureMap.put("Earth",   "2k_earth_daymap.jpg");
    planetTextureMap.put("Mars",    "2k_mars.jpg");
    planetTextureMap.put("Jupiter", "2k_jupiter.jpg");
    planetTextureMap.put("Saturn",  "2k_saturn.jpg");
    planetTextureMap.put("Uranus",  "2k_uranus.jpg");
    planetTextureMap.put("Neptune", "2k_neptune.jpg");
    planetTextureMap.put("Moon",    "2k_moon.jpg");
  }

  /** Carrega apenas o Sol. */
  Sun loadSun() {
    lock.readLock().lock();
    try {
      JSONObject sunObj = pApplet.loadJSONObject("solar2.json")
                                 .getJSONObject("sun");

      float massSolar     = sunObj.getFloat("massSolar");
      float radiusAU      = sunObj.getFloat("radiusAU");
      float rotPeriodDays = sunObj.getFloat("rotationPeriodDays");
      JSONArray cn        = sunObj.getJSONArray("colorNorm");
      int displayColor    = pApplet.color(
        cn.getFloat(0)*255f,
        cn.getFloat(1)*255f,
        cn.getFloat(2)*255f
      );

      this.sunRadiusAU = radiusAU;

      float radiusPx = SUN_VISUAL_RADIUS * simParams.globalScale;
      PImage texture = textureManager.getTexture(planetTextureMap.get("Sun"));

      return new Sun(
        pApplet,
        radiusPx,
        massSolar,
        radiusAU,
        rotPeriodDays,
        new PVector(0, 0, 0),
        displayColor,
        texture
      );
    } finally {
      lock.readLock().unlock();
    }
  }

  // ─────────────────────────────────── Planets ────────────────────────────────── 

  /** Lê “planets” aplicando Kepler no plano XZ e depois Ω→i→ω. */
  public ArrayList<Planet> loadPlanets() {
      JSONObject cfg = pApplet.loadJSONObject("solar2.json");
      JSONArray arr   = cfg.getJSONArray("planets");
      ArrayList<Planet> out = new ArrayList<>();

      for (int k = 0; k < arr.size(); k++) {
        JSONObject pd = arr.getJSONObject(k);

        // básicos
        String name               = pd.getString("name");
        float  massSolar          = pd.getFloat("massSolar");
        float  radiusAU           = pd.getFloat("radiusAU");
        float  rotationPeriodDays = pd.getFloat("rotationPeriodDays");
        float  orbitalPeriodDays  = pd.getFloat("orbitalPeriodDays");

        // elementos
        float perihelionAU           = pd.getFloat("perihelionAU");
        float aphelionAU             = pd.getFloat("aphelionAU");
        float eccentricity           = pd.getFloat("eccentricity");
        float Ω                      = pd.getFloat("longitudeAscendingNodeRad");
        float iRad                   = pd.getFloat("orbitInclinationRad");
        float axisTiltRad            = pd.getFloat("axisTiltRad");
        float ω                      = pd.getFloat("argumentOfPeriapsisRad");
        float M0                     = pd.getFloat("meanAnomalyRad");
        float a                      = pd.getFloat("semiMajorAxisAU");

        // 1) resolve Kepler para E, depois posição/vel no plano XZ
        float E0   = solveKeplerEquation(M0, eccentricity);
        float cosE = PApplet.cos(E0), sinE = PApplet.sin(E0);
        float xOp  = a * (cosE - eccentricity);
        float zOp  = a * PApplet.sqrt(1 - eccentricity*eccentricity) * sinE;
        float μ    = G_DAY; 
        float n    = PApplet.sqrt(μ/(a*a*a));
        float vxOp = -n * a * sinE / (1 - eccentricity * cosE);
        float vzOp =  n * a * PApplet.sqrt(1-eccentricity*eccentricity) * cosE 
                          / (1 - eccentricity * cosE);

        PVector rPlane = new PVector(xOp, 0, zOp);
        PVector vPlane = new PVector(vxOp, 0, vzOp);

        // 2) transforma pro referencial global XZ → Y-up
        PVector rGlobal = applyOrbitalPlaneToGlobal(rPlane, Ω, iRad, ω);
        PVector vGlobal = applyOrbitalPlaneToGlobal(vPlane, Ω, iRad, ω);

        // 3) cor e texturas
        JSONArray cn = pd.getJSONArray("colorNorm");
        int displayColor = pApplet.color(
          cn.getFloat(0)*255,
          cn.getFloat(1)*255,
          cn.getFloat(2)*255
        );
        PImage tex    = textureManager.getTexture(planetTextureMap.get(name));
        PImage ring   = "Saturn".equals(name)
                      ? textureManager.getTexture("2k_saturn_ring_alpha.png")
                      : null;

        // 4) monta o objeto
        Planet planet = new Planet(
          pApplet, 
          simParams,
          massSolar,
          radiusAU,
          sunRadiusAU,
          rotationPeriodDays,
          rGlobal, 
          vGlobal,
          displayColor,
          name,
          tex, 
          ring,
          iRad,            // orbitInclinationRad
          axisTiltRad,
          perihelionAU, 
          aphelionAU, 
          eccentricity,
          ω,               // argumentOfPeriapsisRad
          Ω,               // longitudeAscendingNodeRad
          M0,              // meanAnomalyRad
          orbitalPeriodDays,
          vGlobal.mag(), // velocidade média AU/dia
          a                // semiMajorAxisAU
        );

        out.add(planet);
      }

      return out;
  }

 // ─────────────────────────────────── Moons ────────────────────────────────── 

  /** Lê “moons” aplicando o *mesmo* pipeline Ω→i→ω no plano XZ. */
  public void loadMoons(List<Planet> planets) {
      JSONObject cfg = pApplet.loadJSONObject("solar2.json");
      JSONArray  arr = cfg.getJSONArray("moons");

      for (int k = 0; k < arr.size(); k++) {
        JSONObject md = arr.getJSONObject(k);
        Planet host = getPlanetByName(md.getString("planetName"), planets);
        if (host == null) continue;

        // parâmetros
        float massSolar           = md.getFloat("massSolar");
        float radiusAU            = md.getFloat("radiusAU");
        float rotationPeriodDays  = md.getFloat("rotationPeriodDays");
        float a                   = md.getFloat("semiMajorAxisAU");
        float perihelionAU        = md.getFloat("perihelionAU");
        float aphelionAU          = md.getFloat("aphelionAU");
        float eccentricity        = md.getFloat("eccentricity");
        float iRad                = md.getFloat("orbitInclinationRad");
        float ω                   = md.getFloat("argumentOfPeriapsisRad");
        float Ω                   = md.getFloat("longitudeAscendingNodeRad");
        float M0                  = md.getFloat("meanAnomalyRad");
        boolean alignWithAxis     = md.getBoolean("alignWithPlanetAxis");
        String moonName           = md.getString("moonName");

        // Kepler no plano XZ
        float E0   = solveKeplerEquation(M0, eccentricity);
        float cosE = PApplet.cos(E0), sinE = PApplet.sin(E0);
        float xOp  = a * (cosE - eccentricity);
        float zOp  = a * PApplet.sqrt(1 - eccentricity*eccentricity) * sinE;
        float μ    = G_DAY * host.getMassSolar();
        float n    = PApplet.sqrt(μ/(a*a*a));
        float vxOp = -n * a * sinE / (1 - eccentricity * cosE);
        float vzOp =  n * a * PApplet.sqrt(1-eccentricity*eccentricity) * cosE 
                          / (1 - eccentricity * cosE);

        PVector rPlane = new PVector(xOp, 0, zOp);
        PVector vPlane = new PVector(vxOp, 0, vzOp);

        // transforma pro global XZ
        PVector rGlobal = applyOrbitalPlaneToGlobal(rPlane, Ω, iRad, ω);
        PVector vGlobal = applyOrbitalPlaneToGlobal(vPlane, Ω, iRad, ω);

        // cor e textura
        JSONArray cn = md.getJSONArray("colorNorm");
        int displayColor = pApplet.color(
          cn.getFloat(0)*255,
          cn.getFloat(1)*255,
          cn.getFloat(2)*255
        );
        PImage texMoon = ("Earth".equals(host.getName()) && "Moon".equals(moonName))
                        ? textureManager.getTexture("2k_moon.jpg")
                        : null;

        // instancia a lua
        Moon moon = new Moon(
          pApplet, simParams,
          massSolar,
          radiusAU,
          rotationPeriodDays,
          a,            // semiMajorAxisAU
          perihelionAU,
          aphelionAU,
          eccentricity,
          rGlobal,      // initialPosAU
          vGlobal,      // initialVelAU
          moonName,
          displayColor,
          texMoon,
          host,
          iRad, ω, Ω, M0,
          alignWithAxis
        );

        // escala + vincula
        moon.setRadiusPx(radiusAU / host.getRadiusAU() * host.getRadiusPx());
        host.addMoon(moon);
      }
  }

  /** Resolve a equação de Kepler M = E - e·sin(E) para E dado M e e. */
  private float solveKeplerEquation(float M, float e) {
      float E = M;
      for (int i = 0; i < 10; i++) {
          float f  = E - e * PApplet.sin(E) - M;
          float fp = 1 - e * PApplet.cos(E);
          float dE = -f / fp;
          E += dE;
          if (PApplet.abs(dE) < 1e-6f) break;
      }
      return E;
  }


  /** Orquestra o carregamento: planetas primeiro, depois luas. */
  public ArrayList<Planet> loadConfiguration() {
    lock.writeLock().lock();
    try {
      ArrayList<Planet> planets = loadPlanets();
      loadMoons(planets);

      skyTexture = textureManager.getTexture("8k_stars_milky_way.jpg");
      skySphere  = pApplet.createShape(SPHERE, 1);
      skySphere.setTexture(skyTexture);
      skySphere.setStroke(false);
      skySphere.setFill(pApplet.color(255));

      return planets;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private Planet getPlanetByName(String name, List<Planet> planets) {
    for (Planet p : planets) {
      if (p.getName().equals(name)) return p;
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

  public void sendTexturesToShaderManager(ShaderManager shaderManager) {
    lock.readLock().lock();
    try {
      if (skyTexture != null) {
        shaderManager.setTexture("sky_hdri", skyTexture);
      }
      for (String name : planetTextureMap.keySet()) {
        PImage tex = textureManager.getTexture(planetTextureMap.get(name));
        if (tex != null) {
          shaderManager.setTexture("planet", tex);
        }
      }
      PImage ring = textureManager.getTexture("2k_saturn_ring_alpha.png");
      if (ring != null) {
        shaderManager.setTexture("rings", ring);
      }
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
