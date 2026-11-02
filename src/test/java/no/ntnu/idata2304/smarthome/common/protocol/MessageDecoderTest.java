package no.ntnu.idata2304.smarthome.common.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageDecoderTest {

  @Test
  public void decodesTurnOnHeaterCommandCorrectly() {
    String message = "TURN_ON_HEATER;12;3";
    TurnOnHeaterCommand command = MessageDecoder.decodeTurnOnHeater(message);
    assertEquals(12, command.getNodeId());
    assertEquals(3, command.getHeaterId());
  }

  @Test
  public void rejectsUnknownCommandType() {
    String message = "UNKNOWN_COMMAND;12;3";
    assertThrows(IllegalArgumentException.class, () -> {
      MessageDecoder.decodeTurnOnHeater(message);
    });
  }

  @Test
  public void rejectsMissingHeaterId() {
    String message = "TURN_ON_HEATER;12";
    assertThrows(IllegalArgumentException.class, () -> {
      MessageDecoder.decodeTurnOnHeater(message);
    });
  }

  @Test
  public void rejectsExtraFields() {
    String message = "TURN_ON_HEATER;12;3;extra";
    assertThrows(IllegalArgumentException.class, () -> {
      MessageDecoder.decodeTurnOnHeater(message);
    });
  }

  @Test
  public void rejectsTrailingEmptyField() {
    String message = "TURN_ON_HEATER;12;3;";
    assertThrows(IllegalArgumentException.class, () -> {
      MessageDecoder.decodeTurnOnHeater(message);
    });
  }

  @Test
  public void rejectsNonNumericHeaterId() {
    String message = "TURN_ON_HEATER;12;abc";
    assertThrows(NumberFormatException.class, () -> {
      MessageDecoder.decodeTurnOnHeater(message);
    });
  }

}
