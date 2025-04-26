// —————————————————————————— Constantes Físicas ——————————————————————————

final float SOL_MASS = 1.0f;          // Massa do Sol (normalizada)
public static final float G_DAY = 2.9591220828559093e-4f;

// —————————————————————————— Conversão de Unidades ——————————————————————————

// Definição arbitrária do projeto para escala espacial visual
final float PIXELS_PER_AU = 400.0f;   // Pixels por Unidade Astronômica (AU)
final float RADIUS_AU_SUN = 0.004650f;// Raio real do Sol em AU

// —————————————————————————— Parâmetros de Distância dos Planetas ——————————————————————————

final float MERCURY_DIST = 0.39f;
final float VENUS_DIST   = 0.72f;
final float EARTH_DIST   = 1.00f;
final float MARS_DIST    = 1.52f;
final float JUPITER_DIST = 5.20f;
final float SATURN_DIST  = 9.58f;
final float URANUS_DIST  = 19.22f;
final float NEPTUNE_DIST = 30.07f;

// —————————————————————————— Razões de Tamanho dos Planetas (raio_planeta / raio_Sol) ——————————————————————————

final float MERCURY_RATIO  = 0.00351f;
final float VENUS_RATIO    = 0.00870f;
final float EARTH_RATIO    = 0.00916f;
final float MARS_RATIO     = 0.00487f;
final float JUPITER_RATIO  = 0.1005f;
final float SATURN_RATIO   = 0.0837f;
final float URANUS_RATIO   = 0.0365f;
final float NEPTUNE_RATIO  = 0.0354f;

// —————————————————————————— Parâmetros Visuais e de Renderização ——————————————————————————

// Fator extra para tornar o Sol visível (sem ele, o Sol real seria "pequeno demais")
final float VISUAL_SCALE = 40.0f;

// Raio visual do Sol usado para desenhar (em pixels)
final float SUN_VISUAL_RADIUS = PIXELS_PER_AU * RADIUS_AU_SUN * VISUAL_SCALE;

// Ajuste para órbitas das luas (calibração manual para visualização agradável)
final float MOON_ORBIT_CALIBRATION = 12.0f;

// Fator de aceleração da rotação dos planetas
final float ROTATION_FACTOR = 1.0f;

// —————————————————————————— Estilo Wireframe ——————————————————————————

final int WIREFRAME_COLOR = 0xFFFFFFFF;        // Branco
final float WIREFRAME_STROKE_WEIGHT = 0.5f;    // Espessura da linha no wireframe
