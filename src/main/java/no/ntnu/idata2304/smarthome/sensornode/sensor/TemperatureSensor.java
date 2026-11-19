package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.common.model.SensorType;

/** A temperature sensor, measured in degrees Celsius. */
public class TemperatureSensor extends Sensor {
  public TemperatureSensor(int sensorId, double temperature) {
    super(sensorId, temperature, SensorType.TEMPERATURE);
  }
}
