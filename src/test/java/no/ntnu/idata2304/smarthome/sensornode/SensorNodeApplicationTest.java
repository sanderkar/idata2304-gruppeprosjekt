package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SensorNodeApplicationTest {

  @Test
  public void listsTemperatureSensorAndHeater() {
    TemperatureSensor sensor = new TemperatureSensor(1, 22.0);
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "LIST_DEVICES;12", 12, sensor, heater);

    assertEquals(
        "DEVICES;12;SENSOR:1:TEMPERATURE;ACTUATOR:1:HEATER",
        response);
  }

  @Test
  public void turnOnHeaterTurnsOnHeaterAndReportsState() {
    TemperatureSensor sensor = new TemperatureSensor(1, 22.0);
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "TURN_ON_HEATER;12;1", 12, sensor, heater);

    assertEquals("ACTUATOR_STATE;12;1;HEATER;ON", response);
    assertTrue(heater.isOn());
  }

  @Test
  public void turnOffHeaterTurnsOffHeaterAndReportsState() {
    TemperatureSensor sensor = new TemperatureSensor(1, 22.0);
    Heater heater = new Heater(1);
    heater.turnOn();

    String response = SensorNodeApplication.respondToCommand(
        "TURN_OFF_HEATER;12;1", 12, sensor, heater);

    assertEquals("ACTUATOR_STATE;12;1;HEATER;OFF", response);
    assertFalse(heater.isOn());
  }

  @Test
  public void heaterCommandForOtherNodeIsRejectedAndHeaterUnchanged() {
    TemperatureSensor sensor = new TemperatureSensor(1, 22.0);
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "TURN_ON_HEATER;99;1", 12, sensor, heater);

    assertEquals("ERROR;WRONG_NODE", response);
    assertFalse(heater.isOn());
  }

  @Test
  public void heaterCommandForUnknownHeaterIsRejected() {
    TemperatureSensor sensor = new TemperatureSensor(1, 22.0);
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "TURN_ON_HEATER;12;9", 12, sensor, heater);

    assertEquals("ERROR;UNKNOWN_ACTUATOR", response);
    assertFalse(heater.isOn());
  }

  @Test
  public void malformedHeaterCommandGivesBadFormat() {
    TemperatureSensor sensor = new TemperatureSensor(1, 22.0);
    Heater heater = new Heater(1);

    assertEquals("ERROR;BAD_FORMAT",
        SensorNodeApplication.respondToCommand("TURN_ON_HEATER;12", 12, sensor, heater));
    assertEquals("ERROR;BAD_FORMAT",
        SensorNodeApplication.respondToCommand("TURN_ON_HEATER;12;abc", 12, sensor, heater));
  }

  @Test
  public void unknownCommandGivesBadFormat() {
    TemperatureSensor sensor = new TemperatureSensor(1, 22.0);
    Heater heater = new Heater(1);

    assertEquals("ERROR;BAD_FORMAT",
        SensorNodeApplication.respondToCommand("OPEN_WINDOW;12;1", 12, sensor, heater));
  }
}
