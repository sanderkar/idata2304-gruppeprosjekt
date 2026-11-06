package no.ntnu.idata2304.smarthome.common.protocol;

import java.io.IOException;
import java.io.Writer;

public final class Protocol {
  private Protocol() {}

  public static void writeLine(Writer writer, String message) throws IOException {
    writer.write(message);
    writer.write('\n');
    writer.flush();
  }
}
