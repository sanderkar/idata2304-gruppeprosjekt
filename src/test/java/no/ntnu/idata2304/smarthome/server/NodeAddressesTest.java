package no.ntnu.idata2304.smarthome.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NodeAddressesTest {
  @TempDir Path directory;

  @Test
  void reservesStaticAddressesAndPersistsConcurrentAllocations() throws Exception {
    var configuration = Map.of(
        "SMART_HOME_NODE_ADDRESS_FILE", directory.resolve("addresses.properties").toString(),
        "SMART_HOME_NODE_100000_TOKEN", "reserved");
    var addresses = NodeAddresses.load(configuration);
    int kitchen;
    int garage;
    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var requests = List.<Callable<Integer>>of(
          () -> addresses.addressFor("KITCHEN"),
          () -> addresses.addressFor("GARAGE"),
          () -> addresses.addressFor("KITCHEN"));
      var results = workers.invokeAll(requests, 5, TimeUnit.SECONDS);
      kitchen = results.get(0).get();
      garage = results.get(1).get();
      assertEquals(kitchen, results.get(2).get());
    }
    assertNotEquals(100_000, kitchen);
    assertNotEquals(100_000, garage);
    assertNotEquals(kitchen, garage);

    var restarted = NodeAddresses.load(configuration);
    assertTrue(restarted.isAssigned(kitchen));
    assertEquals(garage, restarted.addressFor("GARAGE"));
    assertEquals(kitchen, restarted.addressFor("KITCHEN"));
    assertNotEquals(kitchen, restarted.addressFor("OFFICE"));
  }

  @Test
  void rejectsDuplicateAddressesInTheSavedRegistry() throws IOException {
    Path file = directory.resolve("addresses.properties");
    Files.writeString(file, "KITCHEN=100000\nGARAGE=100000\n");
    assertThrows(IOException.class, () ->
        NodeAddresses.load(Map.of("SMART_HOME_NODE_ADDRESS_FILE", file.toString())));
  }
}
