package no.ntnu.idata2304.smarthome.sensornode;

import java.util.ArrayList;
import java.util.List;
import no.ntnu.idata2304.smarthome.common.protocol.TurnOnHeaterCommand;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Actuator;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.Sensor;

/** Represents a sensor node in the smart home system. */
public class SensorNode {

  private final int nodeId;
  private final List<Actuator> actuators;
  private final List<Sensor> sensors;

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
    this.sensors = new ArrayList<>();
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
        throw new IllegalArgumentException(
            "Actuator with ID " + actuator.getActuatorId() + " already exists.");
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
   * Turns on the actuator with the specified ID.
   *
   * @param actuatorId the actuator ID
   * @return true if the actuator was found and turned on, false otherwise
   */
  public boolean turnOnActuator(int actuatorId) {
    Actuator actuator = findActuator(actuatorId);
    if (actuator == null) {
      return false;
    }

    actuator.turnOn();
    return true;
  }

  /**
   * Turns off the actuator with the specified ID.
   *
   * @param actuatorId the actuator ID
   * @return true if the actuator was found and turned off, false otherwise
   */
  public boolean turnOffActuator(int actuatorId) {
    Actuator actuator = findActuator(actuatorId);
    if (actuator == null) {
      return false;
    }

    actuator.turnOff();
    return true;
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
   * Adds a sensor to the sensor node.
   *
   * @param sensor the sensor to add
   * @throws IllegalArgumentException if a sensor with the same ID already exists
   */
  public void addSensor(Sensor sensor) {
    for (Sensor existingSensor : sensors) {
      if (existingSensor.getSensorId() == sensor.getSensorId()) {
        throw new IllegalArgumentException(
            "Sensor with ID " + sensor.getSensorId() + " already exists.");
      }
    }
    sensors.add(sensor);
  }

  /**
   * Returns a list of all sensors associated with the sensor node.
   *
   * @return a list of sensors
   */
  public List<Sensor> getSensors() {
    return List.copyOf(sensors);
  }

  /**
   * Finds a sensor by its unique identifier.
   *
   * @param sensorId the unique identifier of the sensor to find
   * @return the sensor with the specified ID, or null if not found
   */
  public Sensor findSensor(int sensorId) {
    return sensors.stream()
        .filter(sensor -> sensor.getSensorId() == sensorId)
        .findFirst()
        .orElse(null);
  }

  /** Updates the readings of all associated sensors. */
  public void updateSensors() {
    for (Sensor sensor : sensors) {
      sensor.simulateChange();
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
