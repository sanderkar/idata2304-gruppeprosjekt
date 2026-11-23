package no.ntnu.idata2304.smarthome.common.protocol;

/** A command to turn off a heater. */
public class TurnOffHeaterCommand {
  private final int nodeId;
  private final int heaterId;

  /**
   * Constructs a TurnOffHeaterCommand with the specified node ID and heater ID.
   *
   * @param nodeId the unique identifier for the sensor node
   * @param heaterId the unique identifier for the heater
   */
  public TurnOffHeaterCommand(int nodeId, int heaterId) {
    this.nodeId = nodeId;
    this.heaterId = heaterId;
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
