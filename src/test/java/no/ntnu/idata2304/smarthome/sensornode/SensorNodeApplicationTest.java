package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;
import no.ntnu.idata2304.smarthome.sensornode.sensor.SensorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SensorNodeApplicationTest {

  @Test
  void discoversAndReadsFactorySensorsById() {
    var node = new SensorNode(12);
    var temperature = SensorFactory.create("TEMPERATURE", 1, 22);
    node.addSensor(temperature);
    node.addSensor(SensorFactory.create("HUMIDITY", 2, 50));
    var heater = new Heater(1);
    assertEquals("DEVICES;12;SENSOR:1:TEMPERATURE;SENSOR:2:HUMIDITY;ACTUATOR:1:HEATER",
        SensorNodeApplication.respondToCommand("LIST_DEVICES;12", node, heater));
    String reading = SensorNodeApplication.respondToCommand("GET_READING;12;2", node, heater);
    assertTrue(reading.startsWith("READING;12;2;HUMIDITY;"));
    double value = Double.parseDouble(reading.split(";")[4]);
    assertTrue(value >= 49.5 && value <= 50.5);
    assertEquals(22, temperature.getValue());
    assertEquals("ERROR;UNKNOWN_SENSOR",
        SensorNodeApplication.respondToCommand("GET_READING;12;99", node, heater));
    assertEquals("ERROR;WRONG_NODE",
        SensorNodeApplication.respondToCommand("GET_READING;99;2", node, heater));
  }

  @Test
  public void listsTemperatureSensorAndHeater() {
    SensorNode node = new SensorNode(12);
    node.addSensor(new TemperatureSensor(1, 22.0));
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "LIST_DEVICES;12", node, heater);

    assertEquals(
        "DEVICES;12;SENSOR:1:TEMPERATURE;ACTUATOR:1:HEATER",
        response);
  }

  @Test
  public void turnOnHeaterTurnsOnHeaterAndReportsState() {
    SensorNode node = new SensorNode(12);
    node.addSensor(new TemperatureSensor(1, 22.0));
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "TURN_ON_HEATER;12;1", node, heater);

    assertEquals("ACTUATOR_STATE;12;1;HEATER;ON", response);
    assertTrue(heater.isOn());
  }

  @Test
  public void turnOffHeaterTurnsOffHeaterAndReportsState() {
    SensorNode node = new SensorNode(12);
    node.addSensor(new TemperatureSensor(1, 22.0));
    Heater heater = new Heater(1);
    heater.turnOn();

    String response = SensorNodeApplication.respondToCommand(
        "TURN_OFF_HEATER;12;1", node, heater);

    assertEquals("ACTUATOR_STATE;12;1;HEATER;OFF", response);
    assertFalse(heater.isOn());
  }

  @Test
  public void heaterCommandForOtherNodeIsRejectedAndHeaterUnchanged() {
    SensorNode node = new SensorNode(12);
    node.addSensor(new TemperatureSensor(1, 22.0));
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "TURN_ON_HEATER;99;1", node, heater);

    assertEquals("ERROR;WRONG_NODE", response);
    assertFalse(heater.isOn());
  }

  @Test
  public void heaterCommandForUnknownHeaterIsRejected() {
    SensorNode node = new SensorNode(12);
    node.addSensor(new TemperatureSensor(1, 22.0));
    Heater heater = new Heater(1);

    String response = SensorNodeApplication.respondToCommand(
        "TURN_ON_HEATER;12;9", node, heater);

    assertEquals("ERROR;UNKNOWN_ACTUATOR", response);
    assertFalse(heater.isOn());
  }

  @Test
  public void malformedHeaterCommandGivesBadFormat() {
    SensorNode node = new SensorNode(12);
    node.addSensor(new TemperatureSensor(1, 22.0));
    Heater heater = new Heater(1);

    assertEquals("ERROR;BAD_FORMAT",
        SensorNodeApplication.respondToCommand("TURN_ON_HEATER;12", node, heater));
    assertEquals("ERROR;BAD_FORMAT",
        SensorNodeApplication.respondToCommand("TURN_ON_HEATER;12;abc", node, heater));
  }

  @Test
  public void unknownCommandGivesBadFormat() {
    SensorNode node = new SensorNode(12);
    node.addSensor(new TemperatureSensor(1, 22.0));
    Heater heater = new Heater(1);

    assertEquals("ERROR;BAD_FORMAT",
        SensorNodeApplication.respondToCommand("OPEN_WINDOW;12;1", node, heater));
  }
}
