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

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.system.ExtensionExecutionProperties;
import org.ubikit.system.ExtensionManagerService;
import org.ubikit.system.ExtensionProperties;
import org.ubikit.system.SystemInspectionService;
import org.ubikit.system.ExtensionManagerService.ExtensionType;
import org.ubikit.system.ExtensionManagerService.InstallationStatus;
import org.ubikit.tools.http.WebApiCommons;

//import fr.immotronic.backoffice.gatewaymanagerservice.GatewayAPIConnector;
import fr.immotronic.commons.http.tools.HttpServiceConnector;
import fr.immotronic.ubikit.placetouch.config.AppListManager;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;

import org.ubikit.ConfigurationManager;
import org.ubikit.ConfigurationView;

public class APIServletApps extends HttpServlet 
{
	private static final long serialVersionUID = 4566526199597949055L;

	private final SystemInspectionService ubikitInspectionService;
	private final ExtensionManagerService extensionManagerService;
	private final AppListManager appListManager;
	//private final GatewayAPIConnector gatewayManagerAPIConnector;
	private final ConfigurationView configurationView;
	
	final Logger logger = LoggerFactory.getLogger(APIServletApps.class);
	
	public APIServletApps(SystemInspectionService ubikitInspectionService, 	ExtensionManagerService extensionManagerService, AppListManager appListManager, /*GatewayAPIConnector gatewayManagerAPIConnector,*/ ConfigurationManager configurationManager)
	{
		this.ubikitInspectionService = ubikitInspectionService;
		this.extensionManagerService = extensionManagerService;
		this.appListManager = appListManager;
	//	this.gatewayManagerAPIConnector = gatewayManagerAPIConnector;
		
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
		
		if(pathInfo != null && pathInfo.length > 1)
		{
			if(pathInfo[1].equals("public") || pathInfo[1].equals("all"))
			{
				Collection<ExtensionProperties> apps = null;
				if(pathInfo[1].equals("public")) {
					apps = appListManager.getAppsWithPublicUI();
				}
				else {
					apps = appListManager.getAllApps();
				}
				
				JSONArray res = new JSONArray();
				
				for(ExtensionProperties app : apps)
				{	
					ExtensionExecutionProperties appExecutionProperties = ubikitInspectionService.getAppRegistry().getExtensionExecutionProperties(app.getUID());
					JSONObject o = new JSONObject();
				
					try 
					{
						o.put("uid", app.getUID());
						o.put("name", app.getName());
						o.put("version", app.getVersion());
						o.put("status", appExecutionProperties.getRunningStatus().name());
					}
					catch (JSONException e) {
						logger.error("doGet(/public): While building the response object.", e);
					}
					
					res.put(o);
				}
				
				JSONObject o = new JSONObject();
				
				try 
				{
					o.put("apps", res);
					//resp.setContentType("application/json");
					resp.addHeader("Content-Type", "application/json");
					resp.getOutputStream().write(WebApiCommons.okMessage(o).getBytes("UTF8"));
				} 
				catch (JSONException e) {
					logger.error("doGet(/public): While building the response object.", e);
				}
			}
			else if(pathInfo[1].equals("checkAppInstallation"))
			{
				String appUID = pathInfo[2]+"/"+pathInfo[3];
				
				ExtensionProperties appProperties = ubikitInspectionService.getAppRegistry().getExtensionProperties(appUID);
				
				JSONObject o = new JSONObject();
				
				try 
				{
					if(appProperties == null) {
						o.put("appStatus", "notInstalled");
					}
					else {
						o.put("appStatus", "installed");
					}
					
					resp.addHeader("Content-Type", "application/json");
					resp.getWriter().write(WebApiCommons.okMessage(o));
				}
				catch (JSONException e) {
					logger.error("doGet(/checkAppInstallation): While building the response object.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
				}
			}
			else if(pathInfo[1].equals("appProperties"))
			{
				String appUID = pathInfo[2] + "/" + pathInfo[3];
				ExtensionProperties appProperties = ubikitInspectionService.getAppRegistry().getExtensionProperties(appUID);
				ExtensionExecutionProperties appExecutionProperties = ubikitInspectionService.getAppRegistry().getExtensionExecutionProperties(appUID);
				
				ExtensionExecutionProperties.Status appStatus = ExtensionExecutionProperties.Status.PAUSED;
				if(appExecutionProperties != null) {
					appStatus = appExecutionProperties.getRunningStatus();
				}
				
				JSONObject o = new JSONObject();
				
				try 
				{
					o.put("name", appProperties.getName());
					o.put("version", appProperties.getVersion());
					o.put("vendorName", appProperties.getVendor());
					o.put("appStatus", appStatus.name());
					
					switch(appStatus)
					{
						case PAUSED:
							if(appExecutionProperties != null) {
								o.put("downtime", appExecutionProperties.getDowntime());
							}
							else {
								o.put("downtime", -1);
							}
							break;
						case RUNNING:
							o.put("uptime", appExecutionProperties.getUptime());
							break;
					}
					
					resp.addHeader("Content-Type", "application/json");
					resp.getOutputStream().write(WebApiCommons.okMessage(o).getBytes("UTF8"));
				}
				catch (JSONException e) {
					logger.error("doGet(/appProperties): While building the response object.", e);
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
				}
			}
			else if(pathInfo[1].equals("icon"))
			{
				
				resp.addHeader("Content-Type", "image/png");
				
				String appUID = null;
				byte[] iconData = null;
				
				if(pathInfo[2].equals("big")) {
					appUID = pathInfo[3] + "/" + pathInfo[4].replaceFirst("\\.png", "");
					iconData = ubikitInspectionService.getAppRegistry().getExtensionResourceBinaryContent(appUID, "/app-resources/icon_172.png");
				}
				else {
					appUID = pathInfo[2] + "/" + pathInfo[3].replaceFirst("\\.png", "");
					iconData = ubikitInspectionService.getAppRegistry().getExtensionResourceBinaryContent(appUID, "/app-resources/icon_76.png");
				}

				resp.getOutputStream().write(iconData);
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
		if(pathInfo != null && !pathInfo.equals("/"))
		{
			if(pathInfo.equals("/install"))
			{
				String appUrl = req.getParameter("appUrl");
				String appID = req.getParameter("appID"); 

				if(appUrl != null && !appUrl.isEmpty())
				{
					if(extensionManagerService.install(ExtensionType.APP, appUrl, "Immotronic", "http://www.immotronic.fr") == InstallationStatus.OK) 
					{
						/*gatewayManagerAPIConnector.activateApp(appID, configurationView.getInteger(Property.customerGatewayId));
						HttpServiceConnector.Error err = gatewayManagerAPIConnector.getLastError();
						if(err == null)
						{
							resp.getWriter().write(WebApiCommons.okMessage(null));
						}
						else
						{
							resp.getWriter().write(WebApiCommons.errorMessage("Error while activating the application: "+err.toString(), WebApiCommons.Errors.invalid_query));
						}*/
						
					}
					else {
						resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error)); // Do better, because a network error could also .
					}
				}
			}
			else if(pathInfo.equals("/uninstall"))
			{
				String appUID = req.getParameter("appUID");

				logger.trace("Will try to uninstall bundle {}.", appUID);
				
				if(appUID != null && !appUID.isEmpty())
				{
					if(extensionManagerService.uninstall(ExtensionType.APP, appUID)) {
						resp.getWriter().write(WebApiCommons.okMessage(null));
						logger.debug("Bundle uninstalling succeed :-) for bundle {}.",appUID);
					}
					else {
						resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
						logger.debug("Bundle uninstalling failed :-( for bundle {}.",appUID);
					}
				}
			}
			else if(pathInfo.equals("/start"))
			{
				String appUID = req.getParameter("appUID");

				logger.trace("Will try to start bundle {}.",appUID);
				
				if(appUID != null && !appUID.isEmpty())
				{
					if(extensionManagerService.start(ExtensionType.APP, appUID)) {
						resp.getWriter().write(WebApiCommons.okMessage(null));
						logger.debug("Bundle starting succeed :-) for bundle {}.",appUID);
					}
					else {
						resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
						logger.debug("Bundle starting failed :-( for bundle {}.",appUID);
					}
				}
			}
			else if(pathInfo.equals("/stop"))
			{
				String appUID = req.getParameter("appUID");
				
				logger.trace("Will try to stop bundle {}.", appUID);
				
				if(appUID != null && !appUID.isEmpty())
				{
					if(extensionManagerService.stop(ExtensionType.APP, appUID)) {
						resp.getWriter().write(WebApiCommons.okMessage(null));
						logger.debug("Bundle stopping succeed :-) for bundle {}.", appUID);
					}
					else {
						resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
						logger.debug("Bundle stopping failed :-( for bundle {}.", appUID);
					}
				}
			}
			else {
				resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
			}
		}
		else {
			resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
		}
		
		logger.trace("End of doPost().");
	}
}
