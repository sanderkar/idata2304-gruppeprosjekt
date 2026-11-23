# Running locally

Use Java 25. All three applications must use the project root as their working
directory so they can read the `config` directory.

## One-time setup

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
server machine's reachable IP address instead of `127.0.0.1`.

## Start the applications

Run these main classes in this order, keeping the first two running:

1. `no.ntnu.idata2304.smarthome.server.ServerApplication`
2. `no.ntnu.idata2304.smarthome.sensornode.SensorNodeApplication`
3. `no.ntnu.idata2304.smarthome.controlpanel.ControlPanelApplication`

The default node is `12`, with temperature sensor `1` and heater `1`. The default
panel user is `owner-example`. By default, the applications communicate on `127.0.0.1:5000`.
The node should receive `READY;1`; the panel should display its command menu.

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

1. Run the three applications. In the panel, run `read 12 1` and `on 12 2` to
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
