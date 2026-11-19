package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.common.model.SensorType;
import java.util.concurrent.ThreadLocalRandom;

/**  Sensor, an abstract parent class
 */
public abstract class Sensor {
  private final int sensorId;
  private final SensorType type;
  private double value;

  protected Sensor(int sensorId, double value, SensorType type) {
    if (sensorId < 0) {
      throw new IllegalArgumentException("Sensor ID must be non-negative.");
    }
    this.sensorId = sensorId;
    this.type = type;
    setValue(value);
  }

  public int getSensorId() {
    return sensorId;
  }

  public SensorType getType() {
    return type;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    if (!type.isValid(value)) {
      throw new IllegalArgumentException("Invalid value for " + type);
    }
    this.value = value;
  }

  /** Changes the reading by at most 0.5, within the sensor type's limits. */
  public void simulateChange() {
    value = type.clamp(value + ThreadLocalRandom.current().nextDouble(-0.5, 0.5));
  }
}
