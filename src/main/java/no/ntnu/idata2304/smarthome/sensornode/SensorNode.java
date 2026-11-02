package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sensor node in the smart home system.
 */

public class SensorNode {

  private final int nodeId;
  private final List<Heater> heaters;
  private final List<TemperatureSensor> temperatureSensors;

  /**
   * Constructs a SensorNode with the specified node ID.
   *
   * @param nodeId the unique identifier for the sensor node
   */

  public SensorNode(int nodeId) {
    this.nodeId = nodeId;
    this.temperatureSensors = new ArrayList<>();
    this.heaters = new ArrayList<>();
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

  /**
   * Returns a list of all heaters associated with the sensor node.
   *
   * @return a list of heaters
   */

  public List<Heater> getHeaters() {
    return List.copyOf(heaters);
  }

  /**
   * Adds a heater to the sensor node.
   *
   * @param heater the heater to add
   */

  public void addHeater(Heater heater) {
    heaters.add(heater);
  }

  /**
   * Turns on the heater with the specified heater ID.
   *
   * @param heaterId the unique identifier of the heater to turn on
   * @return true if the heater was found and turned on, false otherwise
   */

  public boolean turnOnHeater(int heaterId) {
    for (Heater heater : heaters) {
      if (heater.getHeaterId() == heaterId) {
        heater.turnOn();
        return true;
      }
    }
    return false;
  }

  /**
   * Turns off the heater with the specified heater ID.
   *
   * @param heaterId the unique identifier of the heater to turn off
   * @return true if the heater was found and turned off, false otherwise
   */

  public boolean turnOffHeater(int heaterId) {
    for (Heater heater : heaters) {
      if (heater.getHeaterId() == heaterId) {
        heater.turnOff();
        return true;
      }
    }
    return false;
  }

}
