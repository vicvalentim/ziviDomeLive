// ConfigLoader.pde

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.File;

class ConfigLoader {
  private final PApplet pApplet;
  private final SceneAssets assets;
  private PImage skyTexture;
  private final HashMap<String,String> textureByName = new HashMap<>();

  private double sunRadiusAU = 1.0;
  private double sunMassSolar;

  // NÃO final para permitir recarregar
  private JSONObject solarCfg;
  private JSONObject jsonSun;
  private JSONArray  jsonPlanets;
  private JSONArray  jsonMoons;

  ConfigLoader(PApplet pApplet, SceneAssets assets) {
    this.pApplet        = pApplet;
    this.assets          = assets;


    scanTextureFolder("textures");

    // ---- Lê o JSON UMA ÚNICA VEZ ----
    reloadJson();
  }

  /** (Re)carrega todo o JSON para as quatro variáveis */
  void reloadJson() {
    this.solarCfg    = pApplet.loadJSONObject("solar2.json");
    this.jsonSun     = requireJSONObject(solarCfg, "sun");
    this.sunMassSolar = requireDouble    (jsonSun, "massSolar");
    this.jsonPlanets = requireJSONArray (solarCfg, "planets");
    this.jsonMoons   = requireJSONArray (solarCfg, "moons");
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
    // SceneAssets resolve e mantém uma única referência por caminho.
    return (filename != null) ? assets.loadImage("textures/" + filename) : null;
  }

  // ─────────────────────────────────────────────────────────────────
  // Carrega o Sol
  // ─────────────────────────────────────────────────────────────────
  Sun loadSun() {
    try {
      // 1) lê do JSON
      double massSolar    = requireDouble(jsonSun, "massSolar");
      this.sunMassSolar   = massSolar;          // ← garantido aqui!
      double radiusAU     = requireDouble(jsonSun, "radiusAU");
      double rotPeriodDays= requireDouble(jsonSun, "rotationPeriodDays");
      JSONArray cn        = requireJSONArray(jsonSun, "colorNorm");
      int displayColor    = pApplet.color(
        cn.getFloat(0)*255f,
        cn.getFloat(1)*255f,
        cn.getFloat(2)*255f
      );

      this.sunRadiusAU = radiusAU;
      float radiusPx   = sunRadiusPx();
      PImage tex       = lookupTexture("sun");

      return new Sun(
        pApplet,
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
        double massSolar          = requireDouble (pd, "massSolar");
        double radiusAU           = requireDouble (pd, "radiusAU");
        double rotationPeriodDays = requireDouble (pd, "rotationPeriodDays");
        double orbitalPeriodDays  = requireDouble (pd, "orbitalPeriodDays");

        // ——— elementos orbitais ———————————————————————————
        double perihelionAU       = requireDouble (pd, "perihelionAU");
        double aphelionAU         = requireDouble (pd, "aphelionAU");
        double eccentricity       = requireDouble (pd, "eccentricity");
        double Ω                  = requireDouble (pd, "longitudeAscendingNodeRad");
        double iRad               = requireDouble (pd, "orbitInclinationRad");
        float axisTiltRad         = requireFloat  (pd, "axisTiltRad");
        double ω                  = requireDouble (pd, "argumentOfPeriapsisRad");
        double M0                 = requireDouble (pd, "meanAnomalyRad");
        double a                  = requireDouble (pd, "semiMajorAxisAU");

        // ——— condição inicial via initialState com massa do Sol —————————————————
        double[] rPlane = new double[3];
        double[] vPlane = new double[3];
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
        double[] preciseGlobalPosition = new double[3];
        double[] preciseGlobalVelocity = new double[3];
        applyOrbitalPlaneToGlobal(rPlane, Ω, iRad, ω, preciseGlobalPosition);
        applyOrbitalPlaneToGlobal(vPlane, Ω, iRad, ω, preciseGlobalVelocity);
        PVector rGlobal = new PVector(
          (float) preciseGlobalPosition[0],
          (float) preciseGlobalPosition[1],
          (float) preciseGlobalPosition[2]
        );
        PVector vGlobal = new PVector(
          (float) preciseGlobalVelocity[0],
          (float) preciseGlobalVelocity[1],
          (float) preciseGlobalVelocity[2]
        );

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
          pApplet,
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
        double massSolar          = requireDouble(md, "massSolar");
        double radiusAU           = requireDouble(md, "radiusAU");
        double rotationPeriodDays = requireDouble(md, "rotationPeriodDays");
        double a                  = requireDouble(md, "semiMajorAxisAU");
        double perihelionAU       = requireDouble(md, "perihelionAU");
        double aphelionAU         = requireDouble(md, "aphelionAU");
        double eccentricity       = requireDouble(md, "eccentricity");
        double iRad               = requireDouble(md, "orbitInclinationRad");
        double ω                  = requireDouble(md, "argumentOfPeriapsisRad");
        double Ω                  = requireDouble(md, "longitudeAscendingNodeRad");
        double M0                 = requireDouble(md, "meanAnomalyRad");
        boolean alignWithAxis     = md.hasKey("alignWithPlanetAxis")
                                  && md.getBoolean("alignWithPlanetAxis");
        String moonName           = requireString(md, "moonName");

        // ── condição inicial via initialState COM massa do host ─────────
        double[] rPlane = new double[3];
        double[] vPlane = new double[3];
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
        double[] preciseGlobalPosition = new double[3];
        double[] preciseGlobalVelocity = new double[3];
        applyOrbitalPlaneToGlobal(rPlane, Ω, iRad, ω, preciseGlobalPosition);
        applyOrbitalPlaneToGlobal(vPlane, Ω, iRad, ω, preciseGlobalVelocity);
        PVector rGlobal = new PVector(
          (float) preciseGlobalPosition[0],
          (float) preciseGlobalPosition[1],
          (float) preciseGlobalPosition[2]
        );
        PVector vGlobal = new PVector(
          (float) preciseGlobalVelocity[0],
          (float) preciseGlobalVelocity[1],
          (float) preciseGlobalVelocity[2]
        );

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
          pApplet,
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
        moon.setRadiusPx((float) (radiusAU / host.getRadiusAU()
                        * host.getRadiusPx()));
        host.addMoon(moon);
        pApplet.println("[ConfigLoader]   -> associada a " + host.getName() + ": " + moon.getName());


      } catch (Exception e) {
        pApplet.println("[ConfigLoader] Skip moon #" + k + ": " + e.getMessage());
      }
    }
  }

  public ArrayList<Planet> loadConfiguration() {
    ArrayList<Planet> planets = loadPlanets();
    loadMoons(planets);
    skyTexture = assets.loadImage("textures/8k_stars_milky_way.jpg");
    return planets;
  }

  private Planet getPlanetByName(String name, List<Planet> planets) {
    for (Planet p : planets) {
      if (p.getName().equalsIgnoreCase(name)) return p;
    }
    return null;
  }

  public PImage getSkyTexture() {
    return skyTexture;
  }

  public void dispose() {
    skyTexture = null;
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
  private double requireDouble(JSONObject obj, String key) {
    if (!obj.hasKey(key)) throw new RuntimeException("Missing '"+ key + "'");
    return obj.getDouble(key);
  }
}
