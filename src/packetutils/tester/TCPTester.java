package packetutils.tester;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import packetutils.TCPClient;
import packetutils.TCPServer;

public class TCPTester {

	// Be sure to start the TCPServer thread prior to running this test.
	@Test
	public void downloadTest() throws ClassNotFoundException, IOException {
		TCPServer server = new TCPServer();
		new Thread(server).start();
		TCPClient client = new TCPClient();
		new Thread(client).start();
		client.setIP("localhost");
		client.setPort(9876);
		while (!client.isReady()) {
			System.out.println("well shit");
		}
		HashSet<File> files = client.getFiles();
		List<File> fileList = new ArrayList<>(files);
		int index = 0;
		for (int i = 0; i < 100; i++) {
			index = ThreadLocalRandom.current().nextInt(0, fileList.size());
			client.sendInput(fileList.get(index));
		}
	}

	@Test
	public void handshakeTest() throws IOException {
		try {
			for (int i = 0; i < 100; i++) {
				TCPServer server = new TCPServer();
				Thread serverThread = new Thread(server);
				serverThread.start();
				TCPClient client = new TCPClient();
				client.setIP("localhost");
				client.setPort(9876);
				Thread clientThread = new Thread(client);
				clientThread.start();
				while (!client.isReady()) {
					System.out.println("well shit");
				}
				System.out.flush();
				server.killThread();
				client.killThread();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
