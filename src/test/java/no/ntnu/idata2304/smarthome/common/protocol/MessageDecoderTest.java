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

  @Test
  public void decodesNodeRegistration() {
    String message = "HELLO_NODE;1;12;node-example";
    NodeRegistration registration = MessageDecoder.decodeNodeRegistration(message);
    assertEquals("node-example", registration.getToken());
  }

  @Test
  public void rejectsNonNumericNodeId() {
    String message = "HELLO_NODE;1;abc;node-example";
    assertThrows(NumberFormatException.class, () -> {
      MessageDecoder.decodeNodeRegistration(message);
    });
  }

  @Test
  public void rejectsNegativeNodeId() {
    String message = "HELLO_NODE;1;-5;node-example";
    assertThrows(IllegalArgumentException.class, () -> {
      MessageDecoder.decodeNodeRegistration(message);
    });
  }

  @Test
  public void  rejectsUnsupportedProtocolVersion() {
    String message = "HELLO_NODE;2;12;node-example";
    assertThrows(IllegalArgumentException.class, () -> {
      MessageDecoder.decodeNodeRegistration(message);
    });
  }

  @Test
  public void acceptsTokenWith128Characters() {
    String token = "a".repeat(128);
    String message = "HELLO_NODE;1;12;" + token;

    NodeRegistration registration = MessageDecoder.decodeNodeRegistration(message);

    assertEquals(12, registration.getNodeId());
  }

}
