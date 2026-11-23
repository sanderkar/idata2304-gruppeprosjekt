package no.ntnu.idata2304.smarthome.server;

import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder.ActuatorRequest;
import no.ntnu.idata2304.smarthome.common.model.SensorType;
import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder.ReadingRequest;
import no.ntnu.idata2304.smarthome.common.protocol.MessageEncoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.common.protocol.TurnOffHeaterCommand;
import no.ntnu.idata2304.smarthome.common.protocol.TurnOnHeaterCommand;

import java.io.IOException;
import java.io.Writer;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Coordinates requests from panel threads with replies read by the node's connection thread.
 * Code was assisted using AI and reviewed by the team members.
 */
final class NodeConnection {
  private final Socket socket;
  private final Writer writer;
  private boolean active;
  private CompletableFuture<String> pending;

  /**
   * Constructs a NodeConnection with the specified socket and writer.
   *
   * @param socket the socket representing the connection to the sensor node
   * @param writer the writer for sending messages to the sensor node
   */
  
  NodeConnection(Socket socket, Writer writer) {
    this.socket = socket;
    this.writer = writer;
  }
  
  /**
   * Sends the registration response to the node and then makes it available to panels.
   *
   * @param registrationReply the registration response for this node
   * @throws IOException if an I/O error occurs while sending the message
   */

  synchronized void sendReady(String registrationReply) throws IOException {
    Protocol.writeLine(writer, registrationReply);
    active = true;
  }
  
  /**
   * Checks if the connection to the sensor node is active.
   *
   * @return true if the connection is active, false otherwise
   */

  synchronized boolean isActive() {
    return active;
  }
  
  /**
   * Receives a response from the sensor node and completes the pending request if applicable.
   *
   * @param response the response received from the sensor node
   * @return true if there was a pending request and it was completed, false otherwise
   */

  synchronized boolean receiveResponse(String response) {
    return pending != null && pending.complete(response);
  }

  /**
   * Marks the connection to the sensor node as disconnected.
   */
  synchronized void markDisconnected() {
    active = false;
    if (pending != null) {
      pending.complete("ERROR;NODE_UNAVAILABLE");
    }
  }

