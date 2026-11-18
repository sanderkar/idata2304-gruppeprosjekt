package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.common.protocol.TurnOnHeaterCommand;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Actuator;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sensor node in the smart home system.
 */

public class SensorNode {

  private final int nodeId;
  private final List<Actuator> actuators;
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
    this.actuators = new ArrayList<>();
  }

  /**
   * Adds an actuator to the sensor node.
   *
   * @param actuator the actuator to add
   */

  public void addActuator(Actuator actuator) {
    for (Actuator existingActuator : actuators) {
      if (existingActuator.getActuatorId() == actuator.getActuatorId()) {
        throw new IllegalArgumentException("Actuator with ID " + actuator.getActuatorId() + " already exists.");
      }
    }
    actuators.add(actuator);
  }

  /**
   * Returns a list of all actuators associated with the sensor node.
   *
   * @return a list of actuators
   */

  public List<Actuator> getActuators() {
    return List.copyOf(actuators);
  }

  /**
   * Finds an actuator by its unique identifier.
   *
   * @param actuatorId the unique identifier of the actuator to find
   * @return the actuator with the specified ID, or null if not found
   */

  public Actuator findActuator(int actuatorId) {
    for (Actuator actuator : actuators) {
      if (actuator.getActuatorId() == actuatorId) {
        return actuator;
      }
    }
    return null;
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
   * Adds a heater to the sensor node.
   *
   * @param heater the heater to add
   */

  public void addHeater(Heater heater) {
    addActuator(heater);
  }

  /**
   * Returns a list of all heaters associated with the sensor node.
   *
   * @return a list of heaters
   */
  public List<Heater> getHeaters() {
    List<Heater> heaters = new ArrayList<>();
    for (Actuator actuator : actuators) {
      if (actuator instanceof Heater) {
        heaters.add((Heater) actuator);
      }
    }
    return List.copyOf(heaters);
  }

  /**
   * Turns on the heater with the specified heater ID.
   *
   * @param heaterId the unique identifier of the heater to turn on
   * @return true if the heater was found and turned on, false otherwise
   */

  public boolean turnOnHeater(int heaterId) {
    for (Heater heater : getHeaters()) {
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
    for (Heater heater : getHeaters()) {
      if (heater.getHeaterId() == heaterId) {
        heater.turnOff();
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


}
