package no.ntnu.idata2304.smarthome.common.protocol;
/**
 * A utility class for decoding messages in the smart home protocol.
 */
public class MessageDecoder {

  /**
   * Decodes a string message into a TurnOnHeaterCommand.
   *
   * @param message the string message to decode
   * @return the decoded TurnOnHeaterCommand
   * @throws IllegalArgumentException if the message format is invalid
   */
  public static TurnOnHeaterCommand decodeTurnOnHeater(String message) {
    String[] parts = message.split(";", -1);
    if (parts.length != 3 || !parts[0].equals("TURN_ON_HEATER")) {
      throw new IllegalArgumentException("Invalid TURN_ON_HEATER command format");
    }
    int nodeId = Integer.parseInt(parts[1]);
    int heaterId = Integer.parseInt(parts[2]);
    return new TurnOnHeaterCommand(nodeId, heaterId);
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

  public static ReadingRequest decodeGetReading(String message) {
    String[] fields = message.split(";", -1);
    if (fields.length != 3 || !fields[0].equals("GET_READING")
        || !fields[1].matches("[0-9]+") || !fields[2].matches("[0-9]+")) {
      throw new IllegalArgumentException("Invalid GET_READING message");
    }
    return new ReadingRequest(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
  }

  public static int decodeListDevices(String message) {
    String[] fields = message.split(";", -1);
    if (fields.length != 2 || !fields[0].equals("LIST_DEVICES")
        || !fields[1].matches("[0-9]+")) {
      throw new IllegalArgumentException("Invalid LIST_DEVICES message");
    }
    return Integer.parseInt(fields[1]);
  }

  public record ReadingRequest(int nodeId, int sensorId) {}
}
