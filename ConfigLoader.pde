// ConfigLoader.pde

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.File;

class ConfigLoader {
  private final PApplet pApplet;
  private final TextureManager textureManager;
  private final SimParams simParams;
  private PShape skySphere;
  private PImage skyTexture;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final HashMap<String,String> textureByName = new HashMap<>();

  private float sunRadiusAU = 1.0f;
  private float sunMassSolar;

  // NÃO final para permitir recarregar
  private JSONObject solarCfg;       
  private JSONObject jsonSun;        
  private JSONArray  jsonPlanets;    
  private JSONArray  jsonMoons;      

  ConfigLoader(PApplet pApplet, TextureManager textureManager, SimParams simParams) {
    this.pApplet        = pApplet;
    this.textureManager = textureManager;
    this.simParams      = simParams;

    scanTextureFolder("textures");

    // ---- Lê o JSON UMA ÚNICA VEZ ----
    reloadJson();
  }

  /** (Re)carrega todo o JSON para as quatro variáveis */
  void reloadJson() {
    lock.writeLock().lock();
    try {
      this.solarCfg    = pApplet.loadJSONObject("solar2.json");
      this.jsonSun     = requireJSONObject(solarCfg, "sun");
      this.sunMassSolar = requireFloat     (jsonSun, "massSolar");
      this.jsonPlanets = requireJSONArray (solarCfg, "planets");
      this.jsonMoons   = requireJSONArray (solarCfg, "moons");
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
  * Varre o diretório data/subfolder e mapeia
  * tudo que for 2k_<nome>.(jpg|png) → key = nome (lowercase)
  */
  private void scanTextureFolder(String subfolder) {
    File dir = new File(dataPath(subfolder));  // dataPath() mapeia para a pasta /data do sketch
    String[] files = dir.list();
    if (files == null) return;
    for (String f : files) {
      if (f.startsWith("2k_") && (f.endsWith(".jpg") || f.endsWith(".png"))) {
        // guarda apenas o nome do arquivo, sem o "textures/" na frente
        String key = f.substring(3, f.lastIndexOf('.')).toLowerCase();
        textureByName.put(key, f);
      }
    }
  }

  /** Retorna a PImage para planeta ou lua pelo nome (case-insensitive) */
  private PImage lookupTexture(String bodyName) {
    String key      = bodyName.toLowerCase();
    String filename = textureByName.get(key);
    // o TextureManager por baixo usa algo como loadImage(TEXTURE_PATH + filename)
    return (filename != null) ? textureManager.getTexture(filename) : null;
  }

  // ─────────────────────────────────────────────────────────────────
  // Carrega o Sol
  // ─────────────────────────────────────────────────────────────────
  Sun loadSun() {
    lock.readLock().lock();
    try {
      try {
        // 1) lê do JSON
        float massSolar     = requireFloat(jsonSun, "massSolar");
        this.sunMassSolar   = massSolar;          // ← garantido aqui!
        float radiusAU      = requireFloat(jsonSun, "radiusAU");
        float rotPeriodDays = requireFloat(jsonSun, "rotationPeriodDays");
        JSONArray cn        = requireJSONArray(jsonSun, "colorNorm");
        int displayColor    = pApplet.color(
          cn.getFloat(0)*255f,
          cn.getFloat(1)*255f,
          cn.getFloat(2)*255f
        );

        this.sunRadiusAU = radiusAU;
        float radiusPx   = sunRadiusPx(simParams);
        PImage tex       = lookupTexture("sun");

        return new Sun(
          pApplet,
          simParams,
          radiusPx,
          massSolar,
          radiusAU,
          rotPeriodDays,
          new PVector(0, 0, 0),
          displayColor,
          tex
        );
      } catch (Exception e) {
        pApplet.println("[ConfigLoader] Erro ao carregar 'sun': " + e.getMessage());
        return null;
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // Carrega planetas
  // ─────────────────────────────────────────────────────────────────
  public ArrayList<Planet> loadPlanets() {
    ArrayList<Planet> out = new ArrayList<>();
    for (int k = 0; k < jsonPlanets.size(); k++) {
      JSONObject pd = jsonPlanets.getJSONObject(k);
      try {
        // ——— campos básicos ——————————————————————————————
        String name               = requireString (pd, "name");
        float  massSolar          = requireFloat  (pd, "massSolar");
        float  radiusAU           = requireFloat  (pd, "radiusAU");
        float  rotationPeriodDays = requireFloat  (pd, "rotationPeriodDays");
        float  orbitalPeriodDays  = requireFloat  (pd, "orbitalPeriodDays");

        // ——— elementos orbitais ———————————————————————————
        float perihelionAU        = requireFloat  (pd, "perihelionAU");
        float aphelionAU          = requireFloat  (pd, "aphelionAU");
        float eccentricity        = requireFloat  (pd, "eccentricity");
        float Ω                   = requireFloat  (pd, "longitudeAscendingNodeRad");
        float iRad                = requireFloat  (pd, "orbitInclinationRad");
        float axisTiltRad         = requireFloat  (pd, "axisTiltRad");
        float ω                   = requireFloat  (pd, "argumentOfPeriapsisRad");
        float M0                  = requireFloat  (pd, "meanAnomalyRad");
        float a                   = requireFloat  (pd, "semiMajorAxisAU");

        // ——— condição inicial via initialState com massa do Sol —————————————————
        PVector rPlane = new PVector();
        PVector vPlane = new PVector();
        // usa μ = G_DAY * sunMassSolar
        initialState(
          a,
          eccentricity,
          M0,
          sunMassSolar,
          rPlane,
          vPlane
        );

        // ——— aplica pipeline Ω → i → ω para referencial global (Y-up) —————————
        PVector rGlobal = applyOrbitalPlaneToGlobal(rPlane, Ω, iRad, ω);
        PVector vGlobal = applyOrbitalPlaneToGlobal(vPlane, Ω, iRad, ω);

        // ——— cor & textura ————————————————————————————————
        JSONArray cn     = requireJSONArray(pd, "colorNorm");
        int displayColor = pApplet.color(
          cn.getFloat(0)*255,
          cn.getFloat(1)*255,
          cn.getFloat(2)*255
        );
        PImage tex  = lookupTexture(name);
        PImage ring = lookupTexture(name + "_ring_alpha");

        // ——— monta o objeto Planet ——————————————————————————
        Planet planet = new Planet(
          pApplet, simParams,
          massSolar,
          radiusAU,
          sunRadiusAU,
          rotationPeriodDays,
          rGlobal, vGlobal,
          displayColor,
          name,
          tex, ring,
          iRad, axisTiltRad,
          perihelionAU, aphelionAU, eccentricity,
          ω, Ω, M0,
          orbitalPeriodDays,
          vGlobal.mag(),  // velocidade média em AU/dia
          a                // semiMajorAxisAU
        );

        out.add(planet);

      } catch (Exception e) {
        pApplet.println("[ConfigLoader] Skip planet #" + k + ": " + e.getMessage());
      }
    }
    return out;
  }

  // ─────────────────────────────────────────────────────────────────
  // Carrega luas
  // ─────────────────────────────────────────────────────────────────
  public void loadMoons(List<Planet> planets) {
    for (int k = 0; k < jsonMoons.size(); k++) {
      JSONObject md = jsonMoons.getJSONObject(k);
      try {
        String hostName = requireString(md, "planetName");
        Planet host     = getPlanetByName(hostName, planets);
        if (host == null) {
          pApplet.println("[ConfigLoader] Host not found: " + hostName);
          continue;
        }
        pApplet.println("[ConfigLoader] Carregando lua #" + k + ": " +
                        requireString(md, "moonName"));

        // ── parâmetros da Lua ─────────────────────────────────────────
        float massSolar           = requireFloat(md, "massSolar");
        float radiusAU            = requireFloat(md, "radiusAU");
        float rotationPeriodDays  = requireFloat(md, "rotationPeriodDays");
        float a                   = requireFloat(md, "semiMajorAxisAU");
        float perihelionAU        = requireFloat(md, "perihelionAU");
        float aphelionAU          = requireFloat(md, "aphelionAU");
        float eccentricity        = requireFloat(md, "eccentricity");
        float iRad                = requireFloat(md, "orbitInclinationRad");
        float ω                   = requireFloat(md, "argumentOfPeriapsisRad");
        float Ω                   = requireFloat(md, "longitudeAscendingNodeRad");
        float M0                  = requireFloat(md, "meanAnomalyRad");
        boolean alignWithAxis     = md.hasKey("alignWithPlanetAxis")
                                  && md.getBoolean("alignWithPlanetAxis");
        String moonName           = requireString(md, "moonName");

        // ── condição inicial via initialState COM massa do host ─────────
        PVector rPlane = new PVector();
        PVector vPlane = new PVector();
        // initialState(a, e, M0, massFocus, outPos, outVel)
        initialState(
          a,
          eccentricity,
          M0,
          host.getMassSolar(),  // massa do planeta-pai
          rPlane,
          vPlane
        );

        // ── aplica rotações Ω→i→ω para referencial global (Y-up) ──────────
        PVector rGlobal = applyOrbitalPlaneToGlobal(rPlane, Ω, iRad, ω);
        PVector vGlobal = applyOrbitalPlaneToGlobal(vPlane, Ω, iRad, ω);

        // ── desloca pelo host (posição + velocidade) ────────────────────
        rGlobal.add(host.getPositionAU());
        vGlobal.add(host.getVelocityAU());

        // ── cor & textura ───────────────────────────────────────────────
        JSONArray cn     = requireJSONArray(md, "colorNorm");
        int displayColor = pApplet.color(
          cn.getFloat(0)*255,
          cn.getFloat(1)*255,
          cn.getFloat(2)*255
        );
        PImage texMoon   = lookupTexture(moonName);

        // ── instancia a lua ────────────────────────────────────────────
        Moon moon = new Moon(
          pApplet, simParams,
          massSolar,
          radiusAU,
          rotationPeriodDays,
          a, perihelionAU, aphelionAU, eccentricity,
          rGlobal,        // posição absoluta
          vGlobal,        // velocidade absoluta
          moonName,
          displayColor,
          texMoon,
          host,
          iRad, ω, Ω, M0,
          alignWithAxis
        );

        // ── escala visual e vincula ao planeta ────────────────────────
        moon.setRadiusPx(radiusAU / host.getRadiusAU()
                        * host.getRadiusPx());
        host.addMoon(moon);
        pApplet.println("[ConfigLoader]   -> associada a " + host.getName() + ": " + moon.getName());
        

      } catch (Exception e) {
        pApplet.println("[ConfigLoader] Skip moon #" + k + ": " + e.getMessage());
      }
    }
  }

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
      if (p.getName().equalsIgnoreCase(name)) return p;
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
      if (skyTexture != null) shaderManager.setTexture("sky_hdri", skyTexture);
      for (String key : textureByName.keySet()) {
        PImage tex = textureManager.getTexture(textureByName.get(key));
        if (tex != null) shaderManager.setTexture("planet", tex);
      }
      PImage ring = textureManager.getTexture("2k_saturn_ring_alpha.png");
      if (ring != null) shaderManager.setTexture("rings", ring);
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

  // ─────────────────────────────────────────────────────────────────
  // Helpers de extração com validação
  // ─────────────────────────────────────────────────────────────────
  private JSONObject requireJSONObject(JSONObject obj, String key) {
    if (!obj.hasKey(key)) throw new RuntimeException("Missing '"+ key + "'");
    return obj.getJSONObject(key);
  }
  private JSONArray requireJSONArray(JSONObject obj, String key) {
    if (!obj.hasKey(key)) throw new RuntimeException("Missing '"+ key + "'");
    return obj.getJSONArray(key);
  }
  private String requireString(JSONObject obj, String key) {
    if (!obj.hasKey(key)) throw new RuntimeException("Missing '"+ key + "'");
    return obj.getString(key);
  }
  private float requireFloat(JSONObject obj, String key) {
    if (!obj.hasKey(key)) throw new RuntimeException("Missing '"+ key + "'");
    return obj.getFloat(key);
  }
}
