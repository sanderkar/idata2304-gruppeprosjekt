package no.ntnu.idata2304.smarthome.common.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public final class Protocol {
  public static final String HEARTBEAT_SUFFIX = ";HEARTBEAT";
  public static final String PING = "PING";
  public static final String PONG = "PONG";

  private Protocol() {}

  /** Reads a complete LF-terminated frame, accepting CRLF as well. */
  public static String readLine(Reader reader) throws IOException {
    var line = new StringBuilder();
    int character;
    while ((character = reader.read()) != -1) {
      if (character == '\n') {
        if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
          line.setLength(line.length() - 1);
        }
        return line.toString();
      }
      line.append((char) character);
    }
    if (!line.isEmpty()) {
      throw new EOFException("Connection closed before the end of a message");
    }
    return null;
  }

  public static void writeLine(Writer writer, String message) throws IOException {
    writer.write(message);
    writer.write('\n');
    writer.flush();
  }
}
