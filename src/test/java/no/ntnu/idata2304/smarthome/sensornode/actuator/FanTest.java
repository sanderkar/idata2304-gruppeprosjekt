package no.ntnu.idata2304.smarthome.sensornode.actuator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FanTest {

  @Test
  void storesAndReturnsIdAndType() {
    Fan fan = new Fan(5);

    assertEquals(5, fan.getActuatorId());
    assertEquals("FAN", fan.getType());
  }

  @Test
  void fanCanBeTurnedOnAndOff() {
    Fan fan = new Fan(1);

    assertFalse(fan.isOn());

    fan.turnOn();
    assertTrue(fan.isOn());

    fan.turnOff();
    assertFalse(fan.isOn());
  }

  @Test
  void rejectsNegativeId() {
    assertThrows(IllegalArgumentException.class, () -> new Fan(-1));
  }
}