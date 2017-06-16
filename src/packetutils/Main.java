package packetutils;

import java.io.FileNotFoundException;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		TCPServer server = new TCPServer();
		new Thread(server).start();
		FtpApplication.main(null);
	}
}
