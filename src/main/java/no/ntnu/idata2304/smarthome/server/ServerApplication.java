package no.ntnu.idata2304.smarthome.server;

import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration;
import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Entry point for the server application.
 */
public final class ServerApplication {

  private final ServerSocket serverSocket;
  private final Properties panelUsers;
  private final Map<String, String> credentials;
  private final ConcurrentMap<Integer, NodeConnection> nodes = new ConcurrentHashMap<>();

  public ServerApplication(int port) throws IOException {
    this("0.0.0.0", port);
  }

  public ServerApplication(String host, int port) throws IOException {
    this.panelUsers = LocalConfiguration.loadPanelUsers();
    this.credentials = LocalConfiguration.loadCredentials();
    this.serverSocket = new ServerSocket();
    this.serverSocket.bind(new InetSocketAddress(host, port));
  }

  /** Supplies configuration and a socket for isolated tests  */
  ServerApplication(ServerSocket serverSocket, Properties panelUsers, Map<String, String> environment) {
    this.serverSocket = serverSocket;
    this.panelUsers = panelUsers;
    this.credentials = environment;
  }

  /**
   * Starts the server application and listens for incoming client connections.
   *
   * @param args command-line arguments (not used)
   * @throws IOException if an I/O error occurs when opening the socket
   */

   static void main(String[] args) throws IOException {
     var serverAddress = LocalConfiguration.loadServerAddress();
     ServerApplication server =
         new ServerApplication(serverAddress.host(), serverAddress.port());
     System.out.println("Server started at " + serverAddress.host() + ":"
         + serverAddress.port() + ". Waiting for clients to connect...");

    try (ServerSocket listener = server.serverSocket) {
      while (true) {
        Socket client = listener.accept();

        Thread.startVirtualThread(() -> {
          try (client) {
            server.handleClient(client);
          } catch (IOException exception) {
            System.err.println("Client connection ended with an I/O error.");
          }
        });
      }
    }
  }

  /**
   * Handles the communication with a connected client.
   *
   * @param client the connected client socket
   * @throws IOException if an I/O error occurs during communication
   */

  private void handleClient(Socket client) throws IOException {
    System.out.println("Client connected: " + client.getInetAddress().getHostAddress());
    var reader = new BufferedReader(
        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
    var writer = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8);

    String message = Protocol.readLine(reader);
    if (message == null) {
      return;
    }

    switch (message.split(";", 2)[0]) {
      case "HELLO_NODE" -> handleNode(client, reader, writer, message);
      case "HELLO_PANEL" -> handlePanel(reader, writer, message);
      default -> Protocol.writeLine(writer, "ERROR;BAD_STATE");
    }
  }

  /**
   * Handles the registration and communication with a connected sensor node.
   *
   * @param client the connected client socket
   * @param reader the BufferedReader to read messages from the sensor node
   * @param writer the OutputStreamWriter to send messages to the sensor node
   * @param message the initial registration message from the sensor node
   * @throws IOException if an I/O error occurs during communication
   */

  private void handleNode(Socket client, BufferedReader reader, OutputStreamWriter writer,
                          String message) throws IOException {
    final int nodeId;
    boolean heartbeat = message.endsWith(Protocol.HEARTBEAT_SUFFIX);
    String registrationMessage = heartbeat
        ? message.substring(0, message.length() - Protocol.HEARTBEAT_SUFFIX.length()) : message;
    try {
      var registration = MessageDecoder.decodeNodeRegistration(registrationMessage);
      nodeId = registration.getNodeId();
      String expectedToken = credentials.get("SMART_HOME_NODE_" + nodeId + "_TOKEN");
      if (!registration.getToken().equals(expectedToken)) {
        Protocol.writeLine(writer, "ERROR;AUTH_FAILED");
        return;
      }
    } catch (IllegalArgumentException exception) {
      Protocol.writeLine(writer, "ERROR;BAD_FORMAT");
      return;
    }

    var node = new NodeConnection(client, writer);
    if (nodes.putIfAbsent(nodeId, node) != null) {
      Protocol.writeLine(writer, "ERROR;DUPLICATE_NODE");
      return;
    }

    Thread heartbeatThread = null;
    try {
      node.sendReady();
      System.out.println("Registered node " + nodeId);
      if (heartbeat) {
        heartbeatThread = Thread.startVirtualThread(node::keepAlive);
      }
      String response;
      while ((response = Protocol.readLine(reader)) != null && node.receiveResponse(response)) {
        // The panel thread waiting for this response handles the result
      }
    } finally {
      nodes.remove(nodeId, node);
      node.markDisconnected();
      if (heartbeatThread != null) {
        heartbeatThread.interrupt();
      }
    }
  }

  /**
   * Handles the registration and communication with a connected control panel.
   *
   * @param reader the BufferedReader to read messages from the control panel
   * @param writer the OutputStreamWriter to send messages to the control panel
   * @param message the initial registration message from the control panel
   * @throws IOException if an I/O error occurs during communication
   */

