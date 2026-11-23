package no.ntnu.idata2304.smarthome.sensornode.actuator;

/** Represents a fan in the smart home system. */
public class Fan implements Actuator {

  private final int fanId;
  private boolean isOn;

  /**
   * Constructs a fan with the specified ID.
   *
   * @param fanId the unique identifier for the fan
   * @throws IllegalArgumentException if the fan ID is negative
   */
  public Fan(int fanId) {
    if (fanId < 0) {
      throw new IllegalArgumentException("Fan ID must be a non-negative integer.");
    }
    this.fanId = fanId;
  }

  @Override
  public int getActuatorId() {
    return fanId;
  }

  @Override
  public String getType() {
    return "FAN";
  }

  @Override
  public void turnOn() {
    isOn = true;
  }

  @Override
  public void turnOff() {
    isOn = false;
  }

  @Override
  public boolean isOn() {
    return isOn;
  }
}
