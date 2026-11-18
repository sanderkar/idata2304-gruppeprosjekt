package no.ntnu.idata2304.smarthome.sensornode.actuator;

public interface Actuator {

  int getActuatorId();

  String getType();

  void turnOn();

  void turnOff();

  boolean isOn();
}
