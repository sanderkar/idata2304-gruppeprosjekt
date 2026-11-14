package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}