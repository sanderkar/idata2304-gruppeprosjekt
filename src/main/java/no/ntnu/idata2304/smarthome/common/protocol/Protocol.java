package no.ntnu.idata2304.smarthome.common.protocol;

import java.io.IOException;
import java.io.Writer;

/** Utility class for handling protocol-related operations. */
public final class Protocol {
  private Protocol() {}

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
