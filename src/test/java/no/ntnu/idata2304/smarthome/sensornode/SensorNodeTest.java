package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SensorNodeTest {

  @Test
  public void idSavesAndReturnsCorrectly() {
    SensorNode node = new SensorNode(1);
    assertEquals(1, node.getNodeId());
  }

  @Test
  public void canAddAndRetrieveTemperatureSensors() {
    SensorNode node = new SensorNode(1);
    TemperatureSensor sensor1 = new TemperatureSensor(1, 25.0);
    TemperatureSensor sensor2 = new TemperatureSensor(2, 30.0);
    node.addTemperatureSensor(sensor1);
    node.addTemperatureSensor(sensor2);
    assertEquals(2, node.getTemperatureSensors().size());
    assertTrue(node.getTemperatureSensors().contains(sensor1));
    assertTrue(node.getTemperatureSensors().contains(sensor2));
  }

  @Test
  public void temperatureSensorValuesAreCorrect() {
    SensorNode node = new SensorNode(1);
    TemperatureSensor sensor = new TemperatureSensor(1, 25.0);
    node.addTemperatureSensor(sensor);
    assertEquals(25.0, node.getTemperatureSensors().get(0).getTemperature());
    sensor.setTemperature(30.0);
    assertEquals(30.0, node.getTemperatureSensors().get(0).getTemperature());
  }

  @Test
  public void returnedSensorListCannotBeModified() {
    SensorNode node = new SensorNode(1);
    TemperatureSensor sensor = new TemperatureSensor(1, 25.0);
    node.addTemperatureSensor(sensor);
    var sensors = node.getTemperatureSensors();
    assertThrows(UnsupportedOperationException.class, () -> sensors.add(new TemperatureSensor(2, 30.0)));
    assertEquals(1, node.getTemperatureSensors().size());
  }

  @Test
  public void simulateTemperatureChangeUpdatesTemperature() {
    SensorNode node = new SensorNode(1);
    TemperatureSensor sensor = new TemperatureSensor(1, 25.0);
    node.addTemperatureSensor(sensor);
    double initialTemperature = sensor.getTemperature();
    node.updateTemperatureSensors();
    double updatedTemperature = sensor.getTemperature();
    assertNotEquals(initialTemperature, updatedTemperature);
  }

}
