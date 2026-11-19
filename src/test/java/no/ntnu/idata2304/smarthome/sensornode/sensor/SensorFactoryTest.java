package no.ntnu.idata2304.smarthome.sensornode.sensor;

import no.ntnu.idata2304.smarthome.sensornode.SensorNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensorFactoryTest {
  @Test
  void createsBothTypesWithSharedMetadata() {
    var temperature = SensorFactory.create("TEMPERATURE", 1, 22);
    var humidity = SensorFactory.create("HUMIDITY", 2, 50);
    assertInstanceOf(TemperatureSensor.class, temperature);
    assertInstanceOf(HumiditySensor.class, humidity);
    assertEquals("°C", temperature.getType().getUnit());
    assertEquals("% RH", humidity.getType().getUnit());
    assertEquals(2, humidity.getSensorId());
    assertEquals(50, humidity.getValue());
  }

  @Test
  void rejectsUnknownTypesIdsAndInvalidHumidity() {
    assertThrows(IllegalArgumentException.class, () -> SensorFactory.create("UNKNOWN", 1, 0));
    assertThrows(IllegalArgumentException.class, () -> SensorFactory.create("HUMIDITY", -1, 50));
    for (double value : new double[] {-1, 101, Double.NaN, Double.POSITIVE_INFINITY}) {
      assertThrows(IllegalArgumentException.class, () -> SensorFactory.create("HUMIDITY", 2, value));
      var sensor = SensorFactory.create("HUMIDITY", 2, 50);
      assertThrows(IllegalArgumentException.class, () -> sensor.setValue(value));
    }
  }

  @Test
  void simulationRespectsHumidityLimits() {
    for (double initial : new double[] {0, 50, 100}) {
      var sensor = SensorFactory.create("HUMIDITY", 2, initial);
      for (int i = 0; i < 100; i++) {
        double previous = sensor.getValue();
        sensor.simulateChange();
        assertTrue(sensor.getType().isValid(sensor.getValue()));
        assertTrue(Math.abs(sensor.getValue() - previous) <= 0.5);
      }
    }
  }

  @Test
  void sensorIdsAreUniqueAcrossTypesOnEachNode() {
    var node = new SensorNode(12);
    node.addSensor(SensorFactory.create("TEMPERATURE", 1, 22));
    assertThrows(IllegalArgumentException.class,
        () -> node.addSensor(SensorFactory.create("HUMIDITY", 1, 50)));
  }
}
