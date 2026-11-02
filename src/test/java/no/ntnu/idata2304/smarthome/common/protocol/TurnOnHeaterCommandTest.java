package no.ntnu.idata2304.smarthome.common.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TurnOnHeaterCommandTest {

  @Test
  public void storesNodeIdAndHeaterIdCorrectly() {
    TurnOnHeaterCommand command = new TurnOnHeaterCommand(12, 3);
    assertEquals(12, command.getNodeId());
    assertEquals(3, command.getHeaterId());
  }
}
