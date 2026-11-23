package no.ntnu.idata2304.smarthome.sensornode.actuator;

/** Creates concrete actuators behind the shared Actuator interface. */
public final class ActuatorFactory {
  private ActuatorFactory() {
  }

  /**
   * Creates an actuator of the specified type.
   *
   * @param type the actuator type, HEATER or FAN
   * @param actuatorId the ID of the new actuator
   * @return the new actuator
   * @throws IllegalArgumentException if the type is unknown or the ID is negative
   */
  public static Actuator create(String type, int actuatorId) {
    return switch (type) {
      case "HEATER" -> new Heater(actuatorId);
      case "FAN" -> new Fan(actuatorId);
      case null, default -> throw new IllegalArgumentException("Unknown actuator type: " + type);
    };
  }
}
