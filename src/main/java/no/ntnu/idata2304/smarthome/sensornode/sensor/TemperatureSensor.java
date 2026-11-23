package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.common.model.SensorType;

/**
 * A temperature sensor, measured in degrees Celsius.
 */
public class TemperatureSensor extends Sensor {
  /**
   * Constructs a TemperatureSensor with the specified sensor ID and temperature value.
   *
   * @param sensorId    the unique identifier for the temperature sensor
   * @param temperature the current temperature value in degrees Celsius
   */
  public TemperatureSensor(int sensorId, double temperature) {
    super(sensorId, temperature, SensorType.TEMPERATURE);
  }
}
