package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * This class is used to manage TCPServers. It listens for incoming connections
 * starting on port 49152, and increments the port number for each successful
 * connection up to 65535. Each time a connection is made to a socket, that
 * socket is passed to a TCPServer object that gets executed on its own daemon
 * thread. All data is sent over SSL/TLS using SSLSockets.
 * 
 * @author Alec J Strickland
 *
 */
public class TCPServerManager implements Runnable {
	private int currentPort;
	private List<SSLServerSocket> serverSockets;
	private SSLServerSocketFactory socketFactory;
	private final String cipherSuites[] = { "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
			"SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_NULL_SHA256" };

	TCPServerManager() {
		serverSockets = new LinkedList<>();
		SSLContext sslContext = createSSLContext();
		socketFactory = sslContext.getServerSocketFactory();
	}

	/**
	 * Returns a SSLContext object using the KeyStore in the project directory.
	 * 
	 * @return SSLContext object using local KeyStore file.
	 */
	private SSLContext createSSLContext() {
		try {
			// KeyStore keystore = createKeyStore();
			KeyStore keystore = KeyStore.getInstance("JKS");
			/*
			 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			 * Here you must insert the directory of your keystore file as well as the password for it
			 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			 */
			keystore.load(new FileInputStream("<INSERT KEYSTORE DIRECTORY>"), "<INSERT KEYSTORE PASSWORD>".toCharArray());
			// Create key manager
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			/*
			 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			 * Here you must insert the password for your keystore file
			 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			 */
			keyManagerFactory.init(keystore, "<INSERT KEYSTORE PASSWORD>".toCharArray());
			KeyManager[] km = keyManagerFactory.getKeyManagers();

			// Create trust manager
			TrustManagerFactory trustfactory = TrustManagerFactory.getInstance("SunX509");
			trustfactory.init(keystore);
			TrustManager[] tm = trustfactory.getTrustManagers();

			// Initialize SSL context
			SSLContext sslContext = SSLContext.getInstance("TLSv1");
			sslContext.init(km, tm, null);
			return sslContext;

		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// private KeyStore createKeyStore() throws Exception {
	// File file = new File("keystore.jks");
	// KeyStore keyStore = KeyStore.getInstance("JKS");
	// if (file.exists()) {
	// // if exists, load
	// keyStore.load(new FileInputStream(file), "123456".toCharArray());
	// } else {
	// // if not exists, create
	// keyStore.load(null, null);
	// keyStore.store(new FileOutputStream(file), "123456".toCharArray());
	// }
	// return keyStore;
	// }

	/**
	 * Runs the TCPServerManager.
	 */
	@Override
	public void run() {
		try {
			final int MIN_PORT = 49152;
			final int MAX_PORT = 65535;
			currentPort = MIN_PORT;
			// System.out.println("Push 'x' to terminate servers.");
			// Scanner in = new Scanner(System.in);
			while (currentPort < MAX_PORT) {
				listen();
				// if (in.next().equals("x")) {
				// for (ServerSocket s : serverSockets) {
				// if (!s.isClosed()) {
				// s.close();
				// }
				// }
				// System.out.println("Sockets closed.");
				// break;
				// }
			}
			// in.close();
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
			SSLServerSocket sslServerSocket = (SSLServerSocket) socketFactory.createServerSocket(currentPort);
			serverSockets.add(sslServerSocket);
			SSLSocket sslConnectionSocket = (SSLSocket) sslServerSocket.accept();
			sslConnectionSocket.setEnabledCipherSuites(cipherSuites);
			currentPort++;
			TCPServer server = new TCPServer(sslConnectionSocket);
			Thread t = new Thread(server);
			t.setDaemon(true);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
