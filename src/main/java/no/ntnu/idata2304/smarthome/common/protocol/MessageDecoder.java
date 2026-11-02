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


}
