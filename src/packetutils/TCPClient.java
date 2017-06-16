package packetutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;

/**
 * This class is intended to be executed as its own thread. The user is meant to
 * supply a raw IP address in the form of a string, and a port as an integer.
 * When the object attempts to connect to a TCPServer object, it will use the IP
 * and port to do so through a Socket object.
 * 
 * @author Alec J Strickland
 *
 */
public class TCPClient implements Runnable {

	private HashSet<File> files;
	private String input;
	private ObjectOutputStream outToServer;
	private ObjectInputStream inFromServer;
	private Socket clientSocket;
	private String ipAddress;
	private int port;

	public TCPClient() {

	}

	/**
	 * Constructs a TCPClient object. The parameters ip and port will be fed to
	 * the client's socket when connecting to the server.
	 * 
	 * @param ip
	 *            String representation of the raw IP address that the client
	 *            socket will attempt to handshake with.
	 * @param port
	 *            The port that the target server is bound to.
	 */
	public TCPClient(String ip, int port) {
		this.ipAddress = ip;
		this.port = port;
	}

	/**
	 * Attempts to open a connection with a server.
	 */
	@SuppressWarnings("unchecked")
	private void beginConnection() {
		input = null;
		try {
			// Attempt to handshake
			clientSocket = new Socket(ipAddress, port);
			clientSocket.setKeepAlive(true);
			// Wrap the socket's I/o streams with object streams.
			outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
			inFromServer = new ObjectInputStream(clientSocket.getInputStream());
			// Retrieve the available files from the server.
			files = (HashSet<File>) inFromServer.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setIP(String ip) {
		this.ipAddress = ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIP() {
		return ipAddress;
	}

	public int getPort() {
		return port;
	}

	/**
	 * Creates a file containing the data in the argument FileEvent object.
	 * 
	 * @param fileEvent
	 *            Object containing file data
	 * @return true - The file was successfully created. false - There was a
	 *         problem writing the file
	 */
	private boolean createAndWriteFile(FileEvent fileEvent) {
		File dstFile = new File(fileEvent.getFilename());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(dstFile);
			fos.write(fileEvent.getFileData());
			fos.flush();
			fos.close();
			System.out.println("File successfuly saved.");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public HashSet<File> getFiles() {
		return files;
	}

	/**
	 * This method is used to send file requests to the server and receive said
	 * files.
	 * 
	 * @param file
	 *            file currently being requested.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void sendInput(File file) throws IOException, ClassNotFoundException {
		try {
			// If the connection has been closed, attempt to reopen it.
			if (clientSocket.isClosed()) {
				beginConnection();
			}
			// Send file request to server.
			input = file.getName();
			outToServer.writeObject(input);

			// Stream the FileEvent object
			FileEvent fileEvent = (FileEvent) inFromServer.readObject();
			// while (file did not successfully transfer)
			// FileStatus value will be ERROR if there was a problem while
			// creating the FileEvent object on the server-side.
			while (!createAndWriteFile(fileEvent) || fileEvent.getStatus() == FileStatus.ERROR) {
				// Writing false to the output stream at this stage indicates to
				// the server that a new FileEvent object needs to be created
				// and sent to the client.
				outToServer.writeBoolean(false);
				fileEvent = (FileEvent) inFromServer.readObject();
			}
			// Confirm that file successfully transfered
			outToServer.writeObject(new Boolean(true));
			System.out.println("Successfully received " + input + " from server.");
		} catch (SocketException e) {
			clientSocket.close();
			// beginConnection();
		}
	}

	/**
	 * Closes the client socket and interrupts the thread running the TCPClient
	 * object.
	 * 
	 * @throws IOException
	 */
	public void killThread() throws IOException {
		clientSocket.close();
		Thread.currentThread().interrupt();
	}

	// Will return true if the client is connected and has received the files
	// from the server.
	public boolean isReady() {
		if (clientSocket == null) {
			return false;
		} else if (files == null) {
			return false;
		}
		return clientSocket.isConnected();
	}

	@Override
	public void run() {
		beginConnection();
	}
}
