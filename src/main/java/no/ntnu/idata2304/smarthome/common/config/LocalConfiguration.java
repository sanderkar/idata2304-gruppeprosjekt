package no.ntnu.idata2304.smarthome.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** Loads shared local credentials, with environment variables taking precedence
 */
public final class LocalConfiguration {
  private LocalConfiguration() {}
  public record ServerAddress(String host, int port) {}

  public static ServerAddress loadServerAddress() throws IOException {
    return loadServerAddress(loadCredentials());
  }

  static ServerAddress loadServerAddress(Map<String, String> configuration) {
    String host = configuration.getOrDefault("SMART_HOME_HOST", "127.0.0.1");
    String portValue = configuration.getOrDefault("SMART_HOME_PORT", "5000");

    if (host.isBlank()) {
      throw new IllegalArgumentException("Server host cannot be blank");
    }

    final int port;
    try {
      port = Integer.parseInt(portValue);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Server port must be a number", exception);
    }

    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException(
          "Server port must be between 1 and 65535");
    }

    return new ServerAddress(host, port);
  }

  public static Map<String, String> loadCredentials() throws IOException {
    return loadCredentials(Path.of("config/local.properties"), System.getenv());
  }

  static Map<String, String> loadCredentials(Path path, Map<String, String> environment)
      throws IOException {
    var credentials = new HashMap<String, String>();
    if (Files.exists(path)) {
      var properties = new Properties();
      try (var reader = Files.newBufferedReader(path)) {
        properties.load(reader);
      }
      for (String name : properties.stringPropertyNames()) {
        credentials.put(name, properties.getProperty(name));
      }
    }
    credentials.putAll(environment);
    return Map.copyOf(credentials);
  }

  public static Properties loadPanelUsers() throws IOException {
    var users = new Properties();
    try (var reader = Files.newBufferedReader(Path.of("config/panel-users.properties"))) {
      users.load(reader);
    }
    return users;
  }
}
