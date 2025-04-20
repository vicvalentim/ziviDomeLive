import java.util.concurrent.locks.ReentrantReadWriteLock;
import processing.core.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * ConfigLoader — lê o JSON “solar2.json” já com todos os parâmetros físicos em AU e dias,
 * instanciando apenas dados físicos. Nenhum cálculo de pixel aqui.
 */
class ConfigLoader {
  private final PApplet pApplet;
  private final TextureManager textureManager;
  private final SimParams simParams;
  private PShape skySphere;
  private PImage skyTexture;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final HashMap<String,String> planetTextureMap = new HashMap<>();

  private float sunRadiusAU = 1.0f; // Garantia inicial

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

  /** Carrega apenas os dados físicos do Sol (unidades naturais). */
  Sun loadSun() {
    lock.readLock().lock();
    try {
      JSONObject sunObj = pApplet.loadJSONObject("solar2.json")
                              .getJSONObject("sun");

      String name           = sunObj.getString("name");
      float massSolar       = sunObj.getFloat("massSolar");
      float radiusAU        = sunObj.getFloat("radiusAU");
      float rotPeriodDays   = sunObj.getFloat("rotationPeriodDays");
      JSONArray cn          = sunObj.getJSONArray("colorNorm");
      int displayColor      = pApplet.color(
        cn.getFloat(0)*255f,
        cn.getFloat(1)*255f,
        cn.getFloat(2)*255f
      );

      this.sunRadiusAU = radiusAU; // Importante para planetas

      // Raio visual em px: SUN_VISUAL_RADIUS é "tamanho padrão" do Sol
      float radiusPx = SUN_VISUAL_RADIUS * simParams.globalScale;

      PImage texture = planetTextureMap.containsKey(name)
        ? textureManager.getTexture(planetTextureMap.get(name))
        : null;

      return new Sun(
        pApplet,
        radiusPx,
        massSolar,
        radiusAU,
        rotPeriodDays,
        new PVector(0,0,0),
        displayColor,
        texture
      );
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Carrega todos os planetas e luas, com todos os parâmetros físicos. */
  ArrayList<Planet> loadConfiguration() {
    lock.writeLock().lock();
    try {
      JSONObject config     = pApplet.loadJSONObject("solar2.json");
      ArrayList<Planet> pls = new ArrayList<>();

      // 1) Planetas
      JSONArray planetsArray = config.getJSONArray("planets");
      for (int i = 0; i < planetsArray.size(); i++) {
        JSONObject pd        = planetsArray.getJSONObject(i);
        String   name        = pd.getString("name");
        float    massSolar   = pd.getFloat("massSolar");
        float    distanceAU  = pd.getFloat("distanceAU");
        float    radiusAU    = pd.getFloat("radiusAU");
        float    rotPeriod   = pd.getFloat("rotationPeriodDays");
        float    orbPeriod   = pd.getFloat("orbitalPeriodDays");
        float    incRad      = pd.getFloat("orbitInclinationRad");
        float    tiltRad     = pd.getFloat("axisTiltRad");
        float    periAU      = pd.getFloat("perihelionAU");
        float    apheAU      = pd.getFloat("aphelionAU");
        float    ecc         = pd.getFloat("eccentricity");
        float    velAUperDay = pd.getFloat("orbitalVelocityAUperDay");
        JSONArray cn         = pd.getJSONArray("colorNorm");
        int      displayColor = pApplet.color(
                          cn.getFloat(0)*255,
                          cn.getFloat(1)*255,
                          cn.getFloat(2)*255
                        );

        // Texturas
        PImage tex    = planetTextureMap.containsKey(name)
                        ? textureManager.getTexture(planetTextureMap.get(name))
                        : null;
        PImage ringTex= "Saturn".equals(name)
                        ? textureManager.getTexture("2k_saturn_ring_alpha.png")
                        : null;

        PVector posAU = new PVector(distanceAU, 0, 0);
        PVector velAU = new PVector(0, 0, -velAUperDay);

        Planet planet = new Planet(
          pApplet,
          simParams,
          massSolar,
          radiusAU,
          sunRadiusAU, // <<<<<<<<<< ADICIONADO
          rotPeriod,
          posAU,
          velAU,
          displayColor,
          name,
          tex,
          ringTex,
          incRad,
          tiltRad,
          periAU,
          apheAU,
          ecc,
          orbPeriod,
          velAUperDay
        );

        pls.add(planet);
      }

      // 2) Luas
      JSONArray moonsArray = config.getJSONArray("moons");
      for (int i = 0; i < moonsArray.size(); i++) {
        JSONObject md       = moonsArray.getJSONObject(i);
        String   parentName = md.getString("planetName");
        Planet   parent     = getPlanetByName(parentName, pls);
        if (parent == null) continue;

        String   moonName    = md.getString("moonName");
        float    massSolar   = md.hasKey("massSolar") 
                              ? md.getFloat("massSolar") 
                              : 0f;
        float    radiusRatio = md.getFloat("radiusRatio");
        float    radiusAU    = radiusRatio * parent.getRadiusAU();
        float    periAU      = md.getFloat("perihelionAU");
        float    apheAU      = md.getFloat("aphelionAU");
        float    ecc         = md.getFloat("eccentricity");
        float    incRad      = md.getFloat("orbitInclinationRad");
        float    argPeriRad  = md.getFloat("argumentPeriapsisRad");
        boolean  alignAxis   = md.getBoolean("alignWithPlanetAxis");
        float    rotPeriod   = md.getFloat("rotationPeriodDays");
        float    orbPeriod   = md.getFloat("orbitalPeriodDays");
        float    velAUperDay = md.getFloat("orbitalVelocityAUperDay");
        JSONArray cm         = md.getJSONArray("colorNorm");
        int      displayColor= pApplet.color(
                                cm.getFloat(0)*255,
                                cm.getFloat(1)*255,
                                cm.getFloat(2)*255
                              );

        PVector posAU = new PVector(md.getFloat("orbitDistanceAU"), 0, 0);
        PVector velAU = new PVector(0, 0, -velAUperDay);

        Moon moon = new Moon(
          pApplet,
          simParams,
          massSolar,
          radiusAU,
          sunRadiusAU, // <<<<<<<<<< ADICIONADO
          rotPeriod,
          posAU,
          velAU,
          moonName,
          displayColor,
          textureManager.getTexture(planetTextureMap.get("Moon")),
          parent,
          incRad,
          argPeriRad,
          alignAxis,
          periAU,
          apheAU,
          ecc
        );

        parent.addMoon(moon);
      }

      // 3) Sky sphere
      skyTexture = textureManager.getTexture("8k_stars_milky_way.jpg");
      skySphere  = pApplet.createShape(SPHERE, 1);
      skySphere.setTexture(skyTexture);
      skySphere.setStroke(false);
      skySphere.setFill(pApplet.color(255));

      return pls;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private Planet getPlanetByName(String name, ArrayList<Planet> planets) {
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
