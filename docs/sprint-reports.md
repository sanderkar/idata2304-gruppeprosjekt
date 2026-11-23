# Sprint Reports – Smart Home Project

|            |                                                                         |
| ---------- | ----------------------------------------------------------------------- |
| Course     | IDATA2304 Computer Communication and Network Programming                |
| Group      | 4                                                                       |
| Repository | [SHA Smart Home](https://github.com/sanderkar/idata2304-gruppeprosjekt) |

## Group members and GitHub usernames

| Group member | GitHub username |
| --- | --- |
| Haidas Alekna | `Haidule` |
| Sander Skau Karlsen | `sanderkar` |
| Zhenya Ivanova Zhelyazkova | `zhenyazhel80` |

## Working method

The group developed a smart-home simulation with a TCP server, sensor nodes,
and a control panel. Changes were integrated through pull requests, direct
commits, and branch merges. Automated tests covered message handling,
access control, sensors and actuators, configuration, anomaly detection,
TLS, and connection recovery.

The four sprints below are reconstructed from commits and pull requests.
The date ranges indicate when the documented work happened. Some work
overlapped, including the configuration and fan work on 21 November.
The goals are retrospective summaries, and the work tables list completed
results. Contributor names reflect the commits and pull requests available
for this report. Original task assignments, pair work, and informal reviews
should be confirmed by the group where relevant.

## Sprint 1 – Project foundation and communication protocol

**Reconstructed work period** · 2–6 November 2026

### Sprint goal

Establish the Java project and get the sensor node, server, and control
panel communicating over TCP.

### Completed work

| Work item                                                                      | Recorded contributor | Completed result and evidence                                                                                                                                                                                                                                                                                                                     |
| ------------------------------------------------------------------------------ | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Establish the initial Maven project and application entry points               | `Haidule`            | Created the Maven project, source and test structure, and entry points for the three applications. [Initial project commit](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/9d7508e)                                                                                                                                                 |
| Build the first simulated devices and their tests                              | `Haidule`            | Added the temperature sensor, sensor node, and heater with tests. [Temperature sensor](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/834fd5e) · [Sensor node](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/21c824b) · [Heater](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/94502b2)               |
| Scaffold server sockets and the shared protocol class                          | `sanderkar`          | Introduced the server-socket constructor and the initial `Protocol` class. [Server scaffold](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/93ff058)                                                                                                                                                                                |
| Implement initial message encoding, decoding, and end-to-end TCP communication | `Haidule`            | Added the first heater command codec and tests, then implemented line-based communication, node registration, and request forwarding across the applications. [Codec and tests](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/a41b2e4) · [TCP communication](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/4ebd71f) |

### Retrospective

The initial project structure and TCP path made it possible to develop and
test the components independently. The applications depended on the same
message formats. Agreeing on those formats and assigning who would test both
ends of each exchange earlier would have simplified integration.

## Sprint 2 – Core smart-home functionality

**Reconstructed work period** · 10–20 November 2026

### Sprint goal

Extend the initial TCP path with discovery, readings, actuator control, and
server-enforced access levels.

### Completed work

| Work item | Recorded contributor | Completed result and evidence |
| --- | --- | --- |
| Prototype anomaly detection | `sanderkar` | Added the initial anomaly detector. Panel integration and boundary testing followed in Sprint 3. [Initial detector](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/b5e77b1) |
| Expand decoder validation | `zhenyazhel80` | Added tests for valid requests and malformed reading, discovery, and registration requests. Later added validation and tests for negative heater command IDs. [PR #1](https://github.com/sanderkar/idata2304-gruppeprosjekt/pull/1) · [PR #3](https://github.com/sanderkar/idata2304-gruppeprosjekt/pull/3) |
| Connect the heater to the running sensor node | `zhenyazhel80` | Registered the heater with the simulated node, exposed it in device discovery, and added a test for the listing. [PR #2](https://github.com/sanderkar/idata2304-gruppeprosjekt/pull/2) |
| Extend request routing, readings, and heater control | `Haidule`, `sanderkar` | Haidas extended `NodeConnection` and the application handlers. Sander extended the message codec and command routing and added protocol and node tests. Panels could request readings and send targeted heater commands. [Haidas’s routing changes](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/9659302) · [Sander’s command changes](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/3b022a3) |
| Track the protocol draft | `sanderkar`, `Haidule` | Sander removed the ignore rule covering `protocol.md`. Haidas added the first tracked draft and the generic actuator-command decoder. [Ignore rule](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/6433c03) · [First tracked protocol draft](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/6254a70) |
| Introduce a common actuator interface | `Haidule` | Added `Actuator` and adapted the heater and sensor-node model to use it. [Actuator interface](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/e1398d1) · [Sensor-node refactor](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/89c232f) |
| Implement access levels and shared credential configuration | `sanderkar` | Added panel identities, role checks, role tests, configuration loading, example credentials, and initial run instructions. [Access levels](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/a18be51) · [Role tests](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/94948a9) · [Shared configuration](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/67d21bf) |
| Support multiple sensor types | `sanderkar` | Added the sensor type model, sensor factory, humidity sensor, and tests. The node and panel handle temperature and humidity readings. [Sensor factory and humidity](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/dfbac3f) |

### Retrospective

Splitting work across the applications let the group develop in parallel.
Integration depended on consistent command formats and identifying which
requests could reach each device. Decoder, node, and role tests made those
boundaries explicit. Smaller merges with the complete suite run after each
integration would make later fixes easier to isolate.

## Sprint 3.1 – Configuration and anomaly detection

**Reconstructed work period** · 21 November 2026

### Sprint goal

Make the server address configurable across all applications and show warnings with sensor readings.

### Completed work

| Work item | Recorded contributor | Completed result and evidence |
| --- | --- | --- |
| Configure the server host and port | `zhenyazhel80` | Added `SMART_HOME_HOST` and `SMART_HOME_PORT` through the existing shared loader and applied them across all three applications. Updated the sample settings and documentation, stopped tracking local configuration, and added tests for defaults and invalid values. The PR reported 84 tests passing. [PR #4](https://github.com/sanderkar/idata2304-gruppeprosjekt/pull/4) |
| Integrate anomaly detection into the panel | `zhenyazhel80` | Connected the detector from Sprint 2 to the reading display. Added warnings for cold and hot temperatures and high humidity, with tests at all three boundaries. The PR reported 87 tests passing. [PR #5](https://github.com/sanderkar/idata2304-gruppeprosjekt/pull/5) |

### Retrospective

A shared host and port removed the need to hard-code a deployment address.
Keeping anomaly thresholds in the panel let the group add warnings without
changing the network messages. Boundary tests made the threshold behaviour
explicit. Documenting operating-system-specific startup examples earlier
would have made integration easier for the whole group.

## Sprint 3.2 – Generic actuator support, networking extras, and final integration

**Reconstructed work period** · 21–23 November 2026

### Sprint goal

Complete generic actuator handling, demonstrate the networking extras, and
make the final application easy to run and explain.

### Actuator integration

| Work item | Recorded contributor | Completed result and evidence |
| --- | --- | --- |
| Implement fan support and generic actuator control | `zhenyazhel80` | Added a fan alongside the heater, listed both types during discovery, and routed `SET_ACTUATOR` through the panel, server, and node. Added fan and generic control tests and updated the protocol report. PR #6 reported 93 tests passing. [PR #6](https://github.com/sanderkar/idata2304-gruppeprosjekt/pull/6) |
| Merge fan support and fix actuator status | `Haidule` | Merged PR #6 and fixed status queries for fans after integration. The final wire protocol uses `GET_ACTUATOR` for status, `SET_ACTUATOR` for control, and `ACTUATOR_STATE` for the reply. [Merge](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/8aca182) · [Fan status fix](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/6949409) |
| Integrate the actuator factory | `Haidule` | Restored `ActuatorFactory` and used it for heater and fan creation in the node application and its actuator-control test. [Factory](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/2d82792) · [Application and test integration](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/03ee0d7) |

### Networking extras

| Work item | Recorded contributor | Completed result and evidence |
| --- | --- | --- |
| Recover from temporary network outages | `sanderkar` | Added heartbeats and automatic reconnection. Nodes retain actuator state across reconnects. The panel retries read-only requests and can show a timestamped cached reading. It reports uncertain outcomes for actuator commands. Added recovery tests and demo instructions. [Outage recovery](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/7e56cb7) |
| Encrypt client connections with TLS 1.3 | `sanderkar` | Secured both client links. Clients verify the server certificate and address before sending registration tokens. Added keystore setup and tests for untrusted and mismatched certificates. [TLS implementation](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/4a4317b) |
| Improve TLS test compatibility on Windows | `Haidule` | Updated the server-side handshake test to accept the socket error Windows can return after a failed handshake. [TLS test follow-up](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/b6617bd) |
| Assign and persist unique node addresses | `sanderkar` | Added automatic registration for provisioned nodes, skipped configured static addresses, and persisted assignments across server restarts. Added allocation tests and running instructions. [Automatic addresses](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/1b55d3d) |

### Final integration, demo, and documentation

| Work item | Recorded contributor | Completed result and evidence |
| --- | --- | --- |
| Refine the sensor node, protocol, and connection code | `Haidule`, `sanderkar` | Haidas refactored and documented the sensor, actuator, protocol, panel, and configuration code. Sander integrated the networking changes and reconciled the shared protocol and running instructions. [Sensor-node refactor](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/17afcbf) · [Panel/client refactor](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/7434a2a) · [Final documentation](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/bd74f72) |
| Build the one-command launcher and support Windows paths | `sanderkar` | Added `run-local.sh` to build the project, provision temporary credentials and TLS files, and start the demo. Added Git Bash path handling and enforced LF line endings for the launcher. [Launcher](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/21be801) · [Windows path support](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/8014dcc) · [Line endings](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/cb3a650) |
| Improve the panel and add a second demo node | `Haidule` | Improved the panel display and input validation. Added node `13` with an extra humidity sensor and fan, both with device ID `3` in their respective categories. Updated the launcher to start both nodes. [Panel UI](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/4f9c8b1) · [Node 13](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/03ee0d7) · [Two-node launcher](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/b7bd996) |
| Document the final application | `Haidule`, `sanderkar` | Haidas added PlantUML diagrams for architecture, node classes, read and control flow, automatic registration, and connection recovery. Sander wrote and updated the README and revised the running guide and English protocol report to match the implementation. [Diagrams](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/ced4b27) · [README](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/e50cf7c) · [Final documentation](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/bd74f72) |
| Verify the integrated application | `sanderkar` | The final Maven test run verified for this report had 98 passing tests. The documented local demo lists nodes `12` and `13`, reads humidity from sensor `3` on node `13`, and controls that node’s fan `3`. [Final reviewed revision](https://github.com/sanderkar/idata2304-gruppeprosjekt/commit/bd74f72) |

### Presentation and hand-in

The group is responsible for the presentation. Confirm the status of the
slides, recording, and each member’s segment before hand-in. The assignment
calls for a 10–15 minute English video with each team member visible and
presenting. The demo should cover basic device communication and the three
networking extras.

### Retrospective

Generic actuator control affected the panel, routing, sensor node, and
device discovery. The fan status fix showed the value of testing a complete
request across all three applications after a merge. The networking extras
added failure cases that also needed integration tests. The two-node
launcher made end-to-end checks easier to repeat. A joint demo immediately
after each major merge would help catch integration issues earlier.

## Overall result

The final system provides encrypted TCP communication between multiple
sensor nodes and control panels through a central server. It supports
node and device discovery, temperature and humidity readings, targeted
heater and fan control, role-based permissions, and anomaly warnings.
The three networking extras are outage recovery, TLS 1.3, and persistent
automatic node addressing.

The fan integration PR reported 93 passing tests. The final Maven run
verified for this report passed all 98 tests.

## Overall reflection and improvements

Feature branches, pull requests, and commit history made implementation
work traceable. Automated tests and the two-node demo helped verify the
system after integration. For a future project, record planned tasks,
owners, and definitions of done on a shared issue board at the beginning
of each sprint. Compare those plans with completed work during each
retrospective. Smaller pull requests, continuous integration, and earlier
joint demos would make integration feedback easier to act on.

## Evidence and links

- [Source repository](https://github.com/sanderkar/idata2304-gruppeprosjekt)
- [Project assignment](https://github.com/sanderkar/idata2304-gruppeprosjekt/blob/main/IDATA2304GroupProject.pdf)
- [Protocol specification](https://github.com/sanderkar/idata2304-gruppeprosjekt/blob/main/docs/protocol.md)
- [Running guide](https://github.com/sanderkar/idata2304-gruppeprosjekt/blob/main/docs/running.md)
- [Architecture and sequence diagrams](https://github.com/sanderkar/idata2304-gruppeprosjekt/blob/main/docs/diagrams/index.md)
