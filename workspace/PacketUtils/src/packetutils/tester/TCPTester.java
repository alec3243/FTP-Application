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
	private TCPClient client;
	private TCPServer server;

	@Test
	public void downloadTest() throws ClassNotFoundException, IOException {
		server = new TCPServer();
		new Thread(server).start();

		// client = new TCPClient("localhost", 9876);
		new Thread(client).start();
		while (!client.isReady()) {
			Thread.yield();
		}
		HashSet<File> files = client.getFiles();
		List<File> fileList = new ArrayList<>(files);
		int index = 0;
		for (int i = 0; i < 70; i++) {
			index = ThreadLocalRandom.current().nextInt(0, fileList.size());
			client.sendInput(fileList.get(index));
		}
	}

	@Test
	public void handshakeTest() throws IOException {
		try {
			for (int i = 0; i < 100; i++) {
				server = new TCPServer();
				new Thread(server).start();
				// client = new TCPClient("localhost", 9876);
				new Thread(client).start();
				while (!client.isReady()) {
					Thread.yield();
				}
				client.close();
				server.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
