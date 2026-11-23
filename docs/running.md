# Running locally

## Quick start

With JDK 25 installed, run this from the project root on macOS or Linux.

```sh
./run-local.sh
```

The launcher builds the project, generates temporary credentials and a TLS
certificate, and opens the control panel in the same terminal. Try `list` or
`read 12 1`. Type `exit` to stop all three apps and remove the temporary files.
If port 5000 is busy, run `SMART_HOME_DEMO_PORT=5001 ./run-local.sh`.

## Manual setup

Use this setup for IntelliJ, separate terminals, or separate machines. Use
Java 25 and run each application with the project root as its working directory.

Copy `config/local.properties.example` to `config/local.properties` and replace
the placeholder tokens with your own values. The local file is ignored by Git.
The server, sensor node and control panel all read this same file automatically.
You do not need to enter credentials in each IntelliJ run configuration.

Environment variables override values in the file. Remove old token environment
variables from your IntelliJ run configurations when switching to the local file;
otherwise different overrides can cause authentication failures.

The server address is configured with `SMART_HOME_HOST` and `SMART_HOME_PORT`.
The defaults are `127.0.0.1` and `5000`. Set the same values for the server,
sensor nodes and control panels. When running on different machines, use the
server machine's reachable IP address or DNS name from the clients.

### Set up TLS

All connections use TLS 1.3. From the project root, use the Java 25 `keytool`
to create a local server identity and export its public certificate:

```sh
mkdir -p config/tls
keytool -genkeypair -alias smart-home-server -keyalg EC -groupname secp256r1 \
  -storetype PKCS12 -keystore config/tls/server.p12 -validity 365 \
  -dname "CN=Smart Home Server" -ext "SAN=dns:localhost,ip:127.0.0.1" \
  -ext "EKU=serverAuth"
keytool -exportcert -alias smart-home-server -keystore config/tls/server.p12 \
  -rfc -file config/tls/server.crt
```

Enter the keystore password when prompted. Set
`SMART_HOME_TLS_KEYSTORE_PASSWORD` on the server to the same value. Add this
setting to your existing `config/local.properties` if you already have one.
The server's default keystore path is `config/tls/server.p12`. Clients load
`config/tls/server.crt`. Use `SMART_HOME_TLS_KEYSTORE` and
`SMART_HOME_TLS_CERTIFICATE` to override those paths.

For a demo across machines, include the server address used by the clients in
the certificate's SAN. For example, add `ip:192.168.1.20` or `dns:sha.example`
to the comma-separated SAN list before generating the certificate. Copy
`server.crt` to both client machines over a trusted channel. Keep `server.p12`
and its password on the server. The generated TLS files are ignored by Git.
When replacing the certificate, distribute the new public certificate and
restart the affected applications.

## Start the applications

Run these main classes in this order, keeping the first two running:

1. `no.ntnu.idata2304.smarthome.server.ServerApplication`
2. `no.ntnu.idata2304.smarthome.sensornode.SensorNodeApplication`
3. `no.ntnu.idata2304.smarthome.controlpanel.ControlPanelApplication`

The default node is `12`, with temperature sensor `1` and heater `1`. The default
panel user is `owner-example`. By default, the applications communicate on `127.0.0.1:5000`.
The node should receive `READY;1` and the panel should display its command menu.
The server logs `Client connected over TLSv1.3` for each successful handshake.
If a client reports a TLS handshake error, verify its public certificate and
that `SMART_HOME_HOST` matches the certificate.

Try these commands in the control panel:

```text
list
read 12 1
on 12 1
status 12 1
off 12 1
exit
```

## Demonstrate recovery after an outage

1. Start the three applications separately. In the panel, run `read 12 1` and `on 12 2` to
   save a temperature reading and turn on the fan.
2. Stop only the server. Leave the sensor node and panel running. Check that
   the panel reports the interrupted connection.
3. Enter `read 12 1` while the server is down. Leave it down through the retry
   window to see the last confirmed reading and its timestamp. Enter the same
   command again and restart the server while the command is waiting to see it
   return a fresh reading.
4. Run `list` and `status 12 2` after the clients reconnect. Node 12 will
   appear again and its fan will still be on.

If the connection drops during an actuator command, check the status after
recovery before issuing another command. An already-executed command may have
lost its reply.

## Other nodes and users

Pass a node ID as the sensor application's first program argument. For node `13`,
add `SMART_HOME_NODE_13_TOKEN` to the local file before starting the server.

### Automatic node addresses

Give each automatic node a unique device name and its own token. Names start
with an uppercase letter and can contain uppercase letters, digits, and
underscores, up to 32 characters. Put matching credentials on the server and
the corresponding node. For a local demo, put both entries in your local file
and replace the sample token values:

```properties
SMART_HOME_AUTO_NODE_KITCHEN_TOKEN=replace-with-kitchen-token
SMART_HOME_AUTO_NODE_GARAGE_TOKEN=replace-with-garage-token
```

1. Start the server. Launch two sensor applications with program arguments
   `--auto KITCHEN` and `--auto GARAGE`.
2. Run `list` from the panel to see two different addresses. Each node also
   prints its address. Use one of them in `read <address> 1` or
   `status <address> 2`.
3. Restart the server and run `list` once the nodes reconnect. Both addresses
   should be the same as before.

The server stores the mapping in `config/state/node-addresses.properties`.
Keep this file across restarts, redeployments, and backups. Use
`SMART_HOME_NODE_ADDRESS_FILE` to move it to a writable local directory
owned by one server instance. Static addresses configured on the server are
skipped during allocation. If an address was already assigned to an automatic
node, choose a different static address. Guest device grants still use the
numeric address shown in the panel.

### Panel accounts

Pass a user ID as the panel application's first program argument. The configured
example users are `owner-example`, `sha-example`, `guest-example` and
`technician-example`. The panel selects the matching token name from
`config/panel-users.properties`; the server enforces that account's permissions.

Restart the server and affected clients after changing credentials. Each process
loads its configuration at startup. `ERROR;AUTH_FAILED` means the supplied token
does not match the server's configured token or the panel account is disabled.
Setting a token only in the client's environment does not configure the server.

The shared local file is for development on one computer. For separate machines,
provision each client with its own token and configure the server with matching
tokens. Do not distribute a file containing every account's credentials to users.
