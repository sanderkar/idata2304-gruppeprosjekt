package no.ntnu.idata2304.smarthome.controlpanel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Scanner;
import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration;
import no.ntnu.idata2304.smarthome.common.model.SensorType;
import no.ntnu.idata2304.smarthome.common.security.TlsContexts;

/** Console client for listing nodes and requesting sensor readings. */
public final class ControlPanelApplication {

  private static final String USAGE =
      """
      Commands
        list                         Show connected nodes and devices
        read <node> <sensor>         Read a sensor
        status <node> <actuator>     Check actuator state
        on <node> <actuator>         Turn an actuator on
        off <node> <actuator>        Turn an actuator off
        help                         Show this menu
        exit                         Close the control panel
      """;
  private static final String ID = "[0-9]{1,10}";
  private static final int CACHED_READING_LIMIT = 100;

  /** Private constructor to prevent instantiation of the ControlPanelApplication class. */
  private ControlPanelApplication() {}

  /**
   * The main entry point for the control panel application.
   *
   * @param args the command-line arguments
   * @throws IOException if local configuration cannot be read
   * @throws InterruptedException if interrupted while retrying a read
   */
  static void main(String[] args) throws IOException, InterruptedException {
    String userId = args.length == 0 ? "owner-example" : args[0];
    String tokenName = LocalConfiguration.loadPanelUsers().getProperty(userId + ".token-env");
    if (tokenName == null || tokenName.isEmpty()) {
      System.err.println("Unknown panel user. Check config/panel-users.properties.");
      return;
    }
    var configuration = LocalConfiguration.loadCredentials();
    String token = configuration.get(tokenName);
    if (token == null || !token.matches("[A-Za-z0-9._-]{1,128}")) {
      System.err.println(
          "Set "
              + tokenName
              + " in config/local.properties or the environment. See docs/running.md.");
      return;
    }

    var serverAddress = LocalConfiguration.loadServerAddress(configuration);
    var sockets = TlsContexts.forClient(configuration).getSocketFactory();
    try (var server =
            new PanelConnection(serverAddress, sockets, "HELLO_PANEL;1;" + userId + ";" + token);
        Scanner console = new Scanner(System.in, StandardCharsets.UTF_8)) {
      if (!server.start()) {
        return;
      }

      var readings = new LinkedHashMap<String, CachedReading>();
      System.out.println();
      System.out.println("Smart Home | " + userId);
      System.out.print(USAGE);
      while (true) {
        System.out.print("\nsha> ");
        System.out.flush();
        if (!console.hasNextLine()) {
          return;
        }
        String input = console.nextLine().strip();
        if (input.isEmpty()) {
          continue;
        }
        String[] command = input.split("\\s+");
        if (command.length == 1 && command[0].equals("exit")) {
          return;
        }
        if (command.length == 1 && command[0].equals("help")) {
          System.out.print(USAGE);
          continue;
        }
        String request = toRequest(command);
        if (request == null) {
          System.out.println("Invalid command. Type 'help' to see the available commands.");
          continue;
        }
        String response =
            request.startsWith("SET_ACTUATOR;") ? server.control(request) : server.read(request);
        if (response.startsWith("NODES;")) {
          showConnectedNodes(response, server);
        } else if (request.startsWith("GET_READING;")) {
          showReading(request, response, readings);
        } else {
          showResponse(response);
        }
      }
    }
  }

