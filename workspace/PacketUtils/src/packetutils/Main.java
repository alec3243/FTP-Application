package packetutils;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		TCPServerManager manager = new TCPServerManager();
		new Thread(manager).start();
		FtpApplication app = new FtpApplication();
		app.startApplication();
	}
}
