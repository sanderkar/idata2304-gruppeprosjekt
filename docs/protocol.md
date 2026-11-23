# SHA Protocol

## 1. Introduction

This document specifies the protocol used by SHA sensor nodes, control panels,
and the broker. A control panel can discover connected nodes, request sensor
readings and actuator status, and control a specific actuator.

The simulation supports registration, access control, readings, and control of
simulated devices. The server can assign node addresses automatically. TLS 1.3
protects traffic in transit, and clients reconnect after temporary network
outages.

## 2. Terminology

| Term | Meaning |
| --- | --- |
| Sensor node | An application that simulates sensors and actuators and executes commands addressed to that node. |
| Control panel | A client that displays readings and status and sends commands on behalf of a user. |
| Broker | The TCP server that registers connections, checks permissions, and forwards requests and replies. |
| Sensor | A component that produces a measurement, such as temperature or relative humidity. |
| Actuator | A component with a controllable state, such as a heater or fan. |
| Session | The lifetime of an established client TCP connection. |
| Warning | A notice displayed by the control panel when a reading falls outside a defined threshold. |

## 3. Transport

The protocol uses TLS 1.3 over TCP. TCP provides an ordered byte stream so the
applications can exchange requests and replies in sequence. TLS protects that
stream in transit. Newlines delimit application messages within the TLS
connection. If the connection drops during a command, its outcome may be
unknown.

## 4. Port and host

The local demo uses `127.0.0.1:5000` by default. The server uses
`SMART_HOME_HOST` as its bind address, and clients use it as the address to
connect to. `SMART_HOME_PORT` sets the TCP port, from 1 to 65535. These values
can be set in `config/local.properties` or as environment variables.
Environment variables take precedence. A fixed default lets the demo clients
connect with the standard configuration, and the port can be changed if it
is already in use.

## 5. Architecture

Sensor nodes and control panels both open outbound TCP connections to the
broker. The broker tracks active connections and the node ID associated with
each sensor node connection. Multiple sensor nodes and control panels can be
registered at the same time.

Both client types need the server address, its certificate, and their own
credentials. The broker centralizes routing and access control so each panel
can use a single server connection. The server routes requests by node ID.
The node then finds the component by sensor or actuator ID. Different nodes
can therefore use the same actuator ID for their own devices.

## 6. Information flow

1. A sensor node connects and sends `HELLO_NODE` to register a static address
   or `HELLO_AUTO_NODE` to request one from the server. The server checks the
   credentials and registers the node.
2. A control panel connects and sends `HELLO_PANEL`. The server checks the
   credentials and looks up the user's role in its configuration.
3. The panel sends `LIST_NODES` to discover active nodes and `LIST_DEVICES`
   to discover the components on a node.
4. The panel requests a reading with `GET_READING` or actuator status with
   `GET_ACTUATOR`. It sends `SET_ACTUATOR` to change the state of one
   actuator. This pull model avoids sending measurements when no panel
   requests them.
5. The server checks device permissions before forwarding a request to read
   a measurement, check an actuator's state, or control an actuator. The
   node handles the request and replies. The server returns the result to
   the requesting panel. All authenticated panels can discover nodes and
   components.

The panel displays a warning for temperatures below 5 °C or above 35 °C and
for humidity above 80% RH. The thresholds are defined in the panel code.
The panel evaluates the reading when it displays it, so the thresholds can
change independently of the wire format.

## 7. Connections and state

The protocol is connection-oriented and stateful. After a successful TLS
handshake, the first application message must be a `HELLO` message. A reply
of `READY;1` or `READY_NODE;1;<nodeId>` establishes a registered session that
the client can reuse. Each session is bound to the identity used at
registration.

A control panel waits for a reply before sending its next request. The
server allows at most one outstanding request per sensor node, including
heartbeat requests. If another request arrives while the node is busy, the
server replies `ERROR;BUSY`. This rule lets the server match replies to
requests without separate request IDs. The server removes disconnected
nodes from the active connection registry.

## 8. Types and constants

