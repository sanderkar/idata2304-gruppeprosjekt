package no.ntnu.idata2304.smarthome.controlpanel;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration.ServerAddress;
import no.ntnu.idata2304.smarthome.common.network.ClientConnection;
import no.ntnu.idata2304.smarthome.common.network.ClientConnection.RegistrationRejected;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;

/** Maintains the panel session and serializes all traffic on it. */
final class PanelConnection implements AutoCloseable {
  static final String SERVER_UNAVAILABLE = "ERROR;SERVER_UNAVAILABLE";
  private static final long READ_RETRY_WINDOW = Duration.ofSeconds(15).toNanos();
  private final ServerAddress address;
  private final String hello;
  private final ScheduledExecutorService maintenance =
      Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
  private ClientConnection connection;
  private String registrationError;
  private boolean recovering;

  PanelConnection(ServerAddress address, String hello) {
    this.address = address;
    this.hello = hello;
  }

  static boolean isTemporaryFailure(String response) {
    return response.equals(SERVER_UNAVAILABLE)
        || response.equals("ERROR;NODE_UNAVAILABLE")
        || response.equals("ERROR;TIMEOUT")
        || response.equals("ERROR;BUSY");
  }

  /** Attempts registration immediately and starts checking for outages in the background. */
  synchronized boolean start() {
    maintainConnection();
    if (registrationError != null) {
      return false;
    }
    maintenance.scheduleWithFixedDelay(this::maintainConnection, 2, 2, TimeUnit.SECONDS);
    return true;
  }

  private synchronized void maintainConnection() {
    if (registrationError != null) {
      return;
    }
    try {
      if (connection == null) {
        connection = ClientConnection.connect(address, hello, 10_000);
        if (recovering) {
          System.out.println("Control panel reconnected to the server.");
          recovering = false;
        }
      } else if (!Protocol.PONG.equals(connection.request(Protocol.PING))) {
        throw new IOException("Unexpected heartbeat response");
      }
    } catch (RegistrationRejected exception) {
      registrationError = exception.getMessage();
      System.err.println(
          "Panel registration failed: "
              + registrationError
              + ". Check the selected account and server credentials.");
    } catch (IOException exception) {
      disconnect();
    }
  }

  /** Keeps one read-only request pending while either connection recovers. */
  String read(String request) throws InterruptedException {
    long started = System.nanoTime();
    String response = readOnce(request);
    while (isTemporaryFailure(response) && System.nanoTime() - started < READ_RETRY_WINDOW) {
      Thread.sleep(500);
      response = readOnce(request);
    }
    return response;
  }

  private synchronized String readOnce(String request) {
    if (connection == null) {
      return registrationError == null ? SERVER_UNAVAILABLE : registrationError;
    }
    try {
      return connection.request(request);
    } catch (IOException exception) {
      disconnect();
      return SERVER_UNAVAILABLE;
    }
  }

  /** Sends an actuator command once, reporting uncertainty if confirmation is lost. */
  synchronized String control(String request) {
    if (connection == null) {
      return "Command not sent. "
          + (registrationError == null ? SERVER_UNAVAILABLE : registrationError);
    }
    try {
      String response = connection.request(request);
      if (response.equals("ERROR;NODE_UNAVAILABLE")
          || response.equals("ERROR;TIMEOUT")
          || response.equals("ERROR;BAD_RESPONSE")) {
        return "Command outcome unknown (" + response + "). Check actuator status.";
      }
      return response;
    } catch (IOException exception) {
      disconnect();
      return "Command outcome unknown. Check actuator status after reconnecting.";
    }
  }

  private void disconnect() {
    closeSocket();
    if (!recovering) {
      System.err.println("Server connection unavailable. Reconnecting in the background.");
      recovering = true;
    }
  }

  private void closeSocket() {
    if (connection != null) {
      try {
        connection.close();
      } catch (IOException exception) {
        System.err.println("Could not close the panel connection.");
      } finally {
        connection = null;
      }
    }
  }

  @Override
  public void close() {
    maintenance.shutdownNow();
    synchronized (this) {
      closeSocket();
    }
  }
}
