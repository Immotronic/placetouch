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

package fr.immotronic.ubikit.placetouch.networkmanager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.ConfigurationView;

import fr.immotronic.tools.Base64;
import fr.immotronic.ubikit.placetouch.auth.AuthManager;
import fr.immotronic.ubikit.placetouch.cloud.WebAPI;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.DomainStatus;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.TunnelStatus;



final class DistantAccessManager
{
	private static int connectionTimeout = 30000;
	private static final String DDNS_RESPONSE_NOT_REGISTERED = "UNREGISTERED";
	private static final String DDNS_RESPONSE_REACHABLE = "OK";
	private static final String DDNS_RESPONSE_TUNNEL_REQUESTED = "TUNNEL_REQUESTED";
	private static final String DDNS_RESPONSE_UNREACHABLE = "UNREACHABLE";

	private final AuthManager authManager;
	private final ConfigurationView configurationView;

	private String currentHttpPort = null;
	private String currentSshPort = null;

	private JSONObject currentTunnelPorts = null;
	private boolean domainChanged = false;

	private DomainStatus domainStatus = null;

	private boolean tunnelChanged = false;
	private TunnelStatus tunnelStatus = null;

	final Logger logger = LoggerFactory.getLogger(DistantAccessManager.class);






	DistantAccessManager(ConfigurationView configurationView, AuthManager authManager)
	{
		this.configurationView = configurationView;
		this.authManager = authManager;

		tunnelStatus = checkTunnelStatus();
	}






	synchronized DomainStatus check()
	{
		logger.debug("Testing distant access...");

		try
		{
			logger.debug("DDNS 'isReachable' URL = {}", WebAPI.DDNS.v1_1.isReachable.getUrl());

			URL url = WebAPI.DDNS.v1_1.isReachable.getUrl();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(connectionTimeout);
			connection.setReadTimeout(connectionTimeout);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("GET");

			setAuthorization(connection);

			connection.setRequestProperty("Accept", WebAPI.DDNS.v1_2.isReachable
				.getResponseContentType());

			int responseCode = connection.getResponseCode();
			String contentType = connection.getContentType();

			if (responseCode == 200
				&& contentType.equals(WebAPI.DDNS.v1_2.isReachable.getResponseContentType()))
			{
				BufferedReader in = new BufferedReader(
					new InputStreamReader((InputStream) connection.getContent()));

				String response = in.readLine();

				logger.debug("DDNS server response is '{}'", response);

				if (response.contains(DDNS_RESPONSE_REACHABLE))
				{
					setDomainStatus(DomainStatus.LINKED);
					logger.debug("Domain is REGISTERED and LINKED.");
				}
				else if (response.contains(DDNS_RESPONSE_UNREACHABLE))
				{
					setDomainStatus(DomainStatus.LINK_ISSUE);
					logger.info("Domain is not reachable from Internet.");
				}
				else if (response.equals(DDNS_RESPONSE_NOT_REGISTERED))
				{
					setDomainStatus(DomainStatus.NOT_REGISTERED);
					logger.info("Domain is not registered in DDNS.");
				}
				else
				{
					logger.error("INVALID RESPONSE FROM THE DDNS SERVER: {}.", response);
				}

				manageTunnel(
					response.contains(DDNS_RESPONSE_TUNNEL_REQUESTED) ? TunnelStatus.OPEN
						: TunnelStatus.CLOSED);
			}
			else if (responseCode == 401)
			{
				setDomainStatus(DomainStatus.INVALID_CREDENTIALS);
				logger.error("Cannot test domain link: 401 invalid credentials.");
			}
			else
			{
				setDomainStatus(DomainStatus.LINK_ISSUE);
				logger.error(
					"Cannot test domain link: Error code is {}, Response Content-Type: "
						+ "{}, expected Content-Type is {}.",
					responseCode,
					contentType,
					WebAPI.DDNS.v1_2.isReachable.getResponseContentType());
			}
		}
		catch (Exception e)
		{
			setDomainStatus(DomainStatus.LINK_ISSUE);
			logger.error(
				"While testing domain link '{}'.",
				WebAPI.DDNS.v1_1.isReachable.getUrl(),
				e);

			manageTunnel(TunnelStatus.OPEN);
		}

		return domainStatus;
	}






	synchronized DomainStatus getDomainStatus()
	{
		return domainStatus;
	}






	synchronized TunnelStatus getTunnelStatus()
	{
		return tunnelStatus;
	}






	synchronized boolean hasDomainChangedSincePreviousCheck()
	{
		return domainChanged;
	}






