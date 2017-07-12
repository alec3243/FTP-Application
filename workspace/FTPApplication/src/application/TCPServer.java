package application;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
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
	private Socket connectionSocket;
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
		files = new HashSet<>();
	}

	public TCPServer(Socket connectionSocket) throws IOException {
		this.connectionSocket = connectionSocket;
		try {
			inFromClient = new ObjectInputStream(this.connectionSocket.getInputStream());
			outToClient = new ObjectOutputStream(this.connectionSocket.getOutputStream());
		} catch (IOException e) {
			close();
		}
		files = new HashSet<>();
		dir = getDataFromConfig()[0];
	}

	// Closes the socket
	@Override
	public void close() throws IOException {
		if (!connectionSocket.isClosed()) {
			connectionSocket.close();
		}
	}

	/**
	 * Once the beginConnection() method has been called
	 * 
	 * @return boolean
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private boolean handleClient() throws IOException, ClassNotFoundException {
		try {
			// Get client input for file name
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
		} catch (SocketException | EOFException e) {
			return false;
		}
		return true;
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
	 * the server's available files.
	 * 
	 * @return A string representation of the directory containing the server's
	 *         available files.
	 * @throws FileNotFoundException
	 */
	private String[] getDataFromConfig() throws FileNotFoundException {
		Scanner in = new Scanner(new FileReader(CFG_FILE));
		String[] configData = new String[] { in.nextLine() };
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
			// TODO only retrieve files once in TCPServerManager, pass to
			// TCPServer
			listFilesForFolder(new File(dir));
			// Send the file set to the output stream.
			outToClient.writeObject(files);
			boolean keepRunning = true;
			while (keepRunning) {
				keepRunning = handleClient();
			}
			close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