  void handlePanel(BufferedReader reader, OutputStreamWriter writer, String message)
      throws IOException {
    String[] fields = message.split(";", -1);
    if (fields.length != 4 || !fields[1].equals("1") || fields[3].isEmpty()) {
      Protocol.writeLine(writer, "ERROR;BAD_FORMAT");
      return;
    }
    String userId = fields[2];
    String role = panelUsers.getProperty(userId + ".role", "");
    String tokenVariable = panelUsers.getProperty(userId + ".token-env", "");
    boolean knownUser = Arrays.stream(panelUsers.getProperty("users", "").split(","))
        .map(String::trim).anyMatch(userId::equals);
    if (!knownUser || !Set.of("SHA", "OWNER", "GUEST", "TECHNICIAN").contains(role)
        || tokenVariable.isEmpty() || !fields[3].equals(credentials.get(tokenVariable))) {
      Protocol.writeLine(writer, "ERROR;AUTH_FAILED");
      return;
    }

    Protocol.writeLine(writer, "READY;1");
    String request;
    while ((request = Protocol.readLine(reader)) != null) {
      Protocol.writeLine(writer, respondToPanel(request, userId));
    }
  }

  /**
   * Responds to a request from the control panel.
   *
   * @param request the request message from the control panel
   * @param userId the authenticated control-panel user
   * @return the response message to send back to the control panel
   */
  private String respondToPanel(String request, String userId) {
    if (Protocol.PING.equals(request)) {
      return Protocol.PONG;
    }
    if (request.equals("LIST_NODES")) {
      var response = new StringJoiner(";").add("NODES");
      nodes.entrySet().stream()
          .filter(entry -> entry.getValue().isActive())
          .map(entry -> entry.getKey())
          .sorted()
          .forEach(id -> response.add(id.toString()));
      return response.toString();
    }
    try {
      return switch (request.split(";", 2)[0]) {
        case "LIST_DEVICES" -> listDevices(MessageDecoder.decodeListDevices(request));
        case "GET_READING" -> {
          var reading = MessageDecoder.decodeGetReading(request);
          if (!canAccess(userId, "sensors", reading.nodeId(), reading.sensorId(), false)) {
            yield "ERROR;FORBIDDEN";
          }
          NodeConnection node = nodes.get(reading.nodeId());
          yield node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestReading(reading);
        }
        case "GET_ACTUATOR" -> {
          var actuator = MessageDecoder.decodeGetActuator(request);
          if (!canAccess(userId, "actuators", actuator.nodeId(), actuator.actuatorId(), false)) {
            yield "ERROR;FORBIDDEN";
          }
          NodeConnection node = nodes.get(actuator.nodeId());
          yield node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestActuatorState(actuator);
        }
        case "SET_ACTUATOR" -> {
          var command = MessageDecoder.decodeActuator(request);

          if (!canAccess(userId, "actuators",
              command.nodeId(), command.actuatorId(), true)) {
            yield "ERROR;FORBIDDEN";
          }

          NodeConnection node = nodes.get(command.nodeId());
          yield node == null
              ? "ERROR;NODE_UNAVAILABLE"
              : node.requestSetActuator(
              command.nodeId(), command.actuatorId(), command.state());
        }
        case "TURN_ON_HEATER" -> {
          var command = MessageDecoder.decodeTurnOnHeater(request);
          if (!canAccess(userId, "actuators", command.getNodeId(), command.getHeaterId(), true)) {
            yield "ERROR;FORBIDDEN";
          }
          NodeConnection node = nodes.get(command.getNodeId());
          yield node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestTurnOnHeater(command);
        }
        case "TURN_OFF_HEATER" -> {
          var command = MessageDecoder.decodeTurnOffHeater(request);
          if (!canAccess(userId, "actuators", command.getNodeId(), command.getHeaterId(), true)) {
            yield "ERROR;FORBIDDEN";
          }
          NodeConnection node = nodes.get(command.getNodeId());
          yield node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestTurnOffHeater(command);
        }
        default -> "ERROR;UNKNOWN_COMMAND";
      };
    } catch (IllegalArgumentException exception) {
      return "ERROR;BAD_FORMAT";
    }
  }

  /**
   * Lists the devices for a specific sensor node.
   *
   * @param nodeId the ID of the sensor node
   * @return the response message containing the list of devices
   */
  private String listDevices(int nodeId) {
    NodeConnection node = nodes.get(nodeId);
    return node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestDevices(nodeId);
  }

  /** Checks server-configured roles and guest device grants.
   *  Code made partly using AI assistance and later reviewed by the team members.
   */
  private boolean canAccess(String userId, String devices, int nodeId, int deviceId,
                            boolean controlling) {
    return switch (panelUsers.getProperty(userId + ".role", "")) {
      case "SHA", "OWNER" -> true;
      case "TECHNICIAN" -> !controlling;
      case "GUEST" -> Arrays.stream(panelUsers.getProperty(userId + "." + devices, "").split(","))
          .map(String::trim).anyMatch((nodeId + ":" + deviceId)::equals);
      default -> false;
    };
  }
}
