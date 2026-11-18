package no.ntnu.idata2304.smarthome.controlpanel;

import no.ntnu.idata2304.smarthome.common.protocol.MessageEncoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.common.protocol.TurnOffHeaterCommand;
import no.ntnu.idata2304.smarthome.common.protocol.TurnOnHeaterCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Console client for listing nodes and requesting sensor readings.
 */
public final class ControlPanelApplication {

  private static final String USAGE =
      "Commands: list | read <node ID> <sensor ID> | on <node ID> <heater ID> "
          + "| off <node ID> <heater ID> | status <node ID> <actuator ID> | exit";
  private static final String ID = "[0-9]{1,9}";

  /**
   * Private constructor to prevent instantiation of the ControlPanelApplication class.
   */

  private ControlPanelApplication() {}

  public static void main(String[] args) throws IOException {
    String token = System.getenv().getOrDefault("SMART_HOME_PANEL_TOKEN", "panel-example");
    if (!token.matches("[A-Za-z0-9._-]{1,128}")) {
      System.err.println("Provide a valid control panel token.");
      return;
    }

    try (Socket server = new Socket("127.0.0.1", 5000);
         Scanner console = new Scanner(System.in, StandardCharsets.UTF_8)) {
      server.setSoTimeout(10_000);
      var reader = new BufferedReader(
          new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8));
      var writer = new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8);

      Protocol.writeLine(writer, "HELLO_PANEL;1;owner-example;" + token);
      String registration = reader.readLine();
      if (!"READY;1".equals(registration)) {
        System.err.println("Panel registration failed: " + registration);
        return;
      }

      System.out.println(USAGE);
      while (console.hasNextLine()) {
        String[] command = console.nextLine().trim().split("\\s+");
        if (command.length == 1 && command[0].equals("exit")) {
          return;
        }
        String request = toRequest(command);
        if (request == null) {
          System.out.println(USAGE);
          continue;
        }
        Protocol.writeLine(writer, request);

        String response = reader.readLine();
        if (response == null) {
          System.err.println("Server closed the connection.");
          return;
        }
        if (response.startsWith("NODES;")) {
          if (!showConnectedNodes(response, reader, writer)) {
            System.err.println("Server closed the connection.");
            return;
          }
        } else {
          showResponse(response);
        }
      }
    }
  }

  /**
   * Translates a line typed by the user into a protocol message.
   *
   * @param command the typed command, split on whitespace
   * @return the protocol message to send, or null if the command is not recognized
   */
  private static String toRequest(String[] command) {
    if (command.length == 1 && command[0].equals("list")) {
      return "LIST_NODES";
    }
    if (command.length != 3 || !command[1].matches(ID) || !command[2].matches(ID)) {
      return null;
    }
    int nodeId = Integer.parseInt(command[1]);
    int deviceId = Integer.parseInt(command[2]);
    return switch (command[0]) {
      case "read" -> "GET_READING;" + nodeId + ";" + deviceId;
      case "on" -> MessageEncoder.encodeTurnOnHeater(new TurnOnHeaterCommand(nodeId, deviceId));
      case "off" -> MessageEncoder.encodeTurnOffHeater(new TurnOffHeaterCommand(nodeId, deviceId));
      case "status" -> "GET_ACTUATOR;" + nodeId + ";" + deviceId;
      default -> null;
    };
  }

  /**
   * Displays the response from the server to the user.
   *
   * @param response the response from the server
   */

  private static void showResponse(String response) {
    String[] fields = response.split(";", -1);
    if (fields.length == 5 && fields[0].equals("READING")) {
      System.out.println("Node " + fields[1] + ", sensor " + fields[2] + ": " + fields[4] + " °C");
    } else if (fields.length == 5 && fields[0].equals("ACTUATOR_STATE")) {
      System.out.println("Node " + fields[1] + ", actuator " + fields[2] + " (" + fields[3]
          + "): " + fields[4]);
    } else if (response.equals("NODES")) {
      System.out.println("No connected sensor nodes.");
    } else {
      System.out.println(response);
    }
  }

  /**
   * Displays the list of connected sensor nodes and their devices.
   *
   * @param response the response from the server containing the list of nodes
   * @param reader the BufferedReader to read responses from the server
   * @param writer the OutputStreamWriter to send requests to the server
   * @return true if all nodes were successfully listed, false if the server closed the connection
   * @throws IOException if an I/O error occurs while reading from or writing to the server
   */

  private static boolean showConnectedNodes(String response, BufferedReader reader,
                                            OutputStreamWriter writer) throws IOException {
    for (String nodeId : response.substring(6).split(";")) {
      System.out.println("Connected sensor node " + nodeId);
      Protocol.writeLine(writer, "LIST_DEVICES;" + nodeId);
      String devices = reader.readLine();
      if (devices == null) {
        return false;
      }
      showDevices(nodeId, devices);
    }
    return true;
  }

  /**
   * Displays the list of devices for a specific sensor node.
   *
   * @param nodeId the ID of the sensor node
   * @param response the response from the server containing the list of devices
   */

  private static void showDevices(String nodeId, String response) {
    String[] fields = response.split(";", -1);
    if (fields.length < 2 || !fields[0].equals("DEVICES") || !fields[1].equals(nodeId)) {
      System.out.println("  Could not list devices: " + response);
      return;
    }
    if (fields.length == 2) {
      System.out.println("  No devices.");
      return;
    }
    for (int i = 2; i < fields.length; i++) {
      String[] device = fields[i].split(":", -1);
      if (device.length != 3) {
        System.out.println("  Invalid device: " + fields[i]);
        continue;
      }
      String category = device[0].equals("SENSOR") ? "Sensor" : "Actuator";
      System.out.println("  " + category + " ID " + device[1] + " (" + device[2] + ")");
    }
  }
}
