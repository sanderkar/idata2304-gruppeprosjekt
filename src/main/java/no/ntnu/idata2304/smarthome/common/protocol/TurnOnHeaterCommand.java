package no.ntnu.idata2304.smarthome.common.protocol;

/**
 * A command to turn on a heater.
 */
public class TurnOnHeaterCommand {
  private final int nodeId;
  private final int heaterId;

  /**
   * Constructs a TurnOnHeaterCommand with the specified node ID and heater ID.
   *
   * @param nodeId the unique identifier for the sensor node
   * @param heaterId the unique identifier for the heater
   */

  public TurnOnHeaterCommand(int nodeId, int heaterId) {
    this.heaterId = heaterId;
    this.nodeId = nodeId;
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
   * Returns the unique identifier for the heater.
   *
   * @return the heater ID
   */

  public int getHeaterId() {
    return heaterId;
  }
}