	synchronized boolean hasTunnelChangedSincePreviousCheck()
	{
		return tunnelChanged;
	}






	synchronized void invalidate()
	{
		setDomainStatus(null);
	}






	synchronized void markDomainStatusAsRead()
	{
		domainChanged = false;
	}






	synchronized void markTunnelStatusAsRead()
	{
		tunnelChanged = false;
	}






	synchronized DomainStatus registerDomain(String distantWebAccessPort, String distantSSHPort)
	{
		logger.debug("Registering domain on the DDNS server...");

		currentHttpPort = (distantWebAccessPort != null) ? distantWebAccessPort : currentHttpPort;
		currentSshPort = (distantSSHPort != null) ? distantSSHPort : currentSshPort;

		try
		{
			logger.debug("DDNS registration URL = {}", WebAPI.DDNS.v1_2.register.getUrl());
			URL url = WebAPI.DDNS.v1_2.register.getUrl();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			DataOutputStream writer = null;
			BufferedReader reader = null;

			try
			{
				connection.setConnectTimeout(connectionTimeout);
				connection.setReadTimeout(connectionTimeout);
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod("POST");
				connection.setUseCaches(false);

				setAuthorization(connection);

				connection.setRequestProperty("Content-Type", WebAPI.DDNS.v1_2.register
					.getRequestContentType());

				JSONObject data = new JSONObject();
				data.put("http_port", currentHttpPort);
				data.put("ssh_port", currentSshPort);

				writer = new DataOutputStream(connection.getOutputStream());
				writer.write(data.toString().getBytes());
				writer.flush();

				int responseCode = connection.getResponseCode();

				if (responseCode != 200)
				{
					logger.error(
						"Cannot register somain: Error code is {}, message (if any) is:",
						responseCode);

					try
					{
						reader = new BufferedReader(
							new InputStreamReader((InputStream) connection.getErrorStream()));

						String response = null;
						while ((response = reader.readLine()) != null)
						{
							logger.error("  |  {}", response);
						}
					}
					catch (Exception e)
					{
						logger.error("  --> no error message available");
					}

					setDomainStatus(DomainStatus.CANNOT_REGISTER);
				}
				else
				{
					logger.debug("Domain registered, now testing distant access...");

					try
					{
						Thread.sleep(2000); // Leave some time to the DDNS server to apply changes
						check();
					}
					catch (InterruptedException e)
					{}
				}
			}
			finally
			{
				if (writer != null)
				{
					writer.close();
				}

				if (reader != null)
				{
					reader.close();
				}

				connection.disconnect();
			}
		}
		catch (Exception e)
		{
			logger.error("While registering domain: ", e);
			setDomainStatus(null);
		}

		return domainStatus;
	}






	synchronized void stop()
	{
		if (tunnelStatus == TunnelStatus.OPEN)
		{
			manageTunnel(TunnelStatus.CLOSED);
		}
	}






	synchronized void unregisterDomain()
	{
		// TODO: implement unregistration (need server code update)

		setDomainStatus(null);
	}






