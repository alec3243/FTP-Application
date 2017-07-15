package application;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * This class is used to manage TCPServers. It listens for incoming connections
 * starting on port 49152, and increments the port number for each successful
 * connection up to 65535. Each time a connection is made to a socket, that
 * socket is passed to a TCPServer object that gets executed on its own daemon
 * thread.
 * 
 * @author Alec J Strickland
 *
 */
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

	/**
	 * Runs the TCPServerManager.
	 */
	@Override
	public void run() {
		try {
			System.out.println("Push 'x' to terminate servers.");
//			Scanner in = new Scanner(System.in);
			while (currentPort < MAX_PORT) {
				listen();
//				if (in.next().equals("x")) {
//					for (ServerSocket s : serverSockets) {
//						if (!s.isClosed()) {
//							s.close();
//						}
//					}
//					System.out.println("Sockets closed.");
//					break;
//				}
			}
//			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Listens for incoming connections from clients.
	 * 
	 * @throws IOException
	 */
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
