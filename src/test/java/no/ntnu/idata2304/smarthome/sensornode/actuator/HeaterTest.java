package no.ntnu.idata2304.smarthome.sensornode.actuator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HeaterTest {

  @Test
  public void idSavesAndReturnsCorrectly() {
    Heater heater = new Heater(1);
    assertEquals(1, heater.getHeaterId());
  }

  @Test
  public void heaterTurnsOnAndOffCorrectly() {
    Heater heater = new Heater(1);
    assertFalse(heater.isOn());
    heater.turnOn();
    assertTrue(heater.isOn());
    heater.turnOff();
    assertFalse(heater.isOn());
  }

  @Test
  public void heaterRemainsOnWhenTurnedOnAgain() {
    Heater heater = new Heater(1);
    heater.turnOn();
    assertTrue(heater.isOn());
    heater.turnOn();
    assertTrue(heater.isOn());
  }

  @Test
  public void heaterRemainsOffWhenTurnedOffAgain() {
    Heater heater = new Heater(1);
    heater.turnOff();
    assertFalse(heater.isOn());
    heater.turnOff();
    assertFalse(heater.isOn());
  }

  @Test
  public void multipleHeatersMaintainIndependentStates() {
    Heater heater1 = new Heater(1);
    Heater heater2 = new Heater(2);

    heater1.turnOn();
    assertTrue(heater1.isOn());
    assertFalse(heater2.isOn());

    heater2.turnOn();
    assertTrue(heater1.isOn());
    assertTrue(heater2.isOn());

    heater1.turnOff();
    assertFalse(heater1.isOn());
    assertTrue(heater2.isOn());
  }

  @Test
  public void newHeaterIsOffByDefault() {
    Heater heater = new Heater(1);
    assertFalse(heater.isOn());
  }

}