	/**
	 * @param requestedTunnelStatus
	 *            if true, tunnel must be keep opened. If false, tunnel must be closed
	 */
	private void manageTunnel(TunnelStatus requestedTunnelStatus)
	{
		logger.debug(
			"Entering manageTunnel(): request={}, state=",
			requestedTunnelStatus,
			tunnelStatus);

		Runtime rt = Runtime.getRuntime();

		if ((requestedTunnelStatus == TunnelStatus.OPEN)
			&& ((tunnelStatus == TunnelStatus.CLOSED)
				|| tunnelStatus == null))
		{
			// Tunnel have to be opened, and its port registered
			String openTunnelScript = configurationView.getString(
				Property.network_da_openTunnelScript);

			logger.info("Opening tunnels using {}...", openTunnelScript);

			try
			{
				Process proc = rt.exec(openTunnelScript);

				try
				{
					proc.waitFor();
				}
				catch (InterruptedException e1)
				{
					logger.error("DistantAccessManager thread was interrupted "
						+ "while waiting for OpenTunnel script termination.");
				}

				if (proc.exitValue() == 0)
				{
					BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));

					String tunnelDistantPortForSSH = stdInput.readLine();
					String tunnelDistantPortForHTTP = stdInput.readLine();

					if (tunnelDistantPortForSSH != null
						&& tunnelDistantPortForHTTP != null)
					{
						logger.info(
							"Tunnel open on port {} (SSH) and port {} (HTTP).",
							tunnelDistantPortForSSH,
							tunnelDistantPortForHTTP);

						currentTunnelPorts = new JSONObject();
						try
						{
							currentTunnelPorts.put("tunnel_port_ssh", tunnelDistantPortForSSH);
							currentTunnelPorts.put("tunnel_port_http", tunnelDistantPortForHTTP);
						}
						catch (JSONException e)
						{
							logger.error("Cannot create the tunnel ports JSON object.");
						}

						setTunnelStatus(TunnelStatus.OPEN);
					}
					else
					{
						logger.error("Tunnel opening script did not return 2 port numbers.");
					}
				}
				else
				{
					logger.error("Tunnel opening script failed, see errors below:");

					BufferedReader stdError = new BufferedReader(
						new InputStreamReader(proc.getErrorStream()));

					String output = null;
					while ((output = stdError.readLine()) != null)
					{
						logger.error(output);

					}

					String tunnelOutputFile = configurationView.getString(
						Property.network_da_tunnelOutputFile);

					List<String> logs = Files.readAllLines(
						Paths.get(tunnelOutputFile),
						Charset.forName("UTF-8"));

					for (String log : logs)
					{
						logger.error(log);
					}
				}
			}
			catch (IOException e)
			{
				logger.error("While opening tunnel using {}.", openTunnelScript, e);
			}
		}
		else if (requestedTunnelStatus == TunnelStatus.OPEN
			&& tunnelStatus == TunnelStatus.OPEN)
		{
			// Tunnel have to be checked. If not ok, tunnel have to be reopened, and new tunnel
			// port re-registered

			TunnelStatus actualTunnelStatus = checkTunnelStatus();
			if (actualTunnelStatus == TunnelStatus.CLOSED
				|| actualTunnelStatus == null)
			{
				logger.error("Trying to open new tunnels...");

				setTunnelStatus(TunnelStatus.CLOSED);
				currentTunnelPorts = null;
				manageTunnel(TunnelStatus.OPEN);
			}
		}
		else if (requestedTunnelStatus == TunnelStatus.CLOSED
			&& tunnelStatus == TunnelStatus.OPEN)
		{
			// Tunnel have to be closed, and its closure must be reported
			String closeTunnelScript = configurationView.getString(
				Property.network_da_closeTunnelScript);

			logger.info("Closing tunnel using {}...", closeTunnelScript);

			try
			{
				Process proc = rt.exec(closeTunnelScript);

				try
				{
					proc.waitFor();
				}
				catch (InterruptedException e1)
				{
					logger.error("DistantAccessManager thread was interrupted "
						+ "while waiting for CloseTunnel script termination.");
				}

				if (proc.exitValue() == 0)
				{
					logger.info("Closing tunnel: OK");
				}
				else
				{
					logger.error("Tunnel closing script failed, see errors below:");

					BufferedReader stdError = new BufferedReader(
						new InputStreamReader(proc.getErrorStream()));

					String output = null;
					while ((output = stdError.readLine()) != null)
					{
						logger.error(output);

					}
				}

				setTunnelStatus(TunnelStatus.CLOSED);
			}
			catch (IOException e)
			{
				logger.error("While closing tunnel using {}.", closeTunnelScript, e);

				setTunnelStatus(TunnelStatus.CLOSED);
			}
		}

		logger.debug(
			"Exiting manageTunnel(): request={}, state={}.",
			requestedTunnelStatus,
			tunnelStatus);
	}






	private void setDomainStatus(DomainStatus domainStatus)
	{
		if (this.domainStatus != domainStatus)
		{
			logger.info("DOMAIN status changed to {}.", domainStatus);
			this.domainStatus = domainStatus;
			domainChanged = true;
		}
		else
		{
			domainChanged = false;
		}
	}






	private void setTunnelStatus(TunnelStatus tunnelStatus)
	{
		if (this.tunnelStatus != tunnelStatus)
		{
			logger.info("TUNNEL status changed to {}.", tunnelStatus);

			this.tunnelStatus = tunnelStatus;
			tunnelChanged = true;

			if (this.tunnelStatus == TunnelStatus.CLOSED)
			{
				currentTunnelPorts = null;
				deleteTunnelInfo();
			}
			else
			{
				updateTunnelInfo();
			}
		}
		else
		{
			tunnelChanged = false;
		}
	}






	private void updateTunnelInfo()
	{
		logger.debug("Updating tunnel info on the DDNS server...");

		try
		{
			logger.debug("DDNS tunnel info URL = {}.", WebAPI.DDNS.v1_2.tunnel.getUrl());
			URL url = WebAPI.DDNS.v1_2.tunnel.getUrl();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			DataOutputStream writer = null;
			BufferedReader reader = null;

			try
			{
				connection.setConnectTimeout(connectionTimeout);
				connection.setReadTimeout(connectionTimeout);
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod("POST");
				connection.setUseCaches(false);

				setAuthorization(connection);

				connection.setRequestProperty("Content-Type", WebAPI.DDNS.v1_2.tunnel
					.getRequestContentType());

				writer = new DataOutputStream(connection.getOutputStream());
				writer.write(currentTunnelPorts.toString().getBytes());
				writer.flush();

				int responseCode = connection.getResponseCode();

				if (responseCode != 200)
				{
					logger.error(
						"Cannot update tunnel info: Error code is {}, message (if any) is:",
						responseCode);

					try
					{
						reader = new BufferedReader(
							new InputStreamReader((InputStream) connection.getErrorStream()));

						String response = null;
						while ((response = reader.readLine()) != null)
						{
							logger.error("  |  {}", response);
						}
					}
					catch (Exception e)
					{
						logger.error("  --> no error message available");
					}
				}
				else
				{
					logger.debug("Tunnel info updated on DDNS server");
				}
			}
			finally
			{
				if (writer != null)
				{
					writer.close();
				}

				if (reader != null)
				{
					reader.close();
				}

				connection.disconnect();
			}
		}
		catch (Exception e)
		{
			logger.error("While updating tunnel info: ", e);
		}
	}






	private void deleteTunnelInfo()
	{
		logger.debug("Delete tunnel info on the DDNS server...");

		try
		{
			logger.debug("DDNS tunnel info URL = {}.", WebAPI.DDNS.v1_2.tunnel.getUrl());
			URL url = WebAPI.DDNS.v1_2.tunnel.getUrl();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			DataOutputStream writer = null;
			BufferedReader reader = null;

			try
			{
				connection.setConnectTimeout(connectionTimeout);
				connection.setReadTimeout(connectionTimeout);
				connection.setDoInput(true);
				connection.setRequestMethod("DELETE");
				connection.setUseCaches(false);

				setAuthorization(connection);

				int responseCode = connection.getResponseCode();

				if (responseCode != 200)
				{
					logger.error(
						"Cannot delete tunnel info: Error code is {}, message (if any) is:",
						responseCode);

					try
					{
						reader = new BufferedReader(
							new InputStreamReader((InputStream) connection.getErrorStream()));

						String response = null;
						while ((response = reader.readLine()) != null)
						{
							logger.error("  | {}", response);
						}
					}
					catch (Exception e)
					{
						logger.error("  --> no error message available");
					}
				}
				else
				{
					logger.debug("Tunnel info deleted on DDNS server");
				}
			}
			finally
			{
				if (writer != null)
				{
					writer.close();
				}

				if (reader != null)
				{
					reader.close();
				}

				connection.disconnect();
			}
		}
		catch (Exception e)
		{
			logger.error("While deleting tunnel info: ", e);
		}
	}






	private TunnelStatus checkTunnelStatus()
	{
		String checkTunnelScript = configurationView.getString(
			Property.network_da_checkTunnelScript);

		logger.debug("Checking tunnels using {}...", checkTunnelScript);

		Runtime rt = Runtime.getRuntime();

		try
		{
			Process proc = rt.exec(checkTunnelScript);

			try
			{
				proc.waitFor();
			}
			catch (InterruptedException e1)
			{
				logger.error("DistantAccessManager thread was interrupted "
					+ "while waiting for CheckTunnel script termination.");
			}

			if (logger.isDebugEnabled())
			{
				BufferedReader reader = new BufferedReader(
					new InputStreamReader((InputStream) proc.getInputStream()));

				String response = null;
				while ((response = reader.readLine()) != null)
				{
					logger.debug(" | checkTunnelScript :  {}", response);
				}
			}

			if (proc.exitValue() == 0)
			{
				logger.info("Checking tunnels: OK");
				return TunnelStatus.OPEN;
			}
			else
			{
				logger.info("No open tunnels.");
				return TunnelStatus.CLOSED;
			}
		}
		catch (IOException e)
		{
			logger.error("While checking tunnels using {}.", checkTunnelScript, e);

			return null;
		}
	}






	private void setAuthorization(HttpURLConnection connection)
	{
		String credentials = configurationView.getString(
			ConfigurationManager.Property.customerGatewayLogin)
			+ ":" + configurationView.getString(
				ConfigurationManager.Property.customerGatewayPassword);

		logger.debug("Setting connection auth header with: '{}'", credentials);

		connection.setRequestProperty(
			"Authorization",
			"Basic " + new String(Base64.encode(credentials.getBytes())));
	}
}
