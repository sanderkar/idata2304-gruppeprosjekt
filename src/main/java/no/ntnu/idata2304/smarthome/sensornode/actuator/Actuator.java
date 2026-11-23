package no.ntnu.idata2304.smarthome.sensornode.actuator;

/**
 * Represents an actuator in the smart home system. An actuator is a device that can be controlled
 * to perform actions, such as turning on or off.
 */
public interface Actuator {

  /**
   * Returns the unique identifier for the actuator.
   *
   * @return the actuator ID
   */
  int getActuatorId();

  /**
   * Returns the type of the actuator.
   *
   * @return the actuator type as a string
   */
  String getType();

  /** Turns the actuator on. */
  void turnOn();

  /** Turns the actuator off. */
  void turnOff();

  /**
   * Checks if the actuator is currently on.
   *
   * @return true if the actuator is on, false otherwise
   */
  boolean isOn();
}
