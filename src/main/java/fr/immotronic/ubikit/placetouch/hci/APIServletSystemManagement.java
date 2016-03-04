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

package fr.immotronic.ubikit.placetouch.hci;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.system.SystemInspectionService;
import org.ubikit.system.SystemProperties;
import org.ubikit.tools.http.WebApiCommons;

//import fr.immotronic.backoffice.gatewaymanagerservice.CustomerAccountInformation;
//import fr.immotronic.backoffice.gatewaymanagerservice.GatewayAPIConnector;
//import fr.immotronic.license.LicenseManager;
//import fr.immotronic.license.LicenseName;
//import fr.immotronic.license.impl.LicenseManagerImpl;
import fr.immotronic.ubikit.placetouch.auth.impl.AuthController;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.DomainStatus;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.WideAreaNetworkStatus;
import fr.immotronic.ubikit.placetouch.cloud.UpgradeManager;
import org.ubikit.ConfigurationManager;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;

import org.ubikit.ConfigurationView;





//import fr.immotronic.ubikit.placetouch.impl.LC;
import fr.immotronic.ubikit.placetouch.impl.WiFiConnectionUtils;
import fr.immotronic.ubikit.placetouch.license.LicenseInstaller;

//import org.ubikit.Logger;

public final class APIServletSystemManagement extends HttpServlet  
{
	private static final long serialVersionUID = -6349355126159130857L;
	private static final String apiSupportedVersions = "[ \"1.0\" ]";
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	final Logger logger = LoggerFactory.getLogger(APIServletSystemManagement.class);
	
	private final ConfigurationView configurationView;
	private final SystemInspectionService ubikitInspectionService;
	private final NetworkManager networkManager;
	private final String placetouchVersionNumber;
	private final String placetouchBuildDate;
	private final AuthController authController;
	//private final LicenseManagerImpl licenseManager;
	//private final LicenseInstaller licenseInstaller;
	//private final UpgradeManager upgradeManager;
	//private final GatewayAPIConnector gatewayManagerAPIConnector;
	private final boolean isConfiguredForEmbeddedSystem;
	
