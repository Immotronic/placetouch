/*
 *
 * Copyright (c) Immotronic, 2012
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of placetouch, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package fr.immotronic.ubikit.placetouch.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ubikit.Logger;



public final class HttpRedirectorServer implements Runnable
{
	private Thread thread = null;
	private ServerSocket serverSocket = null;

	private int listeningPort;
	private int targetedPort;

	private Pattern urlExtractorRegex = Pattern.compile("GET (.*) HTTP/1[.].");
	private Pattern hostExtractorRegex = Pattern.compile("Host: (\\S*)");

	private volatile boolean running = false;






	public HttpRedirectorServer(int listeningPort, int targetedPort)
	{
		this.listeningPort = listeningPort;
		this.targetedPort = targetedPort;
		if (listeningPort == targetedPort)
		{
			Logger.warn(
				LC.gi(),
				this,
				"Listening and targeted port are the same. HttpRedirector is useless.");
		}
	}






	@Override
	public void run()
	{
		try
		{
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(listeningPort));
			Logger.debug(LC.gi(), this, "@@  TCP server running on port " + listeningPort);

			while (true)
			{
				running = true;

				Logger.debug(LC.gi(), this, "@@  WAITING Incoming connection.");
				Socket connectionSocket = null;
				try
				{
					connectionSocket = serverSocket.accept();
					connectionSocket.setSoTimeout(1000);
				}
				catch (SocketException e)
				{
					break;
				}

				Logger.debug(LC.gi(), this, "@@  Incoming connection: reading...");
				BufferedReader in =
					new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

				String url = null;
				String host = null;

				try
				{
					String line = null;
					int lineCount = 0;
					while ((line = in.readLine()) != null && lineCount < 2)
					{
						switch (lineCount)
						{
							case 0: {
								Matcher matcher = urlExtractorRegex.matcher(line);
								if (matcher.find())
								{
									url = matcher.group(1);
									Logger.debug(LC.gi(), this, "... requested URL: " + url);
								}
								else
								{
									Logger.info(LC.gi(), this, "... No URL found in " + line);
								}
								break;
							}
							case 1: {
								Matcher matcher = hostExtractorRegex.matcher(line);
								if (matcher.find())
								{
									host = matcher.group(1);
									int portDelimiter = host.indexOf(':');
									if (portDelimiter != -1)
									{
										host = host.substring(0, portDelimiter);
									}
									Logger.debug(LC.gi(), this, "... on host: " + host);
								}
								else
								{
									Logger.info(LC.gi(), this, "... No host found in " + line);
								}
								break;
							}
							default:
								break;
						}
						lineCount++;
					}
				}
				catch (SocketTimeoutException e)
				{
					in.close();
				}

				if (url != null && host != null)
				{
					PrintWriter out = new PrintWriter(connectionSocket.getOutputStream());
					out.println("HTTP/1.1 301 Moved Permanently");
					out.println("Location: http://" + host + ":" + targetedPort + url);
					out.println("Content-Length: 0");
					out.flush();

					Logger.debug(LC.gi(), this, "@@ Redirecting '" + url + "' to '" + host + ":"
						+ targetedPort + "/'");
				}
				else
				{
					Logger.debug(LC.gi(), this, "Request is not a HTTP request or client is not "
						+ "quick enougth to perform its GET request. It has been ignored.");
				}

				connectionSocket.close();
			}
		}
		catch (BindException e)
		{
			Logger.error(LC.gi(), this, "The '" + this.listeningPort
				+ "' port is already in use. Cannot bind the HttpRedirector on it.");
		}
		catch (IOException e)
		{
			Logger.error(LC.gi(), this, "While serving requests for HttpRedirector server", e);
			try
			{
				serverSocket.close();
			}
			catch (IOException e1)
			{}
			finally
			{
				serverSocket = null;
				thread = null;
			}
		}

		running = false;
		Logger.info(LC.gi(), this, "@@  HttpRedirectorServer thread has stopped.");
	}






	public synchronized void start()
	{
		if (listeningPort != targetedPort)
		{
			thread = new Thread(this);
			thread.start();
		}
	}






	public synchronized void stop()
	{
		if (serverSocket != null)
		{
			Logger.info(LC.gi(), this, "@@  HttpRedirectorServer thread is stopping...");

			try
			{
				serverSocket.close();
				while (running)
				{
					try
					{
						Thread.sleep(100);
					}
					catch (InterruptedException e)
					{}
				}
			}
			catch (IOException e)
			{
				Logger.error(LC.gi(), this, "While closing the HttpRedirector server socket", e);
			}
		}

		thread = null;
	}
}
