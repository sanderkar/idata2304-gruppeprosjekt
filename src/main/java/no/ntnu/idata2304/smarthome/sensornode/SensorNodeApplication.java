package no.ntnu.idata2304.smarthome.sensornode;

import java.io.IOException;
import java.util.Locale;
import java.util.StringJoiner;
import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration;
import no.ntnu.idata2304.smarthome.common.network.ClientConnection;
import no.ntnu.idata2304.smarthome.common.network.ClientConnection.RegistrationRejected;
import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Fan;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.SensorFactory;

/** Entry point for the sensor node application. */
public final class SensorNodeApplication {

  /** Private constructor to prevent instantiation of the SensorNodeApplication class. */
  private SensorNodeApplication() {}

  /**
   * Starts the sensor node application and connects to the server.
   *
   * @param args optional sensor node ID, defaults to 12
   * @throws IOException if local configuration cannot be read
   * @throws InterruptedException if interrupted while waiting to reconnect
   */
  static void main(String[] args) throws IOException, InterruptedException {
    int nodeId = args.length == 0 ? 12 : Integer.parseInt(args[0]);
    String tokenName = "SMART_HOME_NODE_" + nodeId + "_TOKEN";
    String token = LocalConfiguration.loadCredentials().get(tokenName);
    if (nodeId < 0 || token == null || !token.matches("[A-Za-z0-9._-]{1,128}")) {
      System.err.println(
          "Provide a non-negative node ID and set "
              + tokenName
              + " in config/local.properties or the environment. See docs/running.md.");
      return;
    }

    var serverAddress = LocalConfiguration.loadServerAddress();
    String hello = "HELLO_NODE;1;" + nodeId + ";" + token + Protocol.HEARTBEAT_SUFFIX;
    var node = new SensorNode(nodeId);
    var heater = new Heater(1);
    node.addSensor(SensorFactory.create("TEMPERATURE", 1, 22.0));
    node.addSensor(SensorFactory.create("HUMIDITY", 2, 50.0));
    node.addHeater(heater);
    node.addActuator(new Fan(2));

    long retryDelay = 1_000;
    while (!Thread.currentThread().isInterrupted()) {
      try (var server = ClientConnection.connect(serverAddress, hello, 15_000)) {
        System.out.println("Connected to server at " + serverAddress.host() + ":"
            + serverAddress.port());
        System.out.println("Received response from server: READY;1");
        retryDelay = 1_000;
        while (!Thread.currentThread().isInterrupted()) {
          String command = server.read();
          server.send(Protocol.PING.equals(command)
              ? Protocol.PONG : respondToCommand(command, node, heater));
        }
        return;
      } catch (RegistrationRejected exception) {
        if (!"ERROR;DUPLICATE_NODE".equals(exception.getMessage())) {
          System.err.println("Node registration failed: " + exception.getMessage()
              + ". Check " + tokenName + " against the server configuration.");
          return;
        }
      } catch (IOException exception) {
        // Retry after a failed connection, a closed session or a missed heartbeat.
      }
      System.err.println("Node disconnected. Reconnecting in " + retryDelay / 1_000 + " s.");
      Thread.sleep(retryDelay);
      retryDelay = Math.min(retryDelay * 2, 4_000);
    }
  }

  static String respondToCommand(String command, SensorNode node, Heater heater) {
    int nodeId = node.getNodeId();
    try {
      switch (command.split(";", 2)[0]) {
        case "LIST_DEVICES" -> {
          if (MessageDecoder.decodeListDevices(command) != nodeId) {
            return "ERROR;WRONG_NODE";
          }
          var devices = new StringJoiner(";").add("DEVICES").add(Integer.toString(nodeId));
          for (var sensor : node.getSensors()) {
            devices.add("SENSOR:" + sensor.getSensorId() + ":" + sensor.getType());
          }
          for (var actuator : node.getActuators()) {
            devices.add("ACTUATOR:" + actuator.getActuatorId() + ":" + actuator.getType());
          }
          return devices.toString();
        }
        case "SET_ACTUATOR" -> {
          var actuatorCommand = MessageDecoder.decodeActuator(command);

          if (actuatorCommand.nodeId() != nodeId) {
            return "ERROR;WRONG_NODE";
          }

          var actuator = node.findActuator(actuatorCommand.actuatorId());
          if (actuator == null) {
            return "ERROR;UNKNOWN_ACTUATOR";
          }

          if (actuatorCommand.state().equals("ON")) {
            node.turnOnActuator(actuatorCommand.actuatorId());
          } else {
            node.turnOffActuator(actuatorCommand.actuatorId());
          }

          return "ACTUATOR_STATE;"
              + nodeId
              + ";"
              + actuator.getActuatorId()
              + ";"
              + actuator.getType()
              + ";"
              + (actuator.isOn() ? "ON" : "OFF");
        }
        case "TURN_ON_HEATER" -> {
          var heaterCommand = MessageDecoder.decodeTurnOnHeater(command);

          if (heaterCommand.getNodeId() != nodeId) {
            return "ERROR;WRONG_NODE";
          }

          if (heaterCommand.getHeaterId() != heater.getHeaterId()) {
            return "ERROR;UNKNOWN_ACTUATOR";
          }

          heater.turnOn();
          return "ACTUATOR_STATE;"
              + nodeId
              + ";"
              + heater.getHeaterId()
              + ";HEATER;"
              + (heater.isOn() ? "ON" : "OFF");
        }
        case "TURN_OFF_HEATER" -> {
          var heaterCommand = MessageDecoder.decodeTurnOffHeater(command);

          if (heaterCommand.getNodeId() != nodeId) {
            return "ERROR;WRONG_NODE";
          }

          if (heaterCommand.getHeaterId() != heater.getHeaterId()) {
            return "ERROR;UNKNOWN_ACTUATOR";
          }

          heater.turnOff();
          return "ACTUATOR_STATE;"
              + nodeId
              + ";"
              + heater.getHeaterId()
              + ";HEATER;"
              + (heater.isOn() ? "ON" : "OFF");
        }
        case "GET_ACTUATOR" -> {
          var status = MessageDecoder.decodeGetActuator(command);
          if (status.nodeId() != nodeId) {
            return "ERROR;WRONG_NODE";
          }
          var actuator = node.findActuator(status.actuatorId());
          if (actuator == null) {
            return "ERROR;UNKNOWN_ACTUATOR";
          }
          return "ACTUATOR_STATE;"
              + nodeId
              + ";"
              + actuator.getActuatorId()
              + ";"
              + actuator.getType()
              + ";"
              + (actuator.isOn() ? "ON" : "OFF");
        }
        case "GET_READING" -> {
          return handleGetReading(command, node, nodeId);
        }
        default -> {
          return "ERROR;BAD_FORMAT";
        }
      }
    } catch (IllegalArgumentException exception) {
      return "ERROR;BAD_FORMAT";
    }
  }

  private static String handleGetReading(String command, SensorNode node, int nodeId) {
    var reading = MessageDecoder.decodeGetReading(command);
    if (reading.nodeId() != nodeId) {
      return "ERROR;WRONG_NODE";
    }
    var sensor = node.findSensor(reading.sensorId());
    if (sensor == null) {
      return "ERROR;UNKNOWN_SENSOR";
    }
    sensor.simulateChange();
    String value = String.format(Locale.ROOT, "%.2f", sensor.getValue());
    return "READING;" + nodeId + ";" + sensor.getSensorId() + ";" + sensor.getType() + ";" + value;
  }
}
