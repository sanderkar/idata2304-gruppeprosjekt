package no.ntnu.idata2304.smarthome.sensornode.actuator;

/** Represents a heater in the smart home system. */
public class Heater implements Actuator {

  private final int heaterId;
  private boolean isOn;

  /**
   * Constructs a Heater with the specified heater ID.
   *
   * @param heaterId the unique identifier for the heater
   * @throws IllegalArgumentException if the heater ID is negative
   */
  public Heater(int heaterId) {

    if (heaterId < 0) {
      throw new IllegalArgumentException("Heater ID must be a non-negative integer.");
    }

    this.heaterId = heaterId;
  }

  /**
   * Returns the unique identifier for the heater.
   *
   * @return the heater ID
   */
  public int getHeaterId() {
    return heaterId;
  }

  /** Turns the heater on. */
  @Override
  public void turnOn() {
    isOn = true;
  }

  /** Turns the heater off. */
  @Override
  public void turnOff() {
    isOn = false;
  }

  /**
   * Returns whether the heater is currently on.
   *
   * @return true if the heater is on, false otherwise
   */
  @Override
  public boolean isOn() {
    return isOn;
  }

  /**
   * Returns the unique identifier for the actuator (heater).
   *
   * @return the actuator ID
   */
  @Override
  public int getActuatorId() {
    return heaterId;
  }

  /**
   * Returns the type of the actuator, which is "Heater" in this case.
   *
   * @return the actuator type
   */
  @Override
  public String getType() {
    return "HEATER";
  }
}