  private static void showReading(
      String request, String response, LinkedHashMap<String, CachedReading> readings) {
    if (response.startsWith("READING;")) {
      readings.putLast(request, new CachedReading(response, Instant.now()));
      if (readings.size() > CACHED_READING_LIMIT) {
        readings.pollFirstEntry();
      }
      showResponse(response);
    } else if (PanelConnection.isTemporaryFailure(response) && readings.containsKey(request)) {
      CachedReading cached = readings.get(request);
      System.out.println("  Latest reading unavailable (" + response + ").");
      System.out.println("  Showing cached reading from " + cached.receivedAt() + ".");
      showResponse(cached.response());
    } else {
      showResponse(response);
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

    int nodeId = 0;
    try {
      nodeId = Integer.parseInt(command[1]);
    } catch (NumberFormatException e) {
      return null;
    }
    int deviceId = 0;
    try {
      deviceId = Integer.parseInt(command[2]);
    } catch (NumberFormatException e) {
      return null;
    }
    return switch (command[0]) {
      case "read" -> "GET_READING;" + nodeId + ";" + deviceId;
      case "on" -> "SET_ACTUATOR;" + nodeId + ";" + deviceId + ";ON";
      case "off" -> "SET_ACTUATOR;" + nodeId + ";" + deviceId + ";OFF";
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
      SensorType sensorType = SensorType.valueOf(fields[3]);
      double value = Double.parseDouble(fields[4]);

      System.out.println(
          "Node "
              + fields[1]
              + ", sensor "
              + fields[2]
              + ": "
              + fields[4]
              + " "
              + sensorType.getUnit());

      if (sensorType == SensorType.TEMPERATURE && AnomalyDetector.isTemperatureColdAnomaly(value)) {
        System.out.println("WARNING: Unusually cold temperature.");
      } else if (sensorType == SensorType.TEMPERATURE
          && AnomalyDetector.isTemperatureHotAnomaly(value)) {
        System.out.println("WARNING: Unusually hot temperature.");
      } else if (sensorType == SensorType.HUMIDITY
          && AnomalyDetector.isHumidityHighAnomaly(value)) {
        System.out.println("WARNING: Unusually high humidity.");
      }
    } else if (fields.length == 5 && fields[0].equals("ACTUATOR_STATE")) {
      System.out.println(
          "Node " + fields[1] + ", actuator " + fields[2] + " (" + fields[3] + "): " + fields[4]);
    } else if (response.equals("NODES")) {
      System.out.println("No connected sensor nodes.");
    } else if (response.startsWith("ERROR;")) {
      String message =
          switch (response) {
            case "ERROR;FORBIDDEN" -> "Your account does not have access to this device or action.";
            case "ERROR;NODE_UNAVAILABLE" -> "The node is not connected.";
            case "ERROR;SERVER_UNAVAILABLE" -> "The panel cannot reach the server.";
            case "ERROR;UNKNOWN_SENSOR" -> "That sensor ID does not exist on this node.";
            case "ERROR;UNKNOWN_ACTUATOR" -> "That actuator ID does not exist on this node.";
            case "ERROR;BUSY" -> "The node is busy. Try again.";
            case "ERROR;TIMEOUT" -> "The node did not respond in time.";
            default -> "The request could not be completed.";
          };
      System.out.println(message + " (" + response + ")");
    } else {
      System.out.println(response);
    }
  }

  /**
   * Displays the list of connected sensor nodes and their devices.
   *
   * @param response the response from the server containing the list of nodes
   * @param server the control panel's connection to the server
   * @throws InterruptedException if interrupted while retrying a read
   */
  private static void showConnectedNodes(String response, PanelConnection server)
      throws InterruptedException {
    System.out.println("Connected nodes");
    for (String nodeId : response.substring(6).split(";")) {
      System.out.println();
      System.out.println("  Node " + nodeId);
      String devices = server.read("LIST_DEVICES;" + nodeId);
      showDevices(nodeId, devices);
      if (PanelConnection.SERVER_UNAVAILABLE.equals(devices)) {
        break;
      }
    }
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
      System.out.println("    Could not list devices: " + response);
      return;
    }
    if (fields.length == 2) {
      System.out.println("    No devices.");
      return;
    }
    System.out.printf("    %-10s %-10s %s%n", "Device", "ID", "Type");
    for (int i = 2; i < fields.length; i++) {
      String[] device = fields[i].split(":", -1);
      if (device.length != 3) {
        System.out.println("    Invalid device: " + fields[i]);
        continue;
      }
      String category = device[0].equals("SENSOR") ? "Sensor" : "Actuator";
      System.out.printf("    %-10s %-10s %s%n", category, device[1], device[2]);
    }
  }

  private record CachedReading(String response, Instant receivedAt) {}
}
