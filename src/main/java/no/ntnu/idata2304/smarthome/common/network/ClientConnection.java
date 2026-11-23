package no.ntnu.idata2304.smarthome.common.network;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import no.ntnu.idata2304.smarthome.common.config.LocalConfiguration.ServerAddress;
import no.ntnu.idata2304.smarthome.common.protocol.Protocol;
import no.ntnu.idata2304.smarthome.common.security.TlsContexts;

/** A registered client socket with bounded connect and read times. */
public final class ClientConnection implements Closeable {
  private final Socket socket;
  private final BufferedReader reader;
  private final OutputStreamWriter writer;

  /** Creates a new ClientConnection with the given socket. */
  private ClientConnection(Socket socket) throws IOException {
    this.socket = socket;
    reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
  }

  /** Verifies the TLS peer and completes registration before returning the connection. */
  public static ClientConnection connect(
      ServerAddress address, SSLSocketFactory factory, String hello, int readTimeout)
      throws IOException {
    var tcp = new Socket();
    Socket socket = tcp;
    try {
      tcp.connect(new InetSocketAddress(address.host(), address.port()), 2_000);
      var tls = (SSLSocket) factory.createSocket(tcp, address.host(), address.port(), true);
      socket = tls;
      var parameters = tls.getSSLParameters();
      parameters.setProtocols(new String[] {TlsContexts.PROTOCOL});
      // JSSE's HTTPS identity algorithm verifies the DNS name or IP, even for non-HTTP protocols.
      parameters.setEndpointIdentificationAlgorithm("HTTPS");
      tls.setSSLParameters(parameters);
      tls.setSoTimeout(5_000);
      tls.startHandshake();

      tls.setSoTimeout(3_000);
      var connection = new ClientConnection(tls);
      connection.send(hello);
      String registration = connection.read();
      if (!"READY;1".equals(registration)) {
        throw new RegistrationRejected(registration);
      }
      tls.setSoTimeout(readTimeout);
      return connection;
    } catch (IOException | RuntimeException exception) {
      try {
        socket.close();
      } catch (IOException closeFailure) {
        exception.addSuppressed(closeFailure);
      }
      throw exception;
    }
  }

  /** Sends a message to the server. */
  public void send(String message) throws IOException {
    Protocol.writeLine(writer, message);
  }

  /** Reads a message from the server. */
  public String read() throws IOException {
    String message = Protocol.readLine(reader);
    if (message == null) {
      throw new EOFException("Server closed the connection");
    }
    return message;
  }

  /** Sends a message to the server and waits for a response. */
  public String request(String message) throws IOException {
    send(message);
    return read();
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  /** The server answered registration with a rejection. */
  public static final class RegistrationRejected extends IOException {
    private RegistrationRejected(String response) {
      super(response);
    }
  }
}
