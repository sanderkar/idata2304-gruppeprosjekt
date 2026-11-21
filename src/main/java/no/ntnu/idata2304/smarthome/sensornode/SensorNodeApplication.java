package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration;
import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.SensorFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Entry point for the sensor node application.
 */
public final class SensorNodeApplication {

  /**
   * Private constructor to prevent instantiation of the SensorNodeApplication class.
   */

  private SensorNodeApplication() {}

  /**
   * Starts the sensor node application and connects to the server.
   *
   * @param args optional sensor node ID, defaults to 12
   * @throws IOException if an I/O error occurs when connecting to the server
   */


   static void main(String[] args) throws IOException {
    int nodeId = args.length == 0 ? 12 : Integer.parseInt(args[0]);
    String tokenName = "SMART_HOME_NODE_" + nodeId + "_TOKEN";
    String token = LocalConfiguration.loadCredentials().get(tokenName);
    if (nodeId < 0 || token == null || !token.matches("[A-Za-z0-9._-]{1,128}")) {
      System.err.println("Provide a non-negative node ID and set " + tokenName
          + " in config/local.properties or the environment. See docs/running.md.");
      return;
    }

    var serverAddress = LocalConfiguration.loadServerAddress();
    try (Socket server = new Socket(serverAddress.host(), serverAddress.port())) {
      System.out.println("Connected to server at " + server.getInetAddress().getHostAddress() + ":" + server.getPort());
      var writer = new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8);
      Protocol.writeLine(writer, "HELLO_NODE;1;" + nodeId + ";" + token);

      var reader = new BufferedReader(
          new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8));
      String response = reader.readLine();
      System.out.println("Received response from server: " + response);

      if (!"READY;1".equals(response)) {
        System.err.println("Node registration failed. Check that " + tokenName
            + " matches the server configuration, then restart the server. See docs/running.md.");
        return;
      }

      var node = new SensorNode(nodeId);
      var heater = new Heater(1);

      node.addSensor(SensorFactory.create("TEMPERATURE", 1, 22.0));
      node.addSensor(SensorFactory.create("HUMIDITY", 2, 50.0));
      node.addHeater(heater);
      String command;
      while ((command = reader.readLine()) != null) {
        Protocol.writeLine(writer, respondToCommand(command, node, heater));
      }
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
          return devices.add("ACTUATOR:" + heater.getHeaterId() + ":HEATER").toString();
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
          return "ACTUATOR_STATE;" + nodeId + ";" + heater.getHeaterId()
              + ";HEATER;" + (heater.isOn() ? "ON" : "OFF");
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
          return "ACTUATOR_STATE;" + nodeId + ";" + heater.getHeaterId()
              + ";HEATER;" + (heater.isOn() ? "ON" : "OFF");
        }
        case "GET_ACTUATOR" -> {
          var status = MessageDecoder.decodeGetActuator(command);
          if (status.nodeId() != nodeId) {
            return "ERROR;WRONG_NODE";
          }
          if (status.actuatorId() != heater.getHeaterId()) {
            return "ERROR;UNKNOWN_ACTUATOR";
          }
          return "ACTUATOR_STATE;" + nodeId + ";" + heater.getHeaterId()
              + ";HEATER;" + (heater.isOn() ? "ON" : "OFF");
        }

      }

      var request = MessageDecoder.decodeGetReading(command);
      if (request.nodeId() != nodeId) {
        return "ERROR;WRONG_NODE";
      }
      var sensor = node.findSensor(request.sensorId());
      if (sensor == null) {
        return "ERROR;UNKNOWN_SENSOR";
      }

      sensor.simulateChange();
      String value = String.format(Locale.ROOT, "%.2f", sensor.getValue());
      return "READING;" + nodeId + ";" + sensor.getSensorId() + ";" + sensor.getType() + ";" + value;
    } catch (IllegalArgumentException exception) {
      return "ERROR;BAD_FORMAT";
    }
  }
}
