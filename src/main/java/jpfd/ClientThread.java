package jpfd;

import java.io.*;
import java.net.*;

/**
 * ClientThread is responsible for starting forwarding between
 * the client and the server. It keeps track of the client and
 * servers sockets that are both closed on input/output error
 * durinf the forwarding. The forwarding is bidirectional and
 * is performed by two ForwardThread instances.
 */
class ClientThread extends Thread
{
	private Socket clientSocket = null;
	private Socket serverSocket = null;

	private boolean mForwardingActive = false;

	private String destHost = null;
	private int destPort;

	public ClientThread(final String destHost, final int destPort, Socket clientSocket)
	{
		this.clientSocket = clientSocket;

		this.destHost = destHost;
		this.destPort = destPort;
	}

	/**
	 * Establishes connection to the destination server and
	 * starts bidirectional forwarding ot data between the
	 * client and the server.
	 */
	public void run()
	{
		InputStream clientIn;
		OutputStream clientOut;
		InputStream serverIn;
		OutputStream serverOut;

		try
		{
			//
			// Connect to the destination server
			//
			serverSocket = new Socket(destHost, destPort);

			//
			// Turn on keep-alive for both the sockets
			//
			serverSocket.setKeepAlive(true);
			clientSocket.setKeepAlive(true);

			//
			// Obtain client & server input & output streams
			//
			clientIn  = clientSocket.getInputStream();
			serverIn  = serverSocket.getInputStream();
			clientOut = clientSocket.getOutputStream();
			serverOut = serverSocket.getOutputStream();
		}
		catch (IOException ioe)
		{
			System.err.println("Can not connect to " + destHost + ":" + destPort);
			connectionBroken();
			return;
		}

		//
		// Start forwarding data between server and client
		//
		mForwardingActive = true;

		ForwardThread clientForward = new ForwardThread(this, clientIn, serverOut);
		ForwardThread serverForward = new ForwardThread(this, serverIn, clientOut);

		clientForward.start();
		serverForward.start();

		System.out.println("TCP Forwarding " +
		                   clientSocket.getInetAddress().getHostAddress() +
		                   ":" + clientSocket.getPort() + " <--> " +
		                   serverSocket.getInetAddress().getHostAddress() +
		                   ":" + serverSocket.getPort() + " started.");
	}

	/**
	 * Called by some of the forwarding threads to indicate
	 * that its socket connection is brokean and both client
	 * and server sockets should be closed. Closing the client
	 * and server sockets causes all threads blocked on reading
	 * or writing to these sockets to get an exception and to
	 * finish their execution.
	 */
	public synchronized void connectionBroken()
	{
		try
		{
			serverSocket.close();
		}
		catch (Exception e) {}

		try
		{
			clientSocket.close();
		}
		catch (Exception e) {}

		if (mForwardingActive)
		{
			System.out.println("TCP Forwarding " +
			                   clientSocket.getInetAddress().getHostAddress()
			                   + ":" + clientSocket.getPort() + " <--> " +
			                   serverSocket.getInetAddress().getHostAddress()
			                   + ":" + serverSocket.getPort() + " stopped.");

			mForwardingActive = false;
		}
	}
}