	public APIServletSystemManagement(	SystemInspectionService ubikitInspectionService, 
										NetworkManager networkManager,
										ConfigurationManager configurationManager,
										String placetouchVersionNumber,
										String placetouchBuildDate,
										AuthController authController,
										/*LicenseManagerImpl licenseManager,
										LicenseInstaller licenseInstaller,
										UpgradeManager upgradeManager,
										/*GatewayAPIConnector gatewayManagerAPIConnector,*/
										boolean isConfiguredForEmbeddedSystem)
	{
		this.ubikitInspectionService = ubikitInspectionService;
		this.networkManager = networkManager;
		this.placetouchVersionNumber = placetouchVersionNumber;
		this.placetouchBuildDate = placetouchBuildDate;
		this.authController = authController;
		//this.licenseManager = licenseManager;
		//this.licenseInstaller = licenseInstaller;
		//this.upgradeManager = upgradeManager;
		//this.gatewayManagerAPIConnector = gatewayManagerAPIConnector;
		this.isConfiguredForEmbeddedSystem = isConfiguredForEmbeddedSystem;
		
		ConfigurationView configurationView = null;
		try
		{
			configurationView = configurationManager.createView(null, getClass().getSimpleName());
		}
		catch (ConfigurationException e)
		{
			logger.error("THIS SHOULD NEVER HAPPEN, because first argument of createView is null");
		}
		
		this.configurationView = configurationView;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String[] pathInfo = req.getPathInfo().split("/"); // Expected path info is /command/command_param_1/command_param_2/...etc. 	
		// Notes: if the request is valid (e.g. looks like /command/command_params), 
		//		pathInfo[0] contains an empty string,
		//		pathInfo[1] contains "command",
		//		pathInfo[2] and next each contains a command parameter. Command parameters are "/"-separated.
	
		WideAreaNetworkStatus internetStatus = networkManager.getInternetStatus();
		DomainStatus domainStatus = networkManager.getDomainStatus();
		
		StringBuilder hostIPAddresses = new StringBuilder();
		
		String eth0IP = networkManager.getIPAddressForEthernet();
		if(eth0IP != null) {
			hostIPAddresses.append("Ethernet: ").append(eth0IP).append("<br>");
		}
		
		String wlanIP = networkManager.getIPAddressForWiFi();
		if(wlanIP != null) {
			hostIPAddresses.append("Wi-Fi: ").append(wlanIP);
		}
		
		if(pathInfo != null && pathInfo.length > 1)
		{
			if(pathInfo[1].equals("version"))
			{
				resp.addHeader("Content-Type", "application/vnd.immotronic.general.version-v1+json;charset=utf8");
				resp.getWriter().write(apiSupportedVersions);
			}
			if(pathInfo[1].equals("network"))
			{
				resp.addHeader("Content-Type", "application/vnd.immotronic.placetouch.network-v1+json;charset=utf8");
				SystemProperties sp = ubikitInspectionService.getSystemProperties();
				
				JSONObject o = new JSONObject();
				
				try 
				{
					o.put("hostname", sp.getSystemHostname());
					o.put("hostaddresses", hostIPAddresses.toString());
					o.put("ethernetAddresses", sp.getSystemEthernetAddress());
					o.put("internet", (internetStatus != null?internetStatus.name():"UNKNOWN"));
					o.put("domainValidity", (domainStatus != null?domainStatus.name():"UNKNOWN"));
					o.put("nat", networkManager.getNATStatus());
					o.put("routerReferences", networkManager.getUPnPGatewayReferences());
					o.put("externalIP", networkManager.getExternalIPAddress());
					resp.getWriter().write(o.toString());
				}
				catch (JSONException e) {
					logger.error("doGet(/properties): While building the response object.", e);
					resp.setStatus(500);
				}
			}
			else if(pathInfo[1].equals("properties"))
			{
				SystemProperties sp = ubikitInspectionService.getSystemProperties();
				
				JSONObject o = new JSONObject();
				
				try 
				{
					o.put("placetouchVersion", placetouchVersionNumber);
					o.put("buildDate", placetouchBuildDate);
					o.put("ubikitVersion", sp.getSystemVersion());
					o.put("hostname", sp.getSystemHostname());
					o.put("hostaddresses", hostIPAddresses.toString());
					o.put("ethernetAddresses", sp.getSystemEthernetAddress());
					o.put("internet", (internetStatus != null?internetStatus.name():"UNKNOWN"));
					o.put("uptime", sp.getSystemUptime());
					o.put("domainValidity", (domainStatus != null?domainStatus.name():"UNKNOWN"));
					o.put("nat", networkManager.getNATStatus());
					o.put("routerReferences", networkManager.getUPnPGatewayReferences());
					o.put("externalIP", networkManager.getExternalIPAddress());
					o.put("licenseUID", /*licenseManager.getInstalledLicence()*/"");
					o.put("licenseType", /*licenseManager.getInstalledLicenseName().toString()*/"NONE");
					
					resp.addHeader("Content-Type", "application/json");
					resp.getWriter().write(WebApiCommons.okMessage(o));
				}
				catch (JSONException e) {
					logger.error("doGet(/properties): While building the response object.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
				}
			}
			else if(pathInfo[1].equals("configuration"))
			{
				JSONObject o = new JSONObject();
				try 
				{
					Property[] properties = Property.values();
					
					for(Property p : properties)
					{
						if(p != Property.ui_password)
						{
							o.put(p.name(), configurationView.get(p));
						}
					}
					
					o.put("username", authController.getUsername());
					
					
					resp.addHeader("Content-Type", "application/json");
					resp.getWriter().write(WebApiCommons.okMessage(o));
				} 
				catch (JSONException e) 
				{
					logger.error("doGet(/configuration): While building the response object.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
				}
			}
			else if(pathInfo[1].equals("appstoreURL"))
			{
				JSONObject o = new JSONObject();
				
				try 
				{
					o.put("appstoreURL", configurationView.getString(Property.webstoreURL));
					resp.addHeader("Content-Type", "application/json");
					resp.getWriter().write(WebApiCommons.okMessage(o));
				}
				catch (JSONException e) {
					logger.error("doGet(/appstoreURL): While building the response object.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
				}
			}
			else if(pathInfo[1].equals("upgrade"))
			{
				JSONObject o = new JSONObject();
				
				try 
				{
					UpgradeManager.State currentUpgradeManagerState = /*upgradeManager.getState()*/UpgradeManager.State.NOTHING_TO_DO;
					o.put("state", currentUpgradeManagerState);
					/*
					if(currentUpgradeManagerState == UpgradeManager.State.UPGRADE_AVAILABLE)
					{
						o.put("version", upgradeManager.getAvailableUpgradeVersion());
					}
					else if(currentUpgradeManagerState == UpgradeManager.State.DOWNLOADING)
					{
						o.put("progress", upgradeManager.getDownloadCompleteness());
					}
					else {
						o.put("lastError", upgradeManager.getLastError());
					}
					*/
					resp.addHeader("Content-Type", "application/json");
					resp.getWriter().write(WebApiCommons.okMessage(o));
				}
				catch (JSONException e) {
					logger.error("doGet(/appstoreURL): While building the response object.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
				}
				
				
			}
			else if(pathInfo[1].equals("wifi"))
			{				
				try 
				{
					if (isConfiguredForEmbeddedSystem)
					{
						JSONObject config = WiFiConnectionUtils.getWiFiConfiguration();
						if (config == null) {
							config = new JSONObject();
						}
						
						config.put("isUp", WiFiConnectionUtils.getWiFiConnectionState());
						config.put("allowed", true);
						resp.addHeader("Content-Type", "application/json");
						resp.getWriter().write(WebApiCommons.okMessage(config));
					}
					else
					{
						JSONObject o = new JSONObject();
						o.put("allowed", false);
						resp.addHeader("Content-Type", "application/json");
						resp.getWriter().write(WebApiCommons.okMessage(o));
					}
				}
				catch (JSONException e) {
					logger.error("doGet(/wifi): While building the response object.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
				}
			}
			else {
				resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
			}
		}
		else {
			resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String pathInfo = req.getPathInfo();
		
		logger.debug("doPost(): pathInfo="+pathInfo);
		
		if(pathInfo != null && !pathInfo.equals("/"))
		{
			if(pathInfo.equals("/configuration"))
			{	
				Enumeration<?> parameters = req.getParameterNames();
				JSONObject data = new JSONObject();
				
				try 
				{
					while(parameters.hasMoreElements())
					{
						String key = (String) parameters.nextElement();
						String value = req.getParameter(key);
						
						if(value != null) {
							if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
								data.put(key, Boolean.getBoolean(value));
							}
							
							try { 
								data.put(key, Integer.parseInt(value)); 
							}
							catch(NumberFormatException e) {
								data.put(key, value);
							}
						}
					}
					
					setConfigurationData(data);
					
					resp.getWriter().write(WebApiCommons.okMessage(null));
					return;
				}
				catch(JSONException | IllegalArgumentException | ConfigurationException e) {
					logger.error("doPost(/configuration): updating the configuration with data from UI.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo.equals("/license"))
			{
				String licenseUID = req.getParameter("licenseUID");
				
				/*LicenseName licenseType = licenseInstaller.setLicense(licenseUID);
				CustomerAccountInformation customerAccountInformation = null;
				
				if(licenseType != null)
				{
					if(licenseType != LicenseName.INVALID && licenseType != LicenseName.NONE)
					{
						customerAccountInformation = 
							gatewayManagerAPIConnector.registerGateway(authController.getHwid(), licenseUID);
						
						if(customerAccountInformation != null)
						{
							gatewayManagerAPIConnector.setCredentials(customerAccountInformation.getGatewayLogin(), customerAccountInformation.getGatewayPassword());
							setCustomerAccountInformation(
								customerAccountInformation);
						}
					}
					
					JSONObject o = new JSONObject();
					try 
					{
						o.put("licenseType", licenseType.toString());
						o.put("appstoreURL", licenseManager.getWebstoreURL());
						if(customerAccountInformation != null)
						{
							o.put("customerName", customerAccountInformation.getCustomerName());
							o.put("customerSiteName", customerAccountInformation.getSiteName());
							o.put("customerGatewayId", customerAccountInformation.getGatewayId());
						}
						resp.getWriter().write(WebApiCommons.okMessage(o));
						return;
					}
					catch (JSONException e) 
					{
						logger.error("doPost(/license): While building the configuration JSON object.", e);
					}

				}
								
				String reason = licenseManager.getErrorMessage();
				if(licenseManager.getErrorMessage().equals(LicenseManager.ErrorMessage.UNABLE_TO_DOWNLOAD_LICENSE_FILE.toString()))
				{
					reason = licenseManager.getServerErrorMessage();
				}
				
				resp.getWriter().write(WebApiCommons.errorMessage(reason, WebApiCommons.Errors.internal_error));
				return;*/
				JSONObject o = new JSONObject();
				try
				{
					o.put("licenseType", "NONE");
					o.put("appstoreURL", configurationView.getString(Property.webstoreURL));

					resp.getWriter().write(WebApiCommons.okMessage(o));
					return;
				}
				catch (JSONException e)
				{
					logger.error("doPost(/license): While building the configuration JSON object.", e);
				}
			}
			else if(pathInfo.equals("/credentials"))
			{
				String contentType = req.getContentType();
				if(contentType != null && contentType.equalsIgnoreCase("application/vnd.immotronic.general.credential-update-v1;charset=utf-8"))
				{
					StringBuffer content = new StringBuffer();
					try 
					{
						String line = null;
						BufferedReader reader = req.getReader();
						while ((line = reader.readLine()) != null)
						{
							content.append(line);
						}
					
						JSONObject data = new JSONObject(content.toString());
					
						String oldPassword = data.getString("old_password");
						String newUsername = data.getString("new_username");
						String newPassword = data.getString("new_password");
					
						if(	oldPassword == null || oldPassword.isEmpty() || newUsername == null || newUsername.isEmpty() || 
							newPassword == null || newPassword.length() < 8)
						{
							resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								resp.getWriter().write("Invalid content : no username or new password too short.");
						}
						else
						{
							if(authController.checkCredentials(authController.getUsername(), oldPassword))
							{
								if(authController.updateCredentials(newUsername, newPassword))
								{
									resp.setStatus(HttpServletResponse.SC_OK);
								}
							}
							else
							{
								if(authController.checkCredentials(newUsername, newPassword))
								{
									resp.setStatus(HttpServletResponse.SC_OK);
								}
								else
								{
									authController.requireHttpAuthentification(resp);
								}
							}
						}
					}
					catch(JSONException e)
					{
						logger.error("doPost(/credentials): Invalid POST content: not a JSON object", e);
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						resp.getWriter().write("Cannot get posted content.");
					}
					catch (Exception e) 
					{
						logger.error("doPost(/credentials): While getting the POST content", e);
						resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						resp.getWriter().write("Cannot get posted content.");
					}
					
					return;
				}
				
				logger.warn("doPost(/credentials): Unsupported Content-Type. Expecting application/vnd.immotronic.general.credential-update-v1;charset=utf-8, but getting {}", contentType);
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.getWriter().write("Unsupported Content-Type. Expecting application/vnd.immotronic.general.credential-update-v1;charset=utf-8, but getting "+contentType);
			}
			/*else if(pathInfo.equals("/upgrade-download"))
			{
				String contentType = req.getContentType();
				if(contentType != null && contentType.equalsIgnoreCase("application/vnd.immotronic.general.upgrade-download-v1;charset=utf-8"))
				{
					try 
					{
						String content = null;
						BufferedReader reader = req.getReader();
						content = reader.readLine(); // content is a single line of text
						
						if(upgradeManager.downloadUpgrade(content)) // launch the  upgrade download.
						{
							// If launching was successful...
							resp.setStatus(HttpServletResponse.SC_OK);
						}
						else
						{
							// Launching was NOT successful.
							UpgradeManager.Error error = upgradeManager.getLastError();
							logger.debug("upgradeManager.downloadUpgrade() returned false. Error was {}", error);
							switch(error)
							{
								case CANNOT_CREATE_LOCAL_FILES:
									resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									resp.getWriter().write("CANNOT_CREATE_LOCAL_FILES");
									break;
								case NETWORK_ERROR:
									resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
									resp.getWriter().write("NETWORK_ERROR");
									break;
								default:
									resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									resp.getWriter().write("UNHANDLED_ERROR");
									break;
							}
						}
					}
					catch (Exception e) 
					{
						logger.error("doPost(/upgrade-download): While getting the POST content", e);
						resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						resp.getWriter().write("Cannot get posted content.");
					}
					
					return;  
				}
				
				logger.warn("doPost(/downloadUpgrade): Unsupported Content-Type. Expecting application/vnd.immotronic.general.upgrade-download-v1;charset=utf-8, but getting {}", contentType);
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.getWriter().write("Unsupported Content-Type. Expecting application/vnd.immotronic.general.upgrade-download-v1;charset=utf-8, but getting "+contentType);
			}
			else if(pathInfo.equals("/upgrade-install"))
			{
				if(upgradeManager.installUpgrade()) // launch the  upgrade installation.
				{
					// If installation was successful...
					resp.setStatus(HttpServletResponse.SC_OK);
				}
				else
				{
					// Installation was NOT successful
					UpgradeManager.Error error = upgradeManager.getLastError();
					logger.debug("installUpgrade() returned false. Error was {}", error);
					switch(error)
					{
						case CANNOT_CREATE_LOCAL_FILES:
							resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							resp.getWriter().write("CANNOT_CREATE_LOCAL_FILES");
							break;
						case UPGRADE_EXTRACTION_FAILED:
							resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							resp.getWriter().write("UPGRADE_EXTRACTION_FAILED");
							break;
						case UPGRADE_SERVER_ERROR:
							resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							resp.getWriter().write("UPGRADE_SERVER_ERROR");
							break;
							
						default:
							resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							resp.getWriter().write("UNEXPECTED_ERROR");
							break;
					}
				}
			}
			else if(pathInfo.equals("/upgrade-reboot"))
			{
				if(upgradeManager.reboot()) // launch the  upgrade installation.
				{
					// If installation was successful...
					resp.setStatus(HttpServletResponse.SC_OK);
				}
				else
				{
					// Installation was NOT successful
					UpgradeManager.Error error = upgradeManager.getLastError();
					logger.debug("reboot() returned false. Error was {}", error);
				}
			}
			else if(pathInfo.equals("/general-shutdown"))
			{
				if(upgradeManager.shutdown(false)) // shutdown the whole system
			    {
			    	// If shutdown is on going...
			    	resp.setStatus(HttpServletResponse.SC_OK);
			    }
			    else
			    {
			    	// Installation was NOT successful
			    	UpgradeManager.Error error = upgradeManager.getLastError();
			    	logger.debug("shutdown(false) returned false. Error was {}", error);
			    }
			}
			else if(pathInfo.equals("/general-reboot"))
			{
				if(upgradeManager.shutdown(true)) // reboot the whole system
			    {
			    	// If shutdown is on going...
			    	resp.setStatus(HttpServletResponse.SC_OK);
			    }
			    else
			    {
			    	// Installation was NOT successful
			    	UpgradeManager.Error error = upgradeManager.getLastError();
			    	logger.debug("shutdown(true) returned false. Error was {}", error);
			    }
			}*/
			else if(pathInfo.equals("/network"))
			{
				String contentType = req.getContentType();
				if(contentType != null && contentType.equalsIgnoreCase("application/vnd.immotronic.placetouch.networkpreferences-v1;charset=utf-8"))
				{
					StringBuffer content = new StringBuffer();
					try 
					{
						String line = null;
						BufferedReader reader = req.getReader();
						while ((line = reader.readLine()) != null)
						{
							content.append(line);
						}
					
						JSONObject data = new JSONObject(content.toString());
						JSONObject config = new JSONObject();
						
						if(data.has("natAutomaticManagement"))
						{
							config.put("natAutomaticManagement", data.getBoolean("natAutomaticManagement"));
						}
						
						if(data.has("distantAccess"))
						{
							config.put("isDistantAccessAllowed", data.getBoolean("distantAccess"));
						}
						
						setConfigurationData(config);
						resp.setStatus(HttpServletResponse.SC_OK);
					}
					catch(JSONException e)
					{
						logger.error("doPost(/network): Invalid POST content: not a JSON object", e);
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						resp.getWriter().write("Cannot parse content, invalid data.");
					}
					catch (Exception e) 
					{
						logger.error("doPost(/network): While getting the POST content", e);
						resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						resp.getWriter().write("Cannot get posted content.");
					}
					
					return;
				}
				
				logger.warn("doPost(/network): Unsupported Content-Type. Expecting application/vnd.immotronic.placetouch.networkpreferences-v1;charset=utf-8, but getting {}", contentType);
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.getWriter().write("Unsupported Content-Type. Expecting application/vnd.immotronic.placetouch.networkpreferences-v1;charset=utf-8, but getting "+contentType);
			}
			else if (pathInfo.equals("/time"))
			{
				String reqDate = req.getParameter("date");
				if (reqDate != null && isConfiguredForEmbeddedSystem)
				{
					try
					{
						// We check request parameter format.
						dateFormat.parse(reqDate);
					} 
					catch (ParseException e)
					{
						logger.error("doPost(/network): Invalid POST content: Parsing failed.", e);
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						resp.getWriter().write("Cannot parse content, invalid data.");
						return;
					}
					
					ProcessBuilder pb = new ProcessBuilder();
					pb.command("timedatectl", "set-time", "\""+reqDate+"\"");
					pb.start();
					
					resp.getWriter().write(WebApiCommons.okMessage(null));
					return;
				}
			}
			else if (pathInfo.equals("/wifi"))
			{
				String contentType = req.getContentType();
				if(contentType != null && contentType.equalsIgnoreCase("application/vnd.immotronic.placetouch.wificonfig-v1;charset=utf-8"))
				{
					if (!isConfiguredForEmbeddedSystem)
					{
						resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
						return;
					}
					
					StringBuffer content = new StringBuffer();
					try 
					{
						String line = null;
						BufferedReader reader = req.getReader();
						while ((line = reader.readLine()) != null)
						{
							content.append(line);
						}
					
						JSONObject data = new JSONObject(content.toString());
					
						if (data.has("remove"))
						{
							WiFiConnectionUtils.removeWiFiConnectionFile();
							resp.setStatus(HttpServletResponse.SC_OK);
						}
						else
						{
							String wifiSecurity = data.getString("security");
							String SSID = data.getString("SSID");
							String passphrase = data.getString("passphrase");
							boolean SSIDIsHidden = data.getBoolean("hidden");
						
							if (WiFiConnectionUtils.configureWiFiConnection(WiFiConnectionUtils.WifiSecurity.valueOf(wifiSecurity), SSID, passphrase, SSIDIsHidden)) {
								resp.setStatus(HttpServletResponse.SC_OK);
							}
							else
							{
								WiFiConnectionUtils.Error error = WiFiConnectionUtils.getLastError();
								logger.debug("configureWiFiConnection() returned false. Error was {}", error);
								switch(error)
								{
									case BAD_CONFGURATION_PARAMETERS:
										resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										resp.getWriter().write("BAD_CONFGURATION_PARAMETERS");
										break;
									case INTERNAL_ERROR:
										resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
										resp.getWriter().write("SETTING_CONFIGURATION_FAILED");
										break;
								}
							}
						}
					}
					catch(JSONException e)
					{
						logger.error("doPost(/wifi): Invalid POST content: not a JSON object", e);
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						resp.getWriter().write("Cannot parse content, invalid data.");
					}
					catch(IllegalArgumentException e)
					{
						logger.error("doPost(/wifi): Invalid POST content: security field not valid", e);
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						resp.getWriter().write("Cannot parse content, invalid data.");
					}
					catch (Exception e) 
					{
						logger.error("doPost(/wifi): While getting the POST content", e);
						resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						resp.getWriter().write("Cannot get posted content.");
					}
					return;
				}
				
				logger.warn("doPost(/wifi): Unsupported Content-Type. Expecting application/vnd.immotronic.placetouch.wificonfig-v1;charset=utf-8, but getting {}", contentType);
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.getWriter().write("Unsupported Content-Type. Expecting application/vnd.immotronic.placetouch.wificonfig-v1;charset=utf-8, but getting "+contentType);
			}
		}
		
		resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
	}
	
	private void setConfigurationData(JSONObject data) throws ConfigurationException
	{
		if (data != null)
		{
			Map<Enum<?>, Object> newConfiguration = new HashMap<Enum<?>, Object>();

			Iterator<?> it = data.keys();
			while (it.hasNext())
			{
				String key = (String) it.next();

				try
				{
					String value = data.getString(key);
					Property property = Property.valueOf(key);
					newConfiguration.put(property, value);
				}
				catch (IllegalArgumentException e)
				{
					continue; // if an entry of JSON object is NOT a configuration item, this
								// entry is ignored.
				}
				catch (JSONException e)
				{
					logger.error("setConfigurationData(): Illegal Argument", e);
					throw new IllegalArgumentException(key
						+ " is not a valid configuration data item.");
				}
			}

			configurationView.set(newConfiguration);
		}
	}





/*
	private void
		setCustomerAccountInformation(CustomerAccountInformation customerAccountInformation)
	{
		Map<Enum<?>, Object> newConfiguration = new HashMap<Enum<?>, Object>();

		newConfiguration.put(Property.customerId, customerAccountInformation.getCustomerId());
		newConfiguration.put(Property.customerName, customerAccountInformation.getCustomerName());
		newConfiguration.put(Property.customerSiteId, customerAccountInformation.getSiteId());
		newConfiguration.put(Property.customerSiteName, customerAccountInformation.getSiteName());
		newConfiguration.put(Property.customerGatewayId, customerAccountInformation.getGatewayId());
		newConfiguration.put(Property.customerGatewayLogin, customerAccountInformation
			.getGatewayLogin());
		newConfiguration.put(Property.customerGatewayPassword, customerAccountInformation
			.getGatewayPassword());

		try
		{
			configurationView.set(newConfiguration);
		}
		catch (ConfigurationException e)
		{
			logger.error("This error should never happen. BUG FIX NEEDED.", e);
		}
	}*/
}
