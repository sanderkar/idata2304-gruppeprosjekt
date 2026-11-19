package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.common.model.SensorType;

/** Creates concrete sensors behind the shared Sensor API. */
public final class SensorFactory {
  private SensorFactory() {}

  public static Sensor create(String type, int sensorId, double initialValue) {
    return switch (SensorType.valueOf(type)) {
      case TEMPERATURE -> new TemperatureSensor(sensorId, initialValue);
      case HUMIDITY -> new HumiditySensor(sensorId, initialValue);
    };
  }
}
