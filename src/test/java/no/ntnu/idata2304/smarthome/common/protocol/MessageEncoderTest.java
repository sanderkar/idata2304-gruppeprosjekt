package no.ntnu.idata2304.smarthome.common.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageEncoderTest {

  @Test
  public void encodesTurnOnHeaterCommandCorrectly() {
    TurnOnHeaterCommand command = new TurnOnHeaterCommand(12, 3);
    String encodedMessage = MessageEncoder.encodeTurnOnHeater(command);
    assertEquals("TURN_ON_HEATER;12;3", encodedMessage);
  }

  @Test
  public void encodesTurnOffHeaterCommandCorrectly() {
    TurnOffHeaterCommand command = new TurnOffHeaterCommand(12, 3);
    String encodedMessage = MessageEncoder.encodeTurnOffHeater(command);
    assertEquals("TURN_OFF_HEATER;12;3", encodedMessage);
  }
}
