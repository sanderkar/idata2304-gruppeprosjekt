package no.ntnu.idata2304.smarthome.sensornode.sensor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemperatureSensorTest {

  @Test

  public void idSavesAndReturnsCorrectly() {
    TemperatureSensor sensor = new TemperatureSensor(1, 25.0);
    assertEquals(1, sensor.getSensorId());
  }

  @Test
  public void temperatureSavesAndReturnsCorrectly() {
    TemperatureSensor sensor = new TemperatureSensor(1, 25.0);
    assertEquals(25.0, sensor.getTemperature());
  }

  @Test
  public void temperatureCanBeUpdated() {
    TemperatureSensor sensor = new TemperatureSensor(1, 25.0);
    sensor.setTemperature(30.0);
    assertEquals(30.0, sensor.getTemperature());
  }

  @Test
  public void simulatedTemperatureStaysWithinExpectedRange() {
    TemperatureSensor sensor = new TemperatureSensor(1, 25.0);
    sensor.simulateTemperatureChange();
    assertTrue(sensor.getTemperature() >= 24.5 && sensor.getTemperature() <= 25.5);
  }


}

