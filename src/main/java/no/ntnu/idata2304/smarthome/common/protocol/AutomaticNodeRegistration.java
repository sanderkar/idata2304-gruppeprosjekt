package no.ntnu.idata2304.smarthome.common.protocol;

/** Registration for a provisioned sensor node that asks the server for a numeric address. */
public record AutomaticNodeRegistration(String deviceName, String token) {
  public static final String HELLO = "HELLO_AUTO_NODE";
  private static final String READY_PREFIX = "READY_NODE;1;";

  public static boolean isDeviceName(String name) {
    return name.matches("[A-Z][A-Z0-9_]{0,31}");
  }

  public static String tokenSetting(String name) {
    return "SMART_HOME_AUTO_NODE_" + name + "_TOKEN";
  }

  public static AutomaticNodeRegistration decode(String message) {
    String[] fields = message.split(";", -1);
    if (fields.length != 4 || !fields[0].equals(HELLO) || !fields[1].equals("1")
        || !isDeviceName(fields[2]) || !fields[3].matches("[A-Za-z0-9._-]{1,128}")) {
      throw new IllegalArgumentException("Invalid automatic node registration");
    }
    return new AutomaticNodeRegistration(fields[2], fields[3]);
  }

  public String encode() {
    return HELLO + ";1;" + deviceName + ";" + token;
  }

  public static String ready(int nodeId) {
    return READY_PREFIX + nodeId;
  }

  public static boolean isReady(String response) {
    try {
      readAddress(response);
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  public static int readAddress(String response) {
    if (!response.startsWith(READY_PREFIX)) {
      throw new IllegalArgumentException("Expected an assigned node address");
    }
    String id = response.substring(READY_PREFIX.length());
    if (!id.matches("[0-9]{1,10}")) {
      throw new IllegalArgumentException("Invalid assigned node address");
    }
    return Integer.parseInt(id);
  }
}