| Field | Format and meaning |
| --- | --- |
| `version` | Registration messages use the literal value `1`. |
| `nodeId`, `sensorId`, `actuatorId`, `heaterId` | Integers from 0 to 2147483647, written with ASCII digits. Node IDs are unique among registered nodes. Sensor IDs and actuator IDs are each unique within their respective category on a node. A `heaterId` is the actuator ID of a heater. |
| `deviceName` | A name used for automatic address assignment. It contains 1–32 characters, starts with an uppercase ASCII letter, and may then contain uppercase ASCII letters, digits, and underscores. |
| `userId` | A configured panel account name. The server compares it with its account list. |
| `token` | The clients use 1–128 characters from `A-Z`, `a-z`, `0-9`, periods, hyphens, and underscores. The server enforces this format for `HELLO_AUTO_NODE`, checks the length for `HELLO_NODE`, and requires a nonempty token for `HELLO_PANEL`. It compares the token with the configured value in all cases. |
| `sensorType` | `TEMPERATURE` for degrees Celsius and `HUMIDITY` for relative humidity from 0 to 100 percent. |
| `actuatorType` | `HEATER` for a heater and `FAN` for a fan. |
| `value` | A finite decimal number using ASCII digits, an optional minus sign, and a period as the decimal separator. Sensor nodes send two decimal places, as in `21.50`. |
| `state` | `ON` or `OFF`. |

The nodes simulate temperature and humidity sensors, heaters, and fans.
Additional device types can be defined by agreeing on a type code and the
corresponding unit or state.

## 9. Message format

Each message is one UTF-8 line. The sender terminates it with LF (`\n`), and
the receiver also accepts CRLF (`\r\n`). The line reader has no fixed size
limit. Message names are uppercase. Fields appear in fixed positions and are
separated by semicolons. Senders use the format in the table without extra
spaces or fields. A placeholder such as `<nodeId>` represents the field value
without the angle brackets. Newlines mark message boundaries, and fixed field
positions make the lines straightforward to parse.

| Message | Sender → recipient | Success response |
| --- | --- | --- |
| `HELLO_NODE;<version>;<nodeId>;<token>` | Node → server | `READY;1` |
| `HELLO_NODE;<version>;<nodeId>;<token>;HEARTBEAT` | Node → server | `READY;1` |
| `HELLO_AUTO_NODE;<version>;<deviceName>;<token>` | Node → server | `READY_NODE;1;<nodeId>` |
| `HELLO_PANEL;<version>;<userId>;<token>` | Control panel → server | `READY;1` |
| `PING` | Control panel → server, or server → automatically addressed node or static node that opted into `HEARTBEAT` | `PONG` to the sender |
| `LIST_NODES` | Control panel → server | `NODES` followed by one semicolon-separated field for each active node ID, sorted numerically |
| `LIST_DEVICES;<nodeId>` | Control panel → server → node | `DEVICES;<nodeId>` followed by one field per component |
| `GET_READING;<nodeId>;<sensorId>` | Control panel → server → node | `READING;<nodeId>;<sensorId>;<sensorType>;<value>` |
| `GET_ACTUATOR;<nodeId>;<actuatorId>` | Control panel → server → node | `ACTUATOR_STATE;<nodeId>;<actuatorId>;<actuatorType>;<state>` |
| `SET_ACTUATOR;<nodeId>;<actuatorId>;<state>` | Control panel → server → node | `ACTUATOR_STATE;<nodeId>;<actuatorId>;<actuatorType>;<state>` with the resulting device state |
| `TURN_ON_HEATER;<nodeId>;<heaterId>` | Control panel → server → node | Same response format as `SET_ACTUATOR` with state `ON` and type `HEATER` |
| `TURN_OFF_HEATER;<nodeId>;<heaterId>` | Control panel → server → node | Same response format as `SET_ACTUATOR` with state `OFF` and type `HEATER` |

Automatically addressed nodes and static nodes that include `HEARTBEAT`
respond to the server's heartbeat requests. In the static registration form
without the optional field, `HEARTBEAT` is reserved because the server
interprets that value as a marker when it appears at the end of a registration
message.

The two heater commands support earlier control panels. The sensor node
application uses them for heater `1` and returns `ERROR;UNKNOWN_ACTUATOR`
for other actuator IDs. Use `SET_ACTUATOR` for general actuator control. For
forwarded requests, the node replies to the server, which relays the reply to
the panel.

Example list replies are `NODES;7;12` and
`DEVICES;12;SENSOR:1:TEMPERATURE;ACTUATOR:3:HEATER;ACTUATOR:4:FAN`.
Each component field has the form `category:id:type`. An empty result contains
just `NODES` or `DEVICES;<nodeId>`, with no trailing semicolon.

## 10. Error handling

Errors sent over the connection take the form `ERROR;<code>`. The panel
displays the code and gives a specific explanation for common errors.
`ERROR;SERVER_UNAVAILABLE` is a local code the panel uses when it cannot
reach the server.

