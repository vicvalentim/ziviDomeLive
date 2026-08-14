/**
 * Adaptação astronômica do SimulationTimeline da API para Julian Date absoluto.
 */
class SimulatedClock {
  static final double JD_J2000 = 2451545.0;
  private final SimulationTimeline timeline;

  SimulatedClock(SimulationTimeline timeline) {
    this.timeline = timeline;
  }

  double getCurrentJulianDate() {
    return JD_J2000 + timeline.getPosition();
  }

  double getDaysSinceJ2000() {
    return timeline.getPosition();
  }

  void setJulianDate(double jd) {
    timeline.setPosition(jd - JD_J2000);
  }

  void setCalendarUTC(int year, int month, int day,
                      int hour, int minute, double second) {
    setJulianDate(toJulianDate(year, month, day, hour, minute, second));
  }

  double getTimeScale() {
    return timeline.getRate();
  }

  void setTimeScale(double timeScale) {
    timeline.setRate(timeScale);
  }

  boolean isPaused() {
    return timeline.isPaused();
  }

  void pause() {
    timeline.pause();
  }

  void resume() {
    timeline.resume();
  }

  void jumpDays(double days) {
    timeline.jump(days);
  }

  void resetJulianDate(double jd) {
    setJulianDate(jd);
  }

  double toJulianDate(int year, int month, int day,
                      int hour, int minute, double second) {
    int y = year;
    int m = month;
    if (m <= 2) {
      y -= 1;
      m += 12;
    }
    int a = y / 100;
    int b = 2 - a + (a / 4);
    double dayFraction = (hour + minute / 60.0 + second / 3600.0) / 24.0;
    return Math.floor(365.25 * (y + 4716))
         + Math.floor(30.6001 * (m + 1))
         + day + dayFraction + b - 1524.5;
  }

  String getCalendarUTCString() {
    double jd = getCurrentJulianDate();
    double z0 = Math.floor(jd + 0.5);
    double f0 = (jd + 0.5) - z0;
    double a = z0;
    if (z0 >= 2299161) {
      int alpha = (int) ((z0 - 1867216.25) / 36524.25);
      a += 1 + alpha - alpha / 4;
    }
    double b = a + 1524;
    double c = Math.floor((b - 122.1) / 365.25);
    double d = Math.floor(365.25 * c);
    double e = Math.floor((b - d) / 30.6001);
    double dayDecimal = b - d - Math.floor(30.6001 * e) + f0;

    int day = (int) Math.floor(dayDecimal);
    double dayFraction = dayDecimal - day;
    int month = (e < 14) ? (int) e - 1 : (int) e - 13;
    int year = (month > 2) ? (int) c - 4716 : (int) c - 4715;

    double seconds = dayFraction * 86400.0;
    int hour = (int) (seconds / 3600);
    seconds -= hour * 3600;
    int minute = (int) (seconds / 60);
    int second = (int) (seconds - minute * 60);

    return nf(year, 4) + "-" + nf(month, 2) + "-" + nf(day, 2) + "  " +
      nf(hour, 2) + ":" + nf(minute, 2) + ":" + nf(second, 2);
  }
}
