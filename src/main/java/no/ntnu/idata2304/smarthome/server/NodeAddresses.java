package no.ntnu.idata2304.smarthome.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import no.ntnu.idata2304.smarthome.common.protocol.AutomaticNodeRegistration;

/** Stores server-assigned addresses independently of live node connections. */
final class NodeAddresses {
  private static final int FIRST_ADDRESS = 100_000;
  private static final int LAST_ADDRESS = 999_999_999;
  private static final Pattern STATIC_TOKEN = Pattern.compile(
      "SMART_HOME_NODE_(0|[1-9][0-9]{0,9})_TOKEN");

  private final Path file;
  private final Map<String, Integer> assignments = new HashMap<>();
  private final Set<Integer> unavailable = new HashSet<>();
  private int next = FIRST_ADDRESS;

  static NodeAddresses load(Map<String, String> configuration) throws IOException {
    Path file = Path.of(configuration.getOrDefault(
        "SMART_HOME_NODE_ADDRESS_FILE", "config/state/node-addresses.properties"));
    return new NodeAddresses(file.toAbsolutePath(), configuration.keySet());
  }

  private NodeAddresses(Path file, Set<String> settings) throws IOException {
    this.file = file;
    var saved = new Properties();
    try (var reader = Files.newBufferedReader(file)) {
      saved.load(reader);
      for (String name : saved.stringPropertyNames()) {
        int address = Integer.parseInt(saved.getProperty(name));
        if (!AutomaticNodeRegistration.isDeviceName(name)
            || address < FIRST_ADDRESS || address > LAST_ADDRESS || !unavailable.add(address)) {
          throw new IllegalArgumentException("Invalid or duplicate assignment");
        }
        assignments.put(name, address);
      }
    } catch (NoSuchFileException exception) {
      // The first automatic registration creates the address file.
    } catch (IllegalArgumentException | IOException exception) {
      throw new IOException("Cannot read node address assignments from " + file, exception);
    }

    for (String setting : settings) {
      var match = STATIC_TOKEN.matcher(setting);
      if (match.matches()) {
        long address = Long.parseLong(match.group(1));
        if (address >= FIRST_ADDRESS && address <= LAST_ADDRESS) {
          unavailable.add((int) address);
        }
      }
    }
  }

  synchronized boolean isAssigned(int address) {
    return assignments.containsValue(address);
  }

  /** Returns the same address for a device name and saves a new allocation before advertising it. */
  synchronized int addressFor(String name) throws IOException {
    Integer assigned = assignments.get(name);
    if (assigned != null) {
      return assigned;
    }
    while (next <= LAST_ADDRESS && unavailable.contains(next)) {
      next++;
    }
    if (next > LAST_ADDRESS) {
      throw new IOException("No automatic node addresses available");
    }
    saveAssignment(name, next);
    assignments.put(name, next);
    unavailable.add(next);
    return next++;
  }

  private void saveAssignment(String name, int address) throws IOException {
    var saved = new Properties();
    assignments.forEach((device, id) -> saved.setProperty(device, Integer.toString(id)));
    saved.setProperty(name, Integer.toString(address));
    Path directory = file.getParent();
    Files.createDirectories(directory);
    Path temporary = Files.createTempFile(directory, "node-addresses-", ".tmp");
    try {
      try (var writer = Files.newBufferedWriter(temporary)) {
        saved.store(writer, "Server-assigned sensor node addresses");
      }
      Files.move(temporary, file,
          StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