| Code | When it is used |
| --- | --- |
| `BAD_FORMAT` | The recipient rejects the message format. The server also uses this code when a recognized registration message carries a version other than `1`. A sensor node uses it for unknown or invalid requests. |
| `UNKNOWN_COMMAND` | The server does not recognize a request from a registered control panel. |
| `BAD_STATE` | The first message on the connection is not a registration message. |
| `AUTH_FAILED` | The credentials do not match, or the panel account is unknown or disabled. |
| `FORBIDDEN` | The user's role or device permissions do not allow the operation. |
| `DUPLICATE_NODE` | An active node is already registered with that ID. |
| `ADDRESS_IN_USE` | A static node requests an address already assigned to an automatically addressed node. |
| `ADDRESS_UNAVAILABLE` | The server could not allocate and save a new address. |
| `NODE_UNAVAILABLE` | The specified node has no active connection, or its connection drops while a reply is pending. |
| `WRONG_NODE` | A sensor node receives a request addressed to a different node. |
| `UNKNOWN_SENSOR` | The node has no sensor with the requested ID. |
| `UNKNOWN_ACTUATOR` | The actuator ID is missing from the node or does not match the requested type. |
| `BUSY` | The node already has an outstanding request. |
| `TIMEOUT` | The node did not reply in time. |
| `BAD_RESPONSE` | The node's reply failed the server's checks for the pending request. |

The server closes the connection after rejecting registration. A registered
panel can continue sending requests after a command is rejected. If a
connection drops mid-message, the receiver discards the incomplete line.

The server applies different reply checks according to the pending request.
For a sensor reading, it checks the node ID, sensor ID, sensor type, and
value. For actuator status, it checks the node ID, actuator ID, device type
field, and state. For `SET_ACTUATOR`, it checks the reply prefix containing
the node and actuator IDs and checks that the reply ends with the requested
state. The server passes through `NODE_UNAVAILABLE`, `UNKNOWN_SENSOR`,
`UNKNOWN_ACTUATOR`, and `WRONG_NODE` errors for pending requests. Other
unexpected replies, such as `ERROR;BAD_FORMAT` from the node, result in
`ERROR;BAD_RESPONSE` for the panel. The server closes the node connection
after `TIMEOUT` or `BAD_RESPONSE` so a late reply cannot be mistaken for the
next reply. It also closes the node connection if the node sends a message
when no request is pending.

## 11. Example scenario

The local demo connects nodes 12 and 13. Node 13 has an additional humidity
sensor and fan, each with ID 3 in its respective category. The nodes and the
owner have credentials configured on the server. The tokens below are example
values.

| From → to | Message |
| --- | --- |
| Node 12 → server | `HELLO_NODE;1;12;node-12-example;HEARTBEAT` |
| Server → node 12 | `READY;1` |
| Node 13 → server | `HELLO_NODE;1;13;node-13-example;HEARTBEAT` |
| Server → node 13 | `READY;1` |
| Control panel → server | `HELLO_PANEL;1;owner-example;panel-example` |
| Server → control panel | `READY;1` |
| Control panel → server | `LIST_NODES` |
| Server → control panel | `NODES;12;13` |
| Control panel → server → node 12 | `LIST_DEVICES;12` |
| Node 12 → server → control panel | `DEVICES;12;SENSOR:1:TEMPERATURE;SENSOR:2:HUMIDITY;ACTUATOR:1:HEATER;ACTUATOR:2:FAN` |
| Control panel → server → node 13 | `LIST_DEVICES;13` |
| Node 13 → server → control panel | `DEVICES;13;SENSOR:1:TEMPERATURE;SENSOR:2:HUMIDITY;SENSOR:3:HUMIDITY;ACTUATOR:1:HEATER;ACTUATOR:2:FAN;ACTUATOR:3:FAN` |
| Control panel → server → node 13 | `GET_READING;13;3` |
| Node 13 → server → control panel | `READING;13;3;HUMIDITY;64.92` |
| Control panel → server → node 13 | `SET_ACTUATOR;13;3;ON` |
| Node 13 → server → control panel | `ACTUATOR_STATE;13;3;FAN;ON` |

The user enters `list` to find the nodes, `read 13 3` to read the humidity,
and `on 13 3` to turn on the fan. The panel shows the fan as on after
receiving the node's reply.

### Automatic address assignment

1. A node starts with `--auto KITCHEN` and sends
   `HELLO_AUTO_NODE;1;KITCHEN;kitchen-example` over TLS.
2. The server checks the token configured for `KITCHEN`. On first
   registration, it selects an available address from 100000 to 999999999
   and saves the assignment before replying. It skips all configured static
   addresses, including those for disconnected nodes.
3. The server replies, for example, with `READY_NODE;1;100000`. The node
   uses this address in subsequent protocol messages, and the panel can
   discover it with `LIST_NODES`. Automatically addressed nodes respond to
   heartbeat requests.
4. When the node reconnects, including after a server restart, the server
   uses the device name to find the same address. An additional connection
   for a node that is already active receives `ERROR;DUPLICATE_NODE`. The
   server retains assignments after disconnects so address-based permissions
   and panel commands continue to target the same nodes.

