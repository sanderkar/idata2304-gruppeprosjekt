package no.ntnu.idata2304.smarthome.controlpanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration.ServerAddress;
import no.ntnu.idata2304.smarthome.common.network.ClientConnection;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.common.security.TlsContexts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class PanelConnectionTest {
  private static final String HELLO = "HELLO_PANEL;1;owner-example;test-token";
  private static final String READING_REQUEST = "GET_READING;12;1";
  private static final String READING = "READING;12;1;TEMPERATURE;22.00";
  private static final String CONTROL = "SET_ACTUATOR;12;1;ON";
  private static final String STATUS_REQUEST = "GET_ACTUATOR;12;1";
  private static final String STATUS = "ACTUATOR_STATE;12;1;HEATER;ON";
  private static final String HOST = "127.0.0.1";
  private static final String TEST_PASSWORD = "test-password";

  @TempDir static Path directory;
  private static Identity trustedServer;
  private static Identity wrongHostServer;

  @BeforeAll
  static void createTlsIdentities() throws Exception {
    trustedServer = createIdentity("trusted", "ip:" + HOST);
    wrongHostServer = createIdentity("wrong-host", "dns:wrong.example");
  }

  @Test
  void rejectsAnUntrustedServer() throws Exception {
    assertRejected(trustedServer.server(), wrongHostServer.client());
  }

  @Test
  void rejectsATrustedCertificateForADifferentHost() throws Exception {
    assertRejected(wrongHostServer.server(), wrongHostServer.client());
  }

  @Test
  @Timeout(20)
  void retriesAnInterruptedReadAndDoesNotReplayAnUnconfirmedControlCommand() throws Exception {
    try (var listener = listen(trustedServer.server())) {
      var server = new FutureTask<Void>(() -> {
        serveSession(listener, new Exchange(READING_REQUEST, "READING;12;1;TEMPERATURE;2"));
        serveSession(listener,
            new Exchange(READING_REQUEST, READING + "\n"), new Exchange(CONTROL, null));
        serveSession(listener, new Exchange(STATUS_REQUEST, STATUS + "\n"));
        return null;
      });
      Thread.startVirtualThread(server);

      try (var panel = new PanelConnection(
          new ServerAddress(HOST, listener.getLocalPort()),
          trustedServer.client().getSocketFactory(), HELLO)) {
        assertTrue(panel.start());
        assertEquals(READING, panel.read(READING_REQUEST));
        assertTrue(panel.control(CONTROL).startsWith("Command outcome unknown."));
        assertEquals(STATUS, panel.read(STATUS_REQUEST));
      }
      server.get(5, TimeUnit.SECONDS);
    }
  }

  private record Exchange(String request, String reply) {}

  private record Identity(SSLContext server, SSLContext client) {}

  private static SSLServerSocket listen(SSLContext context) throws IOException {
    var listener = (SSLServerSocket) context.getServerSocketFactory().createServerSocket();
    listener.setEnabledProtocols(new String[] {TlsContexts.PROTOCOL});
    listener.bind(new InetSocketAddress(HOST, 0));
    listener.setSoTimeout(8_000);
    return listener;
  }

  private static void assertRejected(SSLContext server, SSLContext client) throws Exception {
    try (var listener = listen(server)) {
      var handshake = new FutureTask<Void>(() -> {
        try (var socket = (SSLSocket) listener.accept()) {
          socket.setSoTimeout(5_000);
          var failure = assertThrows(IOException.class, socket::startHandshake);
          assertTrue(failure instanceof SSLException || failure instanceof SocketException,
              () -> "Unexpected server-side handshake error: " + failure);
        }
        return null;
      });
      Thread.startVirtualThread(handshake);
      var address = new ServerAddress(HOST, listener.getLocalPort());
      assertThrows(
          SSLHandshakeException.class,
          () -> ClientConnection.connect(address, client.getSocketFactory(), HELLO, 1_000));
      handshake.get(5, TimeUnit.SECONDS);
    }
  }

  private static void serveSession(SSLServerSocket listener, Exchange... exchanges)
      throws IOException {
    try (var socket = (SSLSocket) listener.accept()) {
      socket.setSoTimeout(8_000);
      socket.startHandshake();
      assertEquals("TLSv1.3", socket.getSession().getProtocol());
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

  private static Identity createIdentity(String name, String san) throws Exception {
    Path keystore = directory.resolve(name + ".p12");
    Path certificate = directory.resolve(name + ".crt");
    keytool(
        "-genkeypair", "-alias", name, "-keyalg", "EC", "-groupname", "secp256r1",
        "-dname", "CN=" + name, "-ext", "SAN=" + san, "-ext", "EKU=serverAuth",
        "-storetype", "PKCS12", "-keystore", keystore.toString(), "-storepass", TEST_PASSWORD);
    keytool(
        "-exportcert", "-alias", name, "-keystore", keystore.toString(), "-storepass",
        TEST_PASSWORD, "-rfc", "-file", certificate.toString());
    return new Identity(
        TlsContexts.forServer(Map.of(
            "SMART_HOME_TLS_KEYSTORE", keystore.toString(),
            "SMART_HOME_TLS_KEYSTORE_PASSWORD", TEST_PASSWORD)),
        TlsContexts.forClient(Map.of("SMART_HOME_TLS_CERTIFICATE", certificate.toString())));
  }

  private static void keytool(String... arguments) throws Exception {
    String executable = System.getProperty("os.name").startsWith("Windows")
        ? "keytool.exe" : "keytool";
    var command = new ArrayList<String>();
    command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
    command.addAll(List.of(arguments));
    Path log = Files.createTempFile(directory, "keytool", ".log");
    var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile())
        .start();
    try {
      assertTrue(process.waitFor(10, TimeUnit.SECONDS), "keytool timed out");
      assertEquals(0, process.exitValue(), Files.readString(log));
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }
}
