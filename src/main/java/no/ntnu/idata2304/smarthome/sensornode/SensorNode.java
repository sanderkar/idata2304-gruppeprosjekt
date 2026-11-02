package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.common.protocol.TurnOnHeaterCommand;
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

    if (nodeId < 0) {
      throw new IllegalArgumentException("Node ID must be a non-negative integer.");
    }


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
   * @throws IllegalArgumentException if a temperature sensor with the same ID already exists
   */

  public void addTemperatureSensor(TemperatureSensor sensor) {
    for (TemperatureSensor existingSensor : temperatureSensors) {
      if (existingSensor.getTempSensorId() == sensor.getTempSensorId()) {
        throw new IllegalArgumentException("Temperature sensor with ID " + sensor.getTempSensorId() + " already exists.");
      }
    }
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
   * Adds a heater to the sensor node. If a heater with the same ID already exists, an exception is thrown.
   *
   * @param heater the heater to add
   * @throws IllegalArgumentException if a heater with the same ID already exists
   */

  public void addHeater(Heater heater) {

    for (Heater existingHeater : heaters) {
      if (existingHeater.getHeaterId() == heater.getHeaterId()) {
        throw new IllegalArgumentException("Heater with ID " + heater.getHeaterId() + " already exists.");
      }
    }

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
   * Handles a heater command only when it is addressed to this node.
   *
   * @param command the command to handle
   * @return true if this node contains the requested heater, false otherwise
   */
  public boolean handleTurnOnHeaterCommand(TurnOnHeaterCommand command) {
    if (command.getNodeId() != nodeId) {
      return false;
    }
    return turnOnHeater(command.getHeaterId());
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
