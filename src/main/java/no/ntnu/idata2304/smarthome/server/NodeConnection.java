package no.ntnu.idata2304.smarthome.server;

import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder.ReadingRequest;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;

import java.io.IOException;
import java.io.Writer;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Coordinates requests from panel threads with replies read by the node's connection thread.
 * Code Assisted by an AI.
 */
final class NodeConnection {
  private final Socket socket;
  private final Writer writer;
  private boolean active;
  private CompletableFuture<String> pending;

  NodeConnection(Socket socket, Writer writer) {
    this.socket = socket;
    this.writer = writer;
  }

  synchronized void sendReady() throws IOException {
    Protocol.writeLine(writer, "READY;1");
    active = true;
  }

  synchronized boolean isActive() {
    return active;
  }

  synchronized boolean receiveResponse(String response) {
    return pending != null && pending.complete(response);
  }

  synchronized void markDisconnected() {
    active = false;
    if (pending != null) {
      pending.complete("ERROR;NODE_UNAVAILABLE");
    }
  }

  String requestReading(ReadingRequest request) {
    String command = "GET_READING;" + request.nodeId() + ";" + request.sensorId();
    String expectedPrefix = "READING;" + request.nodeId() + ";" + request.sensorId()
        + ";TEMPERATURE;";
    return request(command, reply -> isTemperatureReading(expectedPrefix, reply));
  }

  String requestDevices(int nodeId) {
    return request("LIST_DEVICES;" + nodeId, reply ->
        reply.matches("DEVICES;" + nodeId + "(;(SENSOR|ACTUATOR):[0-9]+:[A-Z][A-Z0-9_]*)*"));
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

  private String validateReply(String reply, Predicate<String> isValidReply) {
    if (reply.equals("ERROR;TIMEOUT")) {
      closeConnection();
      return reply;
    }
    if (reply.equals("ERROR;NODE_UNAVAILABLE") || reply.equals("ERROR;UNKNOWN_SENSOR")
        || reply.equals("ERROR;WRONG_NODE") || isValidReply.test(reply)) {
      return reply;
    }

    closeConnection();
    return "ERROR;BAD_RESPONSE";
  }

  private static boolean isTemperatureReading(String expectedPrefix, String reply) {
    if (!reply.startsWith(expectedPrefix)) {
      return false;
    }
    String value = reply.substring(expectedPrefix.length());
    return value.matches("-?[0-9]+(\\.[0-9]+)?") && Double.isFinite(Double.parseDouble(value));
  }

  private void closeConnection() {
    markDisconnected();
    try {
      socket.close();
    } catch (IOException exception) {
      System.err.println("Could not close the node connection.");
    }
  }
}
