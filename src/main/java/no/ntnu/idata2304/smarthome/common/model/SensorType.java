package no.ntnu.idata2304.smarthome.common.model;

/** Shared sensor metadata for simulation, response validation and display. */
public enum SensorType {
  TEMPERATURE("°C", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
  HUMIDITY("% RH", 0, 100);

  private final String unit;
  private final double minimum;
  private final double maximum;

  SensorType(String unit, double minimum, double maximum) {
    this.unit = unit;
    this.minimum = minimum;
    this.maximum = maximum;
  }

  public String getUnit() {
    return unit;
  }

  public boolean isValid(double value) {
    return Double.isFinite(value) && value >= minimum && value <= maximum;
  }

  public double clamp(double value) {
    return Math.clamp(value, minimum, maximum);
  }
}
