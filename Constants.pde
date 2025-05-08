// —————————————————————————— Constantes Físicas ——————————————————————————

final float SOL_MASS = 1.0f;          // Massa do Sol (normalizada)
public static final float G_DAY = 2.9591220828559093e-4f;

// —————————————————————————— Conversão de Unidades ——————————————————————————

// Definição arbitrária do projeto para escala espacial visual
final float PIXELS_PER_AU = 600.0f;   // Pixels por Unidade Astronômica (AU)
final float RADIUS_AU_SUN = 0.004650f;// Raio real do Sol em AU
final float NEPTUNE_DIST = 30.07f;

// —————————————————————————— Estilo Wireframe ——————————————————————————

final int WIREFRAME_COLOR = 0xFFFFFFFF;        // Branco
final float WIREFRAME_STROKE_WEIGHT = 0.2f;    // Espessura da linha no wireframe


// —————————————————————————— Parâmetros dinâmicos para a simulação ——————————————————————————

// Variáveis Globais - não são constantes, portanto não usamos static
public float globalScale = 5.0f; // “tabuleiro”
public float bodyScale   = 20.0f; // “peças”
public float planetAmplification = 1.0f; // ajuste fino para planetas

// —————————————————————————— Funções Globais de Distâncias e Tamanhos ——————————————————————————

// ---------- distâncias ----------
public float pxPerAU(){
    return PIXELS_PER_AU * globalScale; 
}

// ---------- tamanhos ----------
public float sunRadiusPx(){
    return RADIUS_AU_SUN * pxPerAU() * bodyScale;
}
