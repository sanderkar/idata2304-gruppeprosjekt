package no.ntnu.idata2304.smarthome.sensornode;

import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.sensornode.actuator.Heater;
import no.ntnu.idata2304.smarthome.sensornode.sensor.TemperatureSensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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


  public static void main(String[] args) throws IOException {
    int nodeId = args.length == 0 ? 12 : Integer.parseInt(args[0]);
    String token = System.getenv("SMART_HOME_NODE_" + nodeId + "_TOKEN");
    if (nodeId == 12 && token == null) {
      token = "node-example";
    }
    if (nodeId < 0 || token == null || !token.matches("[A-Za-z0-9._-]{1,128}")) {
      System.err.println("Provide a non-negative node ID and a valid node token.");
      return;
    }

    try (Socket server = new Socket("127.0.0.1", 5000)) {
      System.out.println("Connected to server at " + server.getInetAddress().getHostAddress() + ":" + server.getPort());
      var writer = new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8);
      Protocol.writeLine(writer, "HELLO_NODE;1;" + nodeId + ";" + token);

      var reader = new BufferedReader(
          new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8));
      String response = reader.readLine();
      System.out.println("Received response from server: " + response);

      if (!"READY;1".equals(response)) {
        System.err.println("Unexpected response from server. Exiting.");
        return;
      }

      var node = new SensorNode(nodeId);
      var sensor = new TemperatureSensor(1, 22.0);
      var heater = new Heater(1);

      node.addTemperatureSensor(sensor);
      node.addHeater(heater);
      String command;
      while ((command = reader.readLine()) != null) {
        Protocol.writeLine(writer, respondToCommand(command, nodeId, sensor, heater));
      }
    }
  }

  static String respondToCommand(String command, int nodeId, TemperatureSensor sensor, Heater heater) {
    try {
      if (command.split(";", 2)[0].equals("LIST_DEVICES")) {
        if (MessageDecoder.decodeListDevices(command) != nodeId) {
          return "ERROR;WRONG_NODE";
        }
        return "DEVICES;" + nodeId
            + ";SENSOR:" + sensor.getTempSensorId() + ":TEMPERATURE"
            + ";ACTUATOR:" + heater.getHeaterId() + ":HEATER";
      }

      var request = MessageDecoder.decodeGetReading(command);
      if (request.nodeId() != nodeId) {
        return "ERROR;WRONG_NODE";
      }
      if (request.sensorId() != sensor.getTempSensorId()) {
        return "ERROR;UNKNOWN_SENSOR";
      }

      sensor.simulateTemperatureChange();
      String temperature = String.format(Locale.ROOT, "%.2f", sensor.getTemperature());
      return "READING;" + nodeId + ";" + sensor.getTempSensorId() + ";TEMPERATURE;" + temperature;
    } catch (IllegalArgumentException exception) {
      return "ERROR;BAD_FORMAT";
    }
  }
}
