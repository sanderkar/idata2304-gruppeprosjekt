package no.ntnu.idata2304.smarthome.common.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/** Utility class for handling protocol-related operations. */
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

  /**
   * Writes a message to the provided Writer, followed by a newline character, and flushes the
   * stream.
   *
   * @param writer the Writer to write the message to
   * @param message the message to be written
   * @throws IOException if an I/O error occurs
   */
  public static void writeLine(Writer writer, String message) throws IOException {
    writer.write(message);
    writer.write('\n');
    writer.flush();
  }
}
