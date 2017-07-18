package application;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		System.setProperty("javax.net.ssl.trustStore", "DebKeyStore.jks");
		System.setProperty("javax.net.ssl.keyStore", "caroot.cer");
		TCPServerManager manager = new TCPServerManager();
		new Thread(manager).start();
		FtpApplication app = new FtpApplication();
		app.startApplication();
	}
}
