package no.ntnu.idata2304.smarthome.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests login and role enforcement through panel messages  */
class ServerRoleTest {
  private final ServerSocket socket = new ServerSocket(); // Unbound, no listening port needed

  ServerRoleTest() throws Exception {}

  @AfterEach
  void closeSocket() throws Exception {
    socket.close();
  }

  private Properties account(String role) {
    var users = new Properties();
    users.setProperty("users", "tester");
    users.setProperty("tester.role", role);
    users.setProperty("tester.token-env", "TEST_TOKEN");
    // Different IDs detect accidental sharing of sensor and actuator permissions.
    users.setProperty("tester.sensors", "12:1, 13:2");
    users.setProperty("tester.actuators", "12:3");
    return users;
  }

  private String session(Properties users, Map<String, String> environment,
                         String login, String... requests) throws Exception {
    var server = new ServerApplication(socket, users, environment);
    var output = new ByteArrayOutputStream();
    var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
    var reader = new BufferedReader(new StringReader(String.join("\n", requests)));
    server.handlePanel(reader, writer, login);
    return output.toString(StandardCharsets.UTF_8);
  }

  private String request(Properties users, String request) throws Exception {
    return session(users, Map.of("TEST_TOKEN", "secret"),
        "HELLO_PANEL;1;tester;secret", request);
  }

  @ParameterizedTest
  @ValueSource(strings = {"SHA", "OWNER"})
  void fullAccessRolesAllowAllExistingDeviceCommands(String role) throws Exception {
    for (String command : new String[] {"GET_READING;99;9", "GET_ACTUATOR;99;9",
        "TURN_ON_HEATER;99;9", "TURN_OFF_HEATER;99;9"}) {
      // With no nodes connected, an authorized request reaches the node lookup.
      assertEquals("READY;1\nERROR;NODE_UNAVAILABLE\n", request(account(role), command), command);
    }
  }

  @Test
  void technicianCanReadButCannotTurnHeatersOnOrOff() throws Exception {
    var users = account("TECHNICIAN");
    for (String command : new String[] {"GET_READING;12;1", "GET_ACTUATOR;12;3"}) {
      assertEquals("READY;1\nERROR;NODE_UNAVAILABLE\n", request(users, command), command);
    }
    for (String command : new String[] {"TURN_ON_HEATER;12;3", "TURN_OFF_HEATER;12;3"}) {
      assertEquals("READY;1\nERROR;FORBIDDEN\n", request(users, command), command);
    }
  }

  @Test
  void guestCanAccessOnlyGrantedNodeDevicePairs() throws Exception {
    var users = account("GUEST");
    for (String command : new String[] {"GET_READING;12;1", "GET_READING;13;2",
        "GET_ACTUATOR;12;3", "TURN_ON_HEATER;12;3", "TURN_OFF_HEATER;12;3"}) {
      assertEquals("READY;1\nERROR;NODE_UNAVAILABLE\n", request(users, command), command);
    }
    for (String command : new String[] {"GET_READING;13;1", "GET_READING;12;3",
        "GET_ACTUATOR;12;1", "TURN_ON_HEATER;12;1", "TURN_OFF_HEATER;13;3"}) {
      assertEquals("READY;1\nERROR;FORBIDDEN\n", request(users, command), command);
    }
  }

  @Test
  void guestWithoutGrantsHasNoReadingOrControlAccess() throws Exception {
    var users = account("GUEST");
    users.remove("tester.sensors");
    users.remove("tester.actuators");
    for (String command : new String[] {"GET_READING;12;1", "GET_ACTUATOR;12;3",
        "TURN_ON_HEATER;12;3", "TURN_OFF_HEATER;12;3"}) {
      assertEquals("READY;1\nERROR;FORBIDDEN\n", request(users, command), command);
    }
  }

  @Test
  void invalidCredentialsAndUnknownRolesAreRejected() throws Exception {
    var users = account("OWNER");
    var environment = Map.of("TEST_TOKEN", "secret");
    assertEquals("ERROR;AUTH_FAILED\n", session(users, environment, "HELLO_PANEL;1;tester;wrong"));
    assertEquals("ERROR;AUTH_FAILED\n", session(users, environment, "HELLO_PANEL;1;unknown;secret"));
    assertEquals("ERROR;AUTH_FAILED\n", session(users, Map.of(), "HELLO_PANEL;1;tester;secret"));
    assertEquals("ERROR;AUTH_FAILED\n", request(account("ADMIN"), "GET_READING;12;1"));
  }

  @Test
  void deniedRequestDoesNotEndSessionOrChangeRole() throws Exception {
    assertEquals("READY;1\nERROR;FORBIDDEN\nERROR;UNKNOWN_COMMAND\nERROR;FORBIDDEN\n",
        session(account("TECHNICIAN"), Map.of("TEST_TOKEN", "secret"),
            "HELLO_PANEL;1;tester;secret", "TURN_ON_HEATER;12;3",
            "HELLO_PANEL;1;owner;secret", "TURN_OFF_HEATER;12;3"));
  }
}
