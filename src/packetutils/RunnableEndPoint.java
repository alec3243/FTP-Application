package packetutils;

import java.io.IOException;

/**
 * Implementing this interface is meant to integrate Thread functionality with
 * Socket connections.
 * 
 * @author Alec J Strickland
 *
 */
public interface RunnableEndPoint extends Runnable {

	/**
	 * This method gets called when the end-point needs to terminate without
	 * leaving behind resource leaks.
	 * 
	 * @throws IOException
	 *             When there's a problem closing the sockets.
	 */
	void stop() throws IOException;
}
