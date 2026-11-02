package no.ntnu.idata2304.smarthome.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * Entry point
 */
public final class ServerApplication {

  private ServerSocket serverSocket;

  // constructor
  private ServerApplication(int port) throws IOException {
    this.serverSocket = new ServerSocket(port);
  }

  public static void main(String[] args) {
  }

}
