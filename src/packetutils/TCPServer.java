package packetutils;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Scanner;

/**
 * This class is intended to be executed as its own thread. It is dependent on a
 * text file in the project directory named "config.cfg", which supplies the
 * directory on the first line containing all available files for the server to
 * transfer, and the port number on the second line which the server will be
 * bound to upon running. All communications are done securely over SSL/TLS
 * security protocols using an SSLServerSocket object to communicate with the
 * client.
 * 
 * @author Alec J Strickland
 *
 */
public class TCPServer implements RunnableEndPoint {
	private final File CFG_FILE = new File("config.cfg");
	private HashSet<File> files;
	private ObjectInputStream inFromClient;
	private ObjectOutputStream outToClient;
	private ServerSocket serverSocket;
	private Socket connectionSocket;
	private int port;
	private String dir;

	/**
	 * Constructs a TCPServer object. This constructor retrieves the two data
	 * fields from the config file, which will be used once a thread is started
	 * for this object.
	 * 
	 * @throws FileNotFoundException
	 */
	public TCPServer() throws FileNotFoundException {
		String[] configData = getDataFromConfig();
		dir = configData[0];
		port = Integer.parseInt(configData[1]);

	}

	/**
	 * Opens a socket associated with the port read from the config.cfg file,
	 * listens for a connection to be made and accepts it, and wraps the socket
	 * I/O streams with object streams.
	 * 
	 * @throws IOException
	 */
	private void beginConnection() throws IOException {
		// Establish server end-point socket
		try {
			serverSocket = new ServerSocket(port);

			files = new HashSet<>();
			// Wait for handshake with client
			connectionSocket = serverSocket.accept();
			connectionSocket.setKeepAlive(true);
			// Establish streams
			inFromClient = new ObjectInputStream(connectionSocket.getInputStream());
			outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
		} catch (IOException e) {
			stop();
		}
	}

	// Closes both sockets and interrupts the thread.
	@Override
	public void stop() throws IOException {
		if (!connectionSocket.isClosed()) {
			connectionSocket.close();
		}
		if (!serverSocket.isClosed()) {
			serverSocket.close();
		}
		Thread.currentThread().interrupt();
	}

	/**
	 * Once the beginConnection() method has been called
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void handleClient() throws IOException, ClassNotFoundException {
		listFilesForFolder(new File(dir));
		// Send the file set to the output stream.
		outToClient.writeObject(files);
		// Get client input for file name
		while (true) {
			try {

				String clientInput = (String) inFromClient.readObject();
				// Append '\' to the end of the path if it has not already been
				// done
				dir = (dir.charAt(dir.length() - 1) != '\\') ? dir + "\\" : dir;

				// Write fileEvent to the output stream. If their is a problem
				// with the fileEvent object as it's being created, the client
				// will request a new one, which will then be created and sent,
				// and the process will repeat until the
				// client receives a valid fileEvent object.
				do {
					// Get the file data
					FileEvent fileEvent = getFileEvent(clientInput, dir);
					// Write the object to the output stream
					outToClient.writeObject(fileEvent);
				} while (!((Boolean) inFromClient.readObject()).booleanValue());
			} catch (SocketTimeoutException | SocketException | EOFException e) {
				stop();
			}
		}

	}

	/**
	 * Adds every file in the given directory to the 'files' HashSet. If the
	 * given File is not a directory, no files will be added to the HashSet.
	 * 
	 * @param folder
	 *            FIle object representing the folder.
	 * @throws IOException
	 */
	private void listFilesForFolder(final File folder) throws IOException {
		if (folder.isDirectory()) {
			for (final File fileEntry : folder.listFiles()) {
				if (fileEntry.isFile()) {
					files.add(fileEntry);
				}
			}
		}
	}

	/**
	 * Opens the program's config file, located in the same directory as the
	 * program, and reads the directory that will be used as the root containing
	 * the server's available files, as well as the port that the socket is
	 * binded to.
	 * 
	 * @return A string representation of the directory containing the server's
	 *         available files.
	 * @throws FileNotFoundException
	 */
	private String[] getDataFromConfig() throws FileNotFoundException {
		Scanner in = new Scanner(new FileReader(CFG_FILE));
		String[] configData = new String[] { in.nextLine(), in.nextLine() };
		in.close();
		return configData;
	}

	/**
	 * Returns a FileEvent object containing all of the necessary data from the
	 * specified file in the specified directory.
	 * 
	 * @param fileName
	 *            String representation of the desired file name within the
	 *            given directory.
	 * @param dir
	 *            String representation of the directory containing the file.
	 * @return
	 */
	private FileEvent getFileEvent(String fileName, String dir) {
		final FileEvent fileEvent = new FileEvent();
		final File file = new File(dir + fileName);
		try {
			// Establish DataInputStream that will stream the bytes from the
			// file to the byte array.
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			long len = (int) file.length();
			byte[] fileBytes = new byte[(int) len];
			int read = 0;
			int numRead = 0;
			// Stream the data into the byte array.
			while (read < fileBytes.length && (numRead = dis.read(fileBytes, read, fileBytes.length - read)) >= 0) {
				read += numRead;
			}
			fileEvent.setFilename(fileName);
			fileEvent.setFileSize(len);
			fileEvent.setFileData(fileBytes);
			fileEvent.setStatus(FileStatus.SUCCESS);
			dis.close();
		} catch (Exception e) {
			e.printStackTrace();
			fileEvent.setStatus(FileStatus.ERROR);
		}
		return fileEvent;
	}

	/**
	 * Runs the TCPServer object, which listens for a connection and
	 * subsequently handles any file requests.
	 */
	@Override
	public void run() {
		try {
			beginConnection();
			handleClient();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
