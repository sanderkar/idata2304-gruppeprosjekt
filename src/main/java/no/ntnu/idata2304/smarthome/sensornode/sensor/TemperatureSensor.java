package no.ntnu.idata2304.smarthome.sensornode.sensor;

import java.util.Random;

/**
 * Represents a temperature sensor in the smart home system.
 */

public class TemperatureSensor {

  private final int tempSensorId;
  private double temperature;
  private final Random random;

  /**
   * Constructs a TemperatureSensor with the specified sensor ID and initial temperature.
   *
   * @param tempSensorId    the unique identifier for the sensor
   * @param temperature the initial temperature reading
   * @throws IllegalArgumentException if the sensor ID is negative
   */

  public TemperatureSensor(int tempSensorId, double temperature) {

    if (tempSensorId < 0) {
      throw new IllegalArgumentException("Sensor ID must be a non-negative integer.");
    }

    this.tempSensorId = tempSensorId;
    this.temperature = temperature;
    this.random = new Random();
  }

  /**
   * Returns the current temperature reading from the sensor.
   *
   * @return the current temperature
   */

  public double getTemperature() {
    return temperature;
  }

  /**
   * Sets the current temperature reading for the sensor.
   *
   * @param temperature the new temperature value
   */

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }

  /**
   * Returns the unique identifier for the sensor.
   *
   * @return the sensor ID
   */

  public int getTempSensorId() {
    return tempSensorId;
  }

  /**
   * Simulates a temperature change by adding a random value between -0.5 and 0.5.
   */

  public void simulateTemperatureChange() {
    double change = (random.nextDouble() - 0.5);
    this.temperature += change;
  }


}
