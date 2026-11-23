package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.common.model.SensorType;

/**
 * Creates concrete sensors behind the shared Sensor API.
 */
public final class SensorFactory {
  private SensorFactory() {
  }

  /**
   * Creates a sensor of the specified type with the given sensor ID and initial value.
   *
   * @param type         the type of sensor to create (e.g., "TEMPERATURE", "HUMIDITY")
   * @param sensorId     the unique identifier for the sensor
   * @param initialValue the initial value for the sensor
   * @return a new instance of the specified sensor type
   */
  public static Sensor create(String type, int sensorId, double initialValue) {
    return switch (SensorType.valueOf(type)) {
      case TEMPERATURE -> new TemperatureSensor(sensorId, initialValue);
      case HUMIDITY -> new HumiditySensor(sensorId, initialValue);
    };
  }
}
