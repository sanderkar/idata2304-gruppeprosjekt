package no.ntnu.idata2304.smarthome.common.protocol;

/** A utility class for decoding messages in the smart home protocol. */
public class MessageDecoder {

  /**
   * Decodes a string message into a TurnOnHeaterCommand.
   *
   * @param message the string message to decode
   * @return the decoded TurnOnHeaterCommand
   * @throws IllegalArgumentException if the message format is invalid
   */
  public static TurnOnHeaterCommand decodeTurnOnHeater(String message) {
    int[] ids = decodeHeaterCommandIds(message, "TURN_ON_HEATER");
    return new TurnOnHeaterCommand(ids[0], ids[1]);
  }

  /**
   * Decodes a string message into a TurnOffHeaterCommand.
   *
   * @param message the string message to decode
   * @return the decoded TurnOffHeaterCommand
   * @throws IllegalArgumentException if the message format is invalid
   */
  public static TurnOffHeaterCommand decodeTurnOffHeater(String message) {
    int[] ids = decodeHeaterCommandIds(message, "TURN_OFF_HEATER");
    return new TurnOffHeaterCommand(ids[0], ids[1]);
  }

  /**
   * Parses "&lt;TYPE&gt;;nodeId;heaterId" and returns {nodeId, heaterId}.
   *
   * @param message the message to parse
   * @param expectedType the command type the message must start with
   * @return an array holding the node ID and the heater ID, in that order
   * @throws IllegalArgumentException if the format is invalid or an ID is not a number
   */
  private static int[] decodeHeaterCommandIds(String message, String expectedType) {
    String[] parts = message.split(";", -1);
    if (parts.length != 3 || !parts[0].equals(expectedType)) {
      throw new IllegalArgumentException("Invalid " + expectedType + " command format");
    }
    int nodeId = Integer.parseInt(parts[1]);
    int heaterId = Integer.parseInt(parts[2]);

    if (nodeId < 0 || heaterId < 0) {
      throw new IllegalArgumentException("Node ID and heater ID cannot be negative");
    }

    return new int[] {nodeId, heaterId};
  }

  /**
   * Decodes a string message into a NodeRegistration.
   *
   * @param message the string message to decode
   * @return the decoded NodeRegistration
   * @throws IllegalArgumentException if the message format is invalid
   */
  public static NodeRegistration decodeNodeRegistration(String message) {
    String[] parts = message.split(";", -1);
    if (parts.length != 4 || !parts[0].equals("HELLO_NODE")) {
      throw new IllegalArgumentException("Invalid HELLO_NODE command format");
    }
    if (!parts[1].equals("1")) {
      throw new IllegalArgumentException("Unsupported protocol version");
    }
    int nodeId = Integer.parseInt(parts[2]);

    if (nodeId < 0) {
      throw new IllegalArgumentException("Node ID cannot be negative");
    }
    if (parts[3].isEmpty()) {
      throw new IllegalArgumentException("Token name cannot be empty");
    }

    if (parts[3].length() > 128) {
      throw new IllegalArgumentException("Token name cannot exceed 128 characters");
    }

    return new NodeRegistration(nodeId, parts[3]);
  }

  /**
   * Decodes a string message into a ReadingRequest.
   *
   * @param message the string message to decode
   * @return the decoded ReadingRequest
   * @throws IllegalArgumentException if the message format is invalid
   */
  public static ReadingRequest decodeGetReading(String message) {
    String[] fields = message.split(";", -1);
    if (fields.length != 3
        || !fields[0].equals("GET_READING")
        || !fields[1].matches("[0-9]+")
        || !fields[2].matches("[0-9]+")) {
      throw new IllegalArgumentException("Invalid GET_READING message");
    }
    return new ReadingRequest(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
  }

  /**
   * Decodes a string message into a ListDevicesRequest.
   *
   * @param message the string message to decode
   * @return the decoded ListDevicesRequest
   * @throws IllegalArgumentException if the message format is invalid
   */
  public static int decodeListDevices(String message) {
    String[] fields = message.split(";", -1);
    if (fields.length != 2 || !fields[0].equals("LIST_DEVICES") || !fields[1].matches("[0-9]+")) {
      throw new IllegalArgumentException("Invalid LIST_DEVICES message");
    }
    return Integer.parseInt(fields[1]);
  }

  /**
   * Decodes a request for an actuator's current state.
   *
   * @param message the GET_ACTUATOR message to decode
   * @return the requested node and actuator IDs
   * @throws IllegalArgumentException if the format or either ID is invalid
   */
  public static ActuatorRequest decodeGetActuator(String message) {
    String[] fields = message.split(";", -1);
    if (fields.length != 3
        || !fields[0].equals("GET_ACTUATOR")
        || !fields[1].matches("[0-9]+")
        || !fields[2].matches("[0-9]+")) {
      throw new IllegalArgumentException("Invalid GET_ACTUATOR message");
    }
    return new ActuatorRequest(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
  }

  /**
   * Decodes a string message into an ActuatorCommand.
   *
   * @param message the string message to decode
   * @return the decoded ActuatorCommand
   * @throws IllegalArgumentException if the message format is invalid
   */
  public static ActuatorCommand decodeActuator(String message) {
    String[] fields = message.split(";", -1);
    if (fields.length != 4
        || !fields[0].equals("SET_ACTUATOR")
        || !fields[1].matches("[0-9]+")
        || !fields[2].matches("[0-9]+")
        || !fields[3].equals("ON") && !fields[3].equals("OFF")) {
      throw new IllegalArgumentException("Invalid SET_ACTUATOR message");
    }
    return new ActuatorCommand(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), fields[3]);
  }

  /**
   * Represents a request to get a reading from a sensor.
   *
   * @param nodeId the ID of the node
   * @param sensorId the ID of the sensor
   */
  public record ReadingRequest(int nodeId, int sensorId) {}

  /**
   * Represents a request to get the current state of an actuator.
   *
   * @param nodeId the ID of the node
   * @param actuatorId the ID of the actuator
   */
  public record ActuatorRequest(int nodeId, int actuatorId) {}

  /**
   * Represents a command to control an actuator.
   *
   * @param nodeId the ID of the node
   * @param actuatorId the ID of the actuator
   * @param state the state to set the actuator to
   */
  public record ActuatorCommand(int nodeId, int actuatorId, String state) {}
}
