package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.common.model.SensorType;

/** A relative humidity sensor, measured as a percentage. */
public class HumiditySensor extends Sensor {
  public HumiditySensor(int sensorId, double humidity) {
    super(sensorId, humidity, SensorType.HUMIDITY);
  }
}
