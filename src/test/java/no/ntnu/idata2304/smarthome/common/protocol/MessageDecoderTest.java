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

  /**
   * Tests GET_READING, LIST_DEVICES, and node registration validation.
   * Code made using AI assistance and reviewed by the team members
   */
  @Test
  public void decodesGetReadingCorrectly() {
    String message = "GET_READING;12;3";

    MessageDecoder.ReadingRequest request =
        MessageDecoder.decodeGetReading(message);

    assertEquals(12, request.nodeId());
    assertEquals(3, request.sensorId());
  }

  @Test
  public void rejectsGetReadingWithMissingSensorId() {
    String message = "GET_READING;12";

    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeGetReading(message));
  }

  @Test
  public void rejectsGetReadingWithNegativeNodeId() {
    String message = "GET_READING;-1;3";

    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeGetReading(message));
  }

  @Test
  public void rejectsGetReadingWithNonNumericSensorId() {
    String message = "GET_READING;12;abc";

    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeGetReading(message));
  }

  @Test
  public void decodesListDevicesCorrectly() {
    String message = "LIST_DEVICES;12";

    int nodeId = MessageDecoder.decodeListDevices(message);

    assertEquals(12, nodeId);
  }

  @Test
  public void rejectsListDevicesWithNegativeNodeId() {
    String message = "LIST_DEVICES;-1";

    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeListDevices(message));
  }

  @Test
  public void rejectsNodeRegistrationWithEmptyToken() {
    String message = "HELLO_NODE;1;12;";

    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeNodeRegistration(message));
  }

  @Test
  public void rejectsNodeRegistrationTokenLongerThan128Characters() {
    String token = "a".repeat(129);
    String message = "HELLO_NODE;1;12;" + token;

    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeNodeRegistration(message));
  }

  @Test
  public void decodesTurnOffHeaterCommandCorrectly() {
    TurnOffHeaterCommand command = MessageDecoder.decodeTurnOffHeater("TURN_OFF_HEATER;12;3");
    assertEquals(12, command.getNodeId());
    assertEquals(3, command.getHeaterId());
  }

  @Test
  public void turnOffDecoderRejectsTurnOnMessage() {
    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeTurnOffHeater("TURN_ON_HEATER;12;3"));
  }

  @Test
  public void turnOnDecoderRejectsTurnOffMessage() {
    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeTurnOnHeater("TURN_OFF_HEATER;12;3"));
  }

  @Test
  public void rejectsTurnOffHeaterWithMissingHeaterId() {
    assertThrows(IllegalArgumentException.class,
        () -> MessageDecoder.decodeTurnOffHeater("TURN_OFF_HEATER;12"));
  }
}
