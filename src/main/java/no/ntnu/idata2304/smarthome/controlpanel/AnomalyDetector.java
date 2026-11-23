package no.ntnu.idata2304.smarthome.controlpanel;

/** A utility class for detecting anomalies in sensor readings. */
public class AnomalyDetector {

  /**
   * Checks if the given temperature value is considered a cold anomaly.
   *
   * @param temperature the temperature value to check
   * @return true if the temperature is below 5.0 degrees Celsius, false otherwise
   */
  public static boolean isTemperatureColdAnomaly(double temperature) {
    return temperature < 5.0;
  }

  /**
   * Checks if the given temperature value is considered a hot anomaly.
   *
   * @param temperature the temperature value to check
   * @return true if the temperature is above 35.0 degrees Celsius, false otherwise
   */
  public static boolean isTemperatureHotAnomaly(double temperature) {
    return temperature > 35.0;
  }

  /**
   * Checks if the given humidity value is considered a high anomaly.
   *
   * @param humidity the humidity value to check
   * @return true if the humidity is above 80.0 percent, false otherwise
   */
  public static boolean isHumidityHighAnomaly(double humidity) {
    return humidity > 80.0;
  }
}
