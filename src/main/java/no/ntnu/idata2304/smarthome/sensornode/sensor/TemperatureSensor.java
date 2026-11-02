package no.ntnu.idata2304.smarthome.sensornode.sensor;

import java.util.Random;

/**
 * Represents a temperature sensor in the smart home system.
 */

public class TemperatureSensor {

  private final int sensorId;
  private double temperature;
  private final Random random;

  /**
   * Constructs a TemperatureSensor with the specified sensor ID and initial temperature.
   *
   * @param sensorId    the unique identifier for the sensor
   * @param temperature the initial temperature reading
   */

  public TemperatureSensor(int sensorId, double temperature) {
    this.sensorId = sensorId;
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

  public int getSensorId() {
    return sensorId;
  }

  /**
   * Simulates a temperature change by adding a random value between -0.5 and 0.5.
   */

  public void simulateTemperatureChange() {
    double change = (random.nextDouble() - 0.5);
    this.temperature += change;
  }


}
