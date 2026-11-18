package no.ntnu.idata2304.smarthome.server;

import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Entry point for the server application.
 */
public final class ServerApplication {

  private final ServerSocket serverSocket;
  private final ConcurrentMap<Integer, NodeConnection> nodes = new ConcurrentHashMap<>();

  public ServerApplication(int port) throws IOException {
    this.serverSocket = new ServerSocket(port);
  }

  /**
   * Starts the server application and listens for incoming client connections.
   *
   * @param args command-line arguments (not used)
   * @throws IOException if an I/O error occurs when opening the socket
   */

  public static void main(String[] args) throws IOException {
    ServerApplication server = new ServerApplication(5000);
    System.out.println("Server started on port 5000. Waiting for clients to connect...");

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

    String message = reader.readLine();
    if (message == null) {
      return;
    }

    switch (message.split(";", 2)[0]) {
      case "HELLO_NODE" -> handleNode(client, reader, writer, message);
      case "HELLO_PANEL" -> handlePanel(reader, writer, message);
      default -> Protocol.writeLine(writer, "ERROR;BAD_STATE");
    }
  }

  private void handleNode(Socket client, BufferedReader reader, OutputStreamWriter writer,
                          String message) throws IOException {
    final int nodeId;
    try {
      var registration = MessageDecoder.decodeNodeRegistration(message);
      nodeId = registration.getNodeId();
      String expectedToken = System.getenv("SMART_HOME_NODE_" + nodeId + "_TOKEN");
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

    try {
      node.sendReady();
      System.out.println("Registered node " + nodeId);
      String response;
      while ((response = reader.readLine()) != null && node.receiveResponse(response)) {
        // The panel thread waiting for this response handles the result.
      }
    } finally {
      nodes.remove(nodeId, node);
      node.markDisconnected();
    }
  }

  private void handlePanel(BufferedReader reader, OutputStreamWriter writer, String message)
      throws IOException {
    String[] fields = message.split(";", -1);
    if (fields.length != 4 || !fields[1].equals("1") || fields[3].isEmpty()) {
      Protocol.writeLine(writer, "ERROR;BAD_FORMAT");
      return;
    }
    if (!fields[2].equals("owner-example")
        || !fields[3].equals(System.getenv("SMART_HOME_PANEL_TOKEN"))) {
      Protocol.writeLine(writer, "ERROR;AUTH_FAILED");
      return;
    }

    Protocol.writeLine(writer, "READY;1");
    String request;
    while ((request = reader.readLine()) != null) {
      Protocol.writeLine(writer, respondToPanel(request));
    }
  }

  private String respondToPanel(String request) {
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
          NodeConnection node = nodes.get(reading.nodeId());
          yield node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestReading(reading);
        }
        case "TURN_ON_HEATER" -> {
          var command = MessageDecoder.decodeTurnOnHeater(request);
          NodeConnection node = nodes.get(command.getNodeId());
          yield node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestTurnOnHeater(command);
        }
        case "TURN_OFF_HEATER" -> {
          var command = MessageDecoder.decodeTurnOffHeater(request);
          NodeConnection node = nodes.get(command.getNodeId());
          yield node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestTurnOffHeater(command);
        }
        default -> "ERROR;UNKNOWN_COMMAND";
      };
    } catch (IllegalArgumentException exception) {
      return "ERROR;BAD_FORMAT";
    }
  }

  private String listDevices(int nodeId) {
    NodeConnection node = nodes.get(nodeId);
    return node == null ? "ERROR;NODE_UNAVAILABLE" : node.requestDevices(nodeId);
  }
}
