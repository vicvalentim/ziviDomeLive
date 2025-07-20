// SimulatedClock.pde

/**
 * Relógio simulado baseado em Julian Date absoluto.
 * 
 * - JD_J2000 = 2451545.0 (2000-01-01 12:00 UTC)
 * - Converte data/hora UTC → Julian Date
 * - Avança conforme timeScale (dias simulados por segundo real)
 */
class SimulatedClock {
  // epoch J2000 em Julian Date
  static final double JD_J2000 = 2451545.0;
  
  // estado interno
  private double currentJD;       // JD atual
  private double timeScale;       // dias simulados por segundo real
  private long   lastUpdateNanos; // timestamp da última atualização
  private boolean paused = false;

  /** Cria o relógio em J2000, escala = 1 dia simulado / 1s real */
  SimulatedClock() {
    this(JD_J2000, 1.0);
  }

  /**
   * Cria o relógio em um JD inicial e escala de tempo.
   * @param initialJD  valor inicial de Julian Date
   * @param timeScale  dias simulados por segundo real
   */
  SimulatedClock(double initialJD, double timeScale) {
    this.currentJD       = initialJD;
    this.timeScale       = timeScale;
    this.lastUpdateNanos = System.nanoTime();
  }

  /**
   * Deve ser chamado a cada frame.
   * @return dias simulados que avançaram desde a última chamada
   */
  double update() {
    long now = System.nanoTime();
    double deltaRealSec = (now - lastUpdateNanos) / 1e9;
    lastUpdateNanos = now;
    if (paused) return 0.0;
    double deltaSimDays = deltaRealSec * timeScale;
    currentJD += deltaSimDays;
    return deltaSimDays;
  }

  /** @return Julian Date atual */
  double getCurrentJulianDate() {
    return currentJD;
  }

  /** @return dias simulados desde J2000 (JD − JD_J2000) */
  double getDaysSinceJ2000() {
    return currentJD - JD_J2000;
  }

  /** Ajusta o relógio para um dado Julian Date */
  void setJulianDate(double jd) {
    this.currentJD       = jd;
    this.lastUpdateNanos = System.nanoTime();
  }

  /**
   * Define a data/hora UTC do relógio convertendo-a em JD.
   * @param year    ano (ex: 2025)
   * @param month   mês [1-12]
   * @param day     dia [1-31]
   * @param hour    hora UTC [0-23]
   * @param minute  minuto [0-59]
   * @param second  segundo (pode ser fracionário)
   */
  void setCalendarUTC(int year, int month, int day,
                      int hour, int minute, double second) {
    this.currentJD       = toJulianDate(year, month, day, hour, minute, second);
    this.lastUpdateNanos = System.nanoTime();
  }

  /** @return escala atual (dias simulados / segundo real) */
  double getTimeScale() {
    return timeScale;
  }

  /** Ajusta a escala (dias simulados / segundo real) */
  void setTimeScale(double timeScale) {
    this.timeScale = timeScale;
  }

  /** @return true se estiver pausado */
  boolean isPaused() {
    return paused;
  }
  void pause()  { this.paused = true; }
  void resume() { this.paused = false; this.lastUpdateNanos = System.nanoTime(); }

  /** Avança o relógio um número fixo de dias simulados */
  void jumpDays(double days) {
    this.currentJD += days;
  }

  /** Reseta o relógio para um JD específico */
  void resetJulianDate(double jd) {
    this.currentJD       = jd;
    this.lastUpdateNanos = System.nanoTime();
  }

  // ——————————————————————————————————————————————————————————————————
  // Conversão calendário UTC → Julian Date (Fliegel & Van Flandern)
  // ——————————————————————————————————————————————————————————————————
  double toJulianDate(int Y, int M, int D,
                             int hour, int minute, double second) {
    int y = Y;
    int m = M;
    if (m <= 2) {
      y -= 1;
      m += 12;
    }
    int A = y / 100;
    int B = 2 - A + (A / 4);
    double dayFraction = (hour + minute/60.0 + second/3600.0) / 24.0;
    return Math.floor(365.25*(y + 4716))
         + Math.floor(30.6001*(m + 1))
         + D + dayFraction + B - 1524.5;
  }

  /**
   * Converte o currentJD → calendário UTC e formata como
   * "YYYY-MM-DD HH:MM:SS".
   */
  String getCalendarUTCString() {
    double jd = currentJD;
    // 1) Separar parte inteira e fracionária de (jd+0.5)
    double Z0 = Math.floor(jd + 0.5);
    double F0 = (jd + 0.5) - Z0;
    double A = Z0;
    // correção do calendário Gregoriano
    if (Z0 >= 2299161) {
      int alpha = (int)((Z0 - 1867216.25) / 36524.25);
      A += 1 + alpha - alpha / 4;
    }
    double B = A + 1524;
    double C = Math.floor((B - 122.1) / 365.25);
    double D = Math.floor(365.25 * C);
    double E = Math.floor((B - D) / 30.6001);
    double dayDec = B - D - Math.floor(30.6001 * E) + F0;

    int day   = (int)Math.floor(dayDec);
    double df = dayDec - day;
    int month = (E < 14) ? (int)E - 1 : (int)E - 13;
    int year  = (month > 2) ? (int)C - 4716 : (int)C - 4715;

    // converte fração de dia em horas/min/segundos
    double secs   = df * 86400.0;
    int hour      = (int)(secs / 3600);
    secs        -= hour * 3600;
    int minute    = (int)(secs / 60);
    int second    = (int)(secs - minute * 60);

    // formata com nf() do Processing
    return  
      nf(year,   4) + "-" +
      nf(month,  2) + "-" +
      nf(day,    2) + "  " +
      nf(hour,   2) + ":" +
      nf(minute, 2) + ":" +
      nf(second, 2);
  }
}
