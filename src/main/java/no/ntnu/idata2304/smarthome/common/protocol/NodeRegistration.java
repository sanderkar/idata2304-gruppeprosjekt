package no.ntnu.idata2304.smarthome.common.protocol;

/**
 * Represents a registration message for a sensor node.
 */

public class NodeRegistration {

  private final int nodeId;
  private final String token;

  /**
   * Constructs a NodeRegistration with the specified node ID.
   *
   * @param nodeId the unique identifier for the sensor node
   * @param token the token for the sensor node
   */
  public NodeRegistration(int nodeId, String token) {
    this.nodeId = nodeId;
    this.token = token;
  }

  /**
   * Returns the unique identifier for the sensor node.
   *
   * @return the node ID
   */
  public int getNodeId() {
    return nodeId;
  }

  /**
   * Returns the token for the sensor node.
   *
   * @return the token
   */
  public String getToken() {
    return token;
  }

}