  /** Probes idle nodes using the same request slot as panel commands. */
  void keepAlive() {
    try {
      while (isActive()) {
        Thread.sleep(3_000);
        String reply = request(Protocol.PING, Protocol.PONG::equals);
        if (!Protocol.PONG.equals(reply) && !"ERROR;BUSY".equals(reply)) {
          closeConnection();
          return;
        }
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }
  
  /**
   * Requests a sensor reading from the sensor node.
   *
   * @param request the reading request containing node ID and sensor ID
   * @return the response from the sensor node, or an error message if the request fails
   */

  String requestReading(ReadingRequest request) {
    String command = "GET_READING;" + request.nodeId() + ";" + request.sensorId();
    String expectedPrefix = "READING;" + request.nodeId() + ";" + request.sensorId()
        + ";";
    return request(command, reply -> isSensorReading(expectedPrefix, reply));
  }
  
  /**
   * Requests the list of devices from the sensor node.
   *
   * @param nodeId the ID of the sensor node
   * @return the response from the sensor node, or an error message if the request fails
   */

  String requestDevices(int nodeId) {
    return request("LIST_DEVICES;" + nodeId, reply ->
        reply.matches("DEVICES;" + nodeId + "(;(SENSOR|ACTUATOR):[0-9]+:[A-Z][A-Z0-9_]*)*"));
  }
  
  /**
   * Requests the current state of an actuator from the sensor node.
   *
   * @param request the actuator request containing node ID and actuator ID
   * @return the response from the sensor node, or an error message if the request fails
   */
  
  String requestActuatorState(ActuatorRequest request) {
    String actuatorAddress = request.nodeId() + ";" + request.actuatorId();
    String replyPattern = "ACTUATOR_STATE;" + actuatorAddress + ";[A-Z][A-Z0-9_]*;(ON|OFF)";
    return request("GET_ACTUATOR;" + actuatorAddress, reply -> reply.matches(replyPattern));
  }
  
  /**
   * Requests the sensor node to turn on a heater.
   *
   * @param command the command containing node ID and heater ID
   * @return the response from the sensor node, or an error message if the request fails
   */

  String requestTurnOnHeater(TurnOnHeaterCommand command) {
    String expected = heaterStateReply(command.getNodeId(), command.getHeaterId(), "ON");
    return request(MessageEncoder.encodeTurnOnHeater(command), reply -> reply.equals(expected));
  }
  
  /**
   * Requests the sensor node to turn off a heater.
   *
   * @param command the command containing node ID and heater ID
   * @return the response from the sensor node, or an error message if the request fails
   */

  String requestTurnOffHeater(TurnOffHeaterCommand command) {
    String expected = heaterStateReply(command.getNodeId(), command.getHeaterId(), "OFF");
    return request(MessageEncoder.encodeTurnOffHeater(command), reply -> reply.equals(expected));
  }

  /**
   * Requests a new state for any actuator on this sensor node.
   *
   * @param nodeId the sensor node ID
   * @param actuatorId the actuator ID
   * @param state the requested state, ON or OFF
   * @return the response from the sensor node
   */
  String requestSetActuator(int nodeId, int actuatorId, String state) {
    String command = "SET_ACTUATOR;" + nodeId + ";" + actuatorId + ";" + state;
    String expectedStart = "ACTUATOR_STATE;" + nodeId + ";" + actuatorId + ";";

    return request(command, reply ->
        reply.startsWith(expectedStart) && reply.endsWith(";" + state));
  }

  /**
   * Constructs a response message indicating the state of a heater.
   *
   * @param nodeId the ID of the sensor node
   * @param heaterId the ID of the heater
   * @param state the state of the heater ("ON" or "OFF")
   * @return the constructed response message
   */

  private static String heaterStateReply(int nodeId, int heaterId, String state) {
    return "ACTUATOR_STATE;" + nodeId + ";" + heaterId + ";HEATER;" + state;
  }

  private String request(String command, Predicate<String> isValidReply) {
    var response = new CompletableFuture<String>();
    synchronized (this) {
      if (!active) {
        return "ERROR;NODE_UNAVAILABLE";
      }
      if (pending != null) {
        return "ERROR;BUSY";
      }
      pending = response;
    }

    try {
      Protocol.writeLine(writer, command);
      String reply = response.completeOnTimeout("ERROR;TIMEOUT", 5, TimeUnit.SECONDS).join();
      return validateReply(reply, isValidReply);
    } catch (IOException exception) {
      closeConnection();
      return "ERROR;NODE_UNAVAILABLE";
    } finally {
      synchronized (this) {
        pending = null;
      }
    }
  }
  
  /**
   * Validates the reply received from the sensor node.
   *
   * @param reply the reply received from the sensor node
   * @param isValidReply a predicate to check if the reply is valid
   * @return the validated reply, or an error message if the reply is invalid
   */

  private String validateReply(String reply, Predicate<String> isValidReply) {
    if (reply.equals("ERROR;TIMEOUT")) {
      closeConnection();
      return reply;
    }
    if (reply.equals("ERROR;NODE_UNAVAILABLE") || reply.equals("ERROR;UNKNOWN_SENSOR")
        || reply.equals("ERROR;UNKNOWN_ACTUATOR") || reply.equals("ERROR;WRONG_NODE")
        || isValidReply.test(reply)) {
      return reply;
    }

    closeConnection();
    return "ERROR;BAD_RESPONSE";
  }
  
  /**
   * Checks if the reply from the sensor node is a valid sensor reading.
   *
   * @param expectedPrefix the expected prefix of the reply
   * @param reply the reply received from the sensor node
   * @return true if the reply is a valid sensor reading, false otherwise
   */

  private static boolean isSensorReading(String expectedPrefix, String reply) {
    if (!reply.startsWith(expectedPrefix)) {
      return false;
    }
    String[] fields = reply.substring(expectedPrefix.length()).split(";", -1);
    if (fields.length != 2 || !fields[1].matches("-?[0-9]+(\\.[0-9]+)?")) {
      return false;
    }
    try {
      return SensorType.valueOf(fields[0]).isValid(Double.parseDouble(fields[1]));
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
  
  /**
   * Closes the connection to the sensor node and marks it as disconnected.
   */

  private void closeConnection() {
    markDisconnected();
    try {
      socket.close();
    } catch (IOException exception) {
      System.err.println("Could not close the node connection.");
    }
  }
}
