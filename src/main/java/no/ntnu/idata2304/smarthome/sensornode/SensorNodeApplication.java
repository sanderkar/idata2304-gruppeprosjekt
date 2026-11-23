package no.ntnu.idata2304.smarthome.sensornode;

import java.io.IOException;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.function.Predicate;
import javax.net.ssl.SSLHandshakeException;
import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration;
import no.ntnu.idata2304.smarthome.common.network.ClientConnection;
import no.ntnu.idata2304.smarthome.common.network.ClientConnection.RegistrationRejected;
import no.ntnu.idata2304.smarthome.common.protocol.AutomaticNodeRegistration;
import no.ntnu.idata2304.smarthome.common.protocol.MessageDecoder;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.common.security.TlsContexts;
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
   * @param args an optional static node ID (default 12) or {@code --auto DEVICE_NAME}
   * @throws IOException if local configuration cannot be read
   * @throws InterruptedException if interrupted while waiting to reconnect
   */
  static void main(String[] args) throws IOException, InterruptedException {
    boolean automatic = args.length > 0 && args[0].equals("--auto");
    int staticNodeId = 12;
    String tokenName;
    if (automatic) {
      if (args.length != 2 || !AutomaticNodeRegistration.isDeviceName(args[1])) {
        System.err.println("Use --auto with an uppercase device name such as KITCHEN.");
        return;
      }
      tokenName = AutomaticNodeRegistration.tokenSetting(args[1]);
    } else {
      try {
        staticNodeId = args.length == 0 ? 12 : Integer.parseInt(args[0]);
        if (staticNodeId < 0 || args.length > 1) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException exception) {
        System.err.println("Use a non-negative node ID or --auto DEVICE_NAME.");
        return;
      }
      tokenName = "SMART_HOME_NODE_" + staticNodeId + "_TOKEN";
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
    String hello = automatic
        ? new AutomaticNodeRegistration(args[1], token).encode()
        : "HELLO_NODE;1;" + staticNodeId + ";" + token + Protocol.HEARTBEAT_SUFFIX;
    Predicate<String> isReady = automatic ? AutomaticNodeRegistration::isReady : "READY;1"::equals;
    var heater = new Heater(1);
    var fan = new Fan(2);
    var temperature = SensorFactory.create("TEMPERATURE", 1, 22.0);
    var humidity = SensorFactory.create("HUMIDITY", 2, 50.0);
    SensorNode node = null;

    long retryDelay = 1_000;
    boolean reportedHandshakeFailure = false;
    while (!Thread.currentThread().isInterrupted()) {
      try (var server = ClientConnection.connect(serverAddress, sockets, hello, 15_000, isReady)) {
        int nodeId = automatic
            ? AutomaticNodeRegistration.readAddress(server.registration()) : staticNodeId;
        if (node == null || node.getNodeId() != nodeId) {
          node = new SensorNode(nodeId);
          node.addSensor(temperature);
          node.addSensor(humidity);
          node.addHeater(heater);
          node.addActuator(fan);
        }
        System.out.println("Connected to server at " + serverAddress.host() + ":"
            + serverAddress.port());
        System.out.println("Received response from server: " + server.registration());
        if (automatic) {
          System.out.println("Assigned node address " + nodeId + " to " + args[1] + ".");
        }
        retryDelay = 1_000;
        reportedHandshakeFailure = false;
        while (!Thread.currentThread().isInterrupted()) {
          String command = server.read();
          server.send(Protocol.PING.equals(command)
              ? Protocol.PONG : respondToCommand(command, node, heater));
        }
        return;
      } catch (RegistrationRejected exception) {
        if (!"ERROR;DUPLICATE_NODE".equals(exception.getMessage())) {
          System.err.println("Node registration failed: " + exception.getMessage() + ".");
          if ("ERROR;AUTH_FAILED".equals(exception.getMessage())) {
            System.err.println("Check " + tokenName + " against the server configuration.");
          }
          return;
        }
      } catch (SSLHandshakeException exception) {
        if (!reportedHandshakeFailure) {
          System.err.println("TLS handshake failed. Check the server certificate and SMART_HOME_HOST.");
          reportedHandshakeFailure = true;
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
