package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.common.model.SensorType;

/**
 * A relative humidity sensor, measured as a percentage.
 */
public class HumiditySensor extends Sensor {
  /**
   * Constructs a HumiditySensor with the specified sensor ID and humidity value.
   *
   * @param sensorId the unique identifier for the humidity sensor
   * @param humidity the current humidity value in percentage
   */
  public HumiditySensor(int sensorId, double humidity) {
    super(sensorId, humidity, SensorType.HUMIDITY);
  }
}
