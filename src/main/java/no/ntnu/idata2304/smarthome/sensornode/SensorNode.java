package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sensor node in the smart home system.
 */

public class SensorNode {

  private final int nodeId;
  private final List<TemperatureSensor> temperatureSensors;

  /**
   * Constructs a SensorNode with the specified node ID.
   *
   * @param nodeId the unique identifier for the sensor node
   */

  public SensorNode(int nodeId) {
    this.nodeId = nodeId;
    this.temperatureSensors = new ArrayList<>();
  }

  /**
   * Returns the unique identifier for the sensor node.
   *
   * @return the node ID
   */

  public int getNodeId() {
    return nodeId;
  }


  /**
   * Adds a temperature sensor to the sensor node.
   *
   * @param sensor the temperature sensor to add
   */

  public void addTemperatureSensor(TemperatureSensor sensor) {
    temperatureSensors.add(sensor);
  }

  /**
   * Returns a list of all temperature sensors associated with the sensor node.
   *
   * @return a list of temperature sensors
   */

  public List<TemperatureSensor> getTemperatureSensors() {
    return List.copyOf(temperatureSensors);
  }

  /**
   * Updates the temperature readings of all associated temperature sensors.
   */

  public void updateTemperatureSensors() {
    for (TemperatureSensor sensor : temperatureSensors) {
      sensor.simulateTemperatureChange();
    }
  }




}
