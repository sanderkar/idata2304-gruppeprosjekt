package no.ntnu.idata2304.smarthome.sensornode.actuator;
/**
 * Represents a heater in the smart home system.
 */
public class Heater {

  private final int heaterId;
  private boolean isOn;

  /**
   * Constructs a Heater with the specified heater ID.
   *
   * @param heaterId the unique identifier for the heater
   */


  public Heater(int heaterId) {
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

  /**
   * Turns the heater on.
   */

  public void turnOn() {
    isOn = true;
  }

  /**
   * Turns the heater off.
   */

  public void turnOff() {
    isOn = false;
  }

  /**
   * Returns whether the heater is currently on.
   *
   * @return true if the heater is on, false otherwise
   */

  public boolean isOn() {
    return isOn;
  }
}