The server stores the name-to-address mapping in
`config/state/node-addresses.properties`. The server setting
`SMART_HOME_NODE_ADDRESS_FILE` can override this path. The server processes
assignments one at a time and atomically replaces the registry file. This
ensures that simultaneous registrations get unique addresses and that a
client can recover its assignment if the connection drops after the server
saves it and before the client receives the reply.

## 12. Reliability

- **Detecting outages.** The control panel sends `PING` in the background.
  It waits two seconds between checks and has a ten-second read timeout.
  The server attempts to send `PING` after three seconds to automatically
  addressed nodes and static nodes that opt into `HEARTBEAT`. It skips the
  check if the node already has an outstanding request. The server waits
  up to five seconds for a node reply, and the sensor node has a
  fifteen-second read timeout. Heartbeats detect outages during idle periods
  even when TCP does not report that the connection has closed.
- **Reconnecting.** Both clients open a new TCP connection, complete the TLS
  handshake, and send a new `HELLO`. The panel retries at the next background
  check. The node waits one, then two, then four seconds between failed
  attempts. A node also retries after `DUPLICATE_NODE`, giving the server
  time to remove the previous connection. Other registration rejections stop
  automatic retries. Simulated readings and actuator state remain in the
  node process across reconnects.
- **Retrying read-only requests.** The panel keeps one read-only request
  outstanding at a time. It may retry after a lost connection, an unavailable
  or busy node, or a timeout. After a temporary failure, the panel checks
  whether fewer than fifteen seconds have elapsed since the first attempt.
  If so, it waits half a second and tries again. Each attempt that starts
  can run until its own reply deadline. Requesting a sensor reading again
  may return a different value.
- **Displaying cached readings.** The panel keeps the last confirmed
  readings for up to 100 sensors in memory. If a sensor remains unavailable
  after retries, the panel can display a cached reading with the time it
  was received. Readings are generated on request, so the panel has no new
  measurements to retrieve for the disconnected period.
- **Reporting uncertain control outcomes.** The panel sends each control
  command once. If a reply is lost, the node may already have changed the
  actuator's state. The panel asks the user to check the current state after
  reconnecting, since retrying automatically could overwrite a newer command
  from another panel.

## 13. Security

The server reads static node tokens from `SMART_HOME_NODE_<nodeId>_TOKEN`
and automatically addressed node tokens from
`SMART_HOME_AUTO_NODE_<deviceName>_TOKEN`.
`../config/panel-users.properties` maps each panel account to a role and a
token setting. An account without a configured token is disabled. The
server checks device permissions before forwarding requests. A node
connection is reserved for replies to server requests.

| Role | Allowed access |
| --- | --- |
| SHA (`SHA`) | Discover nodes and devices, read measurements and actuator status, and control actuators. |
| Owner (`OWNER`) | Same permissions as SHA. |
| Technician (`TECHNICIAN`) | Discover nodes and devices and read measurements and actuator status for troubleshooting. |
| Guest (`GUEST`) | Discover nodes and devices. Read measurements from sensors granted to the account. Read status and control actuators granted to the account. |

Guest sensor and actuator grants are configured separately as comma-separated
lists of `nodeId:deviceId` pairs in `<userId>.sensors` and
`<userId>.actuators`. All authenticated panels can list nodes and devices.
The server checks access to readings, actuator status, and control
operations per device.

TLS 1.3 encrypts traffic, protects message integrity, and authenticates the
broker. Both sensor nodes and control panels have a local copy of the
server's public X.509 certificate. Each client checks that the certificate
is trusted and matches the configured server hostname or IP address. A
failed TLS handshake prevents the application session from starting. The
client completes the handshake before sending its token in `HELLO`.

The server stores its private key in a password-protected PKCS#12 keystore.
Clients authenticate with tokens sent inside TLS. TLS protects both
connections, and the broker reads the messages to enforce permissions and
route requests. Java's TLS implementation provides standardized transport
encryption while preserving the application message format. See the
[running guide](running.md) for setup. Keep tokens, passwords, and
private keys out of Git and application logs.

## 14. Assumptions

- Sensors and actuators are simulated. A node's device configuration is
  local, and its address comes from a command-line argument or the server.
- The broker is the single point of contact. One broker is sufficient for
  the demo and provides one place to register nodes and enforce permissions.
- Line-based messages with fixed fields make the protocol easy to read and
  debug.
- Messages and active connections are kept in memory. The server persists
  automatic address assignments so clients can retain their addresses when
  they register after a restart.
- Panels request current readings. Historical readings, subscriptions, and
  automatic control are outside the scope of version 1.

