# SHA Smart Home

This is a smart-home simulation built for NTNU’s IDATA2304 Computer Communication and Network Programming course. 
The project implements a custom application protocol over TCP with TLS 1.3. 
A control panel discovers sensor nodes, reads temperature and humidity, checks heater and fan status, and sends commands through a server.

## Start


### MacOS & Linux start
Install JDK 25 and run this from the project root on macOS or Linux.

```sh
./run-local.sh
```

### Windows start
Install JDK 25 and run this from the terminal. 

```powershell
& "C:\Program Files\Git\bin\bash.exe" ./run-local.sh
```
This only works if Git is installed at the default location. If your Git isn't stored in the default location, you can run the following from
git bash:
```bash
bash ./run-local.sh
```


The launcher builds the project using the ./run-local.sh file, , creates temporary login tokens and a TLS certificate, 
and starts the server, sensor node, and control panel. The panel should open in the same terminal.

In the demo, node `12` has temperature sensor `1`, humidity sensor `2`, heater `1`, and fan `2`. Type `exit` to stop the demo.

### Manual setup
The ./run-local.sh file is there to make the setup much easier and more user-friendly. 
However, you can set it up manually if you'd like. 
Use this setup for IntelliJ, separate terminals, or separate machines. 

Copy config/local.properties.example to config/local.properties and replace the placeholder tokens with your own values.
Use Java 25 and run each application with the project root as its working directory.

The local file is ignored by Git. The server, sensor node and control panel all read this same file automatically. 
You do not need to enter credentials in each IntelliJ run configuration.

Environment variables override values in the file. Remove old token environment variables from your IntelliJ run configurations when switching to the local file.
Otherwise different overrides can cause authentication failures.
The server address is configured with SMART_HOME_HOST and SMART_HOME_PORT. 
The defaults are `127.0.0.1` and port `5000`. Set the same values for the server, sensor nodes and control panels. 
When running on different machines, use the server machine's IP address or DNS name from the clients.



## How it works
The server authenticates clients, applies role permissions, and routes requests. 
Sensor nodes simulate readings and actuator state. 
The control panel discovers nodes, displays readings and anomaly warnings, and sends commands to specific devices.

The [protocol specification](protocol.md) describes the message formats, registration, permissions, and error handling.

## Networking extras
#### Outage recovery
Nodes and panels reconnect automatically. 
The panel retries interrupted reads and can show a timestamped cached reading during a longer outage.

#### Encrypted transport 
Both client connections use TLS 1.3. 
Clients verify the server’s certificate and address before sending login tokens.

#### Automatic node addresses
The server assigns unique numeric addresses to provisioned nodes and saves the assignments across reconnects and server restarts.

## How to run tests

Run the Maven test suite with JDK 25. The TLS integration tests use localhost sockets.

```sh
mvn test
```
