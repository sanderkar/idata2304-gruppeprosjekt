package no.ntnu.idata2304.smarthome.common.protocol;

/** A utility class for encoding messages in the smart home protocol. */
public class MessageEncoder {

  /**
   * Encodes a TurnOnHeaterCommand into a string message.
   *
   * @param command the TurnOnHeaterCommand to encode
   * @return the encoded string message
   */
  public static String encodeTurnOnHeater(TurnOnHeaterCommand command) {
    return String.format("TURN_ON_HEATER;%d;%d", command.getNodeId(), command.getHeaterId());
  }

  /**
   * Encodes a TurnOffHeaterCommand into a string message.
   *
   * <p>.
   *
   * @param command the TurnOffHeaterCommand to encode
   * @return the encoded string message
   */
  public static String encodeTurnOffHeater(TurnOffHeaterCommand command) {
    return String.format("TURN_OFF_HEATER;%d;%d", command.getNodeId(), command.getHeaterId());
  }
}
