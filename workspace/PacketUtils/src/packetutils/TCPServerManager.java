package packetutils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class TCPServerManager implements Runnable {
	private final int MIN_PORT = 49152;
	private final int MAX_PORT = 65535;
	private int currentPort;
	private ServerSocket serverSocket;
	private Socket connectionSocket;
	private List<ServerSocket> serverSockets;

	TCPServerManager() {
		currentPort = MIN_PORT;
		serverSocket = null;
		connectionSocket = null;
		serverSockets = new LinkedList<>();
	}

	@Override
	public void run() {
		try {
			while (true) {
				if (currentPort > MAX_PORT) {
					break;
				}
				listen();
			}
			for (ServerSocket s : serverSockets) {
				s.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void listen() throws IOException {
		try {
			TCPServer server = null;
			serverSocket = new ServerSocket(currentPort);
			serverSockets.add(serverSocket);
			connectionSocket = serverSocket.accept();
			currentPort++;
			server = new TCPServer(connectionSocket);
			Thread t = new Thread(server);
			t.setDaemon(true);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
