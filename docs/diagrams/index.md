# SHA Smart Home diagrams

| Diagram | What it shows |
| --- | --- |
| [System architecture](01-architecture.puml) | The control panel, server, sensor node, and address registry |
| [Sensor node classes](02-sensor-node-classes.puml) | How the startup class uses both factories and attaches the devices to the node |
| [Reading and controlling](03-read-and-control.puml) | A sensor reading, fan control, and a denied command |
| [Automatic node registration](04-automatic-node-registration.puml) | Token validation, address allocation, and reuse of a saved address |
| [Connection recovery](05-panel-connection-recovery.puml) | How the control panel detects a connection loss and reconnects |

Open the `.puml` files in a PlantUML preview. Message formats are described in [protocol.md](../protocol.md).
