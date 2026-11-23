package no.ntnu.idata2304.smarthome.controlpanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration.ServerAddress;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class PanelConnectionTest {
  private static final String HELLO = "HELLO_PANEL;1;owner-example;test-token";
  private static final String READING_REQUEST = "GET_READING;12;1";
  private static final String READING = "READING;12;1;TEMPERATURE;22.00";
  private static final String CONTROL = "SET_ACTUATOR;12;1;ON";
  private static final String STATUS_REQUEST = "GET_ACTUATOR;12;1";
  private static final String STATUS = "ACTUATOR_STATE;12;1;HEATER;ON";

  @Test
  @Timeout(20)
  void retriesAnInterruptedReadAndDoesNotReplayAnUnconfirmedControlCommand() throws Exception {
    var loopback = InetAddress.getLoopbackAddress();
    try (var listener = new ServerSocket(0, 3, loopback);
        var panel = new PanelConnection(
            new ServerAddress(loopback.getHostAddress(), listener.getLocalPort()), HELLO)) {
      listener.setSoTimeout(8_000);
      var server = new FutureTask<Void>(() -> {
        serveSession(listener, new Exchange(READING_REQUEST, "READING;12;1;TEMPERATURE;2"));
        serveSession(listener,
            new Exchange(READING_REQUEST, READING + "\n"), new Exchange(CONTROL, null));
        serveSession(listener, new Exchange(STATUS_REQUEST, STATUS + "\n"));
        return null;
      });
      Thread.startVirtualThread(server);

      assertTrue(panel.start());
      assertEquals(READING, panel.read(READING_REQUEST));
      assertTrue(panel.control(CONTROL).startsWith("Command outcome unknown."));
      assertEquals(STATUS, panel.read(STATUS_REQUEST));
      server.get(1, TimeUnit.SECONDS);
    }
  }

  private record Exchange(String request, String reply) {}

  private static void serveSession(ServerSocket listener, Exchange... exchanges) throws IOException {
    try (var socket = listener.accept()) {
      socket.setSoTimeout(8_000);
      var reader =
          new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      var writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
      assertEquals(HELLO, reader.readLine());
      Protocol.writeLine(writer, "READY;1");
      for (Exchange exchange : exchanges) {
        String request = reader.readLine();
        while (Protocol.PING.equals(request)) {
          Protocol.writeLine(writer, Protocol.PONG);
          request = reader.readLine();
        }
        assertEquals(exchange.request(), request);
        if (exchange.reply() != null) {
          writer.write(exchange.reply());
          writer.flush();
        }
      }
    }
  }
}
