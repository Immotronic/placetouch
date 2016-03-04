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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.PhysicalEnvironmentModelInformations;
import org.ubikit.service.PhysicalEnvironmentModelService;
import org.ubikit.tools.http.WebApiCommons;

import fr.immotronic.ubikit.placetouch.impl.DiagnosticLogger;
import fr.immotronic.ubikit.placetouch.impl.LC;
import fr.immotronic.ubikit.placetouch.system.SystemEvent;
import fr.immotronic.ubikit.placetouch.system.SystemEventObserver;

import org.ubikit.Logger;

public class APIServletDiagnostics extends HttpServlet 
{	
	private static final long serialVersionUID = 6070567262619833779L;
	private static final String placetouchLogsBaseURI = "file:///var/log/placetouch."; 
	
	private DiagnosticLogger diagnosticLogger;
	private PhysicalEnvironmentModelService pem;
	private SystemEventObserver systemEventObserver;
	
	public APIServletDiagnostics(PhysicalEnvironmentModelService pem, DiagnosticLogger diagnosticLogger, SystemEventObserver systemEventObserver)
	{
		this.pem = pem;
		this.diagnosticLogger = diagnosticLogger;
		this.systemEventObserver = systemEventObserver;
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
			if(pathInfo[1].equals("diagnostics"))
			{
				PhysicalEnvironmentModelInformations info = pem.getInformations();
				
				JSONObject data = new JSONObject();
				
				try {
					data.put("info", info.toString());
				} 
				catch (JSONException e) {
					Logger.error(LC.gi(), this, "doGet/diagnostics: While building the response object.");
				}
				
				resp.getOutputStream().write(WebApiCommons.okMessage(data).getBytes("UTF8"));
			}
			else if(pathInfo[1].equals("diagnosticLogs"))
			{
				JSONArray logs = new JSONArray();
				String log = diagnosticLogger.take();
				while(log != null) 
				{
					logs.put(log);
					log = diagnosticLogger.poll();
				}
				
				JSONObject data = new JSONObject();
				
				try {
					data.put("logs", logs);
				} 
				catch (JSONException e) {
					Logger.error(LC.gi(), this, "doGet/diagnosticsLogs: While building the response object.");
				}
				
				resp.getOutputStream().write(WebApiCommons.okMessage(data).getBytes("UTF8"));
			}
			else if(pathInfo[1].equals("placetouch-logs"))
			{
				String logFileTermination = "log";
				
				if(pathInfo.length == 3 && pathInfo[2] != null)
				{
					if(pathInfo[2].equals("1")) {
						logFileTermination = "log.1";
					}
					else if(pathInfo[2].equals("2")) {
						logFileTermination = "log.2.gz";
					}
					else if(pathInfo[2].equals("3")) {
						logFileTermination = "log.3.gz";
					}
					else if(pathInfo[2].equals("4")) {
						logFileTermination = "log.4.gz";
					}
					else if(pathInfo[2].equals("5")) {
						logFileTermination = "log.5.gz";
					}
					else if(pathInfo[2].equals("6")) {
						logFileTermination = "log.6.gz";
					}
				}
			
				resp.addHeader("Content-Disposition", "attachment; filename=placetouch."+logFileTermination);
				
				InputStream stream = null;
				try 
				{
					URI logFileURI = new URI(placetouchLogsBaseURI+logFileTermination);
					stream = logFileURI.toURL().openStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(stream, "ISO-8859-1"));
					char[] buffer = new char[4096];
					while(true)
					{
						int nbReadBytes = br.read(buffer);
						if(nbReadBytes == -1) {
							break;
						}
						resp.getWriter().write(buffer, 0, nbReadBytes);
					}
					
					return;
				} 
				catch (Exception e) 
				{
					if(e instanceof java.io.FileNotFoundException) 
					{
						resp.sendError(404);
						Logger.error(LC.gi(), this, "Log file: "+placetouchLogsBaseURI+logFileTermination+" not found.");
					}
					else 
					{
						Logger.error(LC.gi(), this, "Error while getting log file: "+placetouchLogsBaseURI+logFileTermination,e);
						resp.sendError(500, "Error while getting "+placetouchLogsBaseURI+logFileTermination+": "+e.getClass().getCanonicalName()+", "+e.getMessage());
					}
					
					return;
				}
				finally
				{
					resp.getWriter().close();
					if(stream != null) {
						try 
						{
							stream.close();
						} 
						catch (IOException e) 
						{ }
					}
				}
			}
			else if(pathInfo[1].equals("system-logs"))
			{
				resp.addHeader("Content-Disposition", "attachment; filename=system.log");
				try
				{
					if(pathInfo.length == 3 && pathInfo[2] != null)
					{
						StringBuilder result = new StringBuilder();
						ProcessBuilder pb = new ProcessBuilder();
						pb.redirectErrorStream(false);
						
						if(pathInfo[2].equals("d"))
						{
							pb.command("journalctl", "--since=-604800", "--no-pager"); // last 7 days system logs
							
							Process p = pb.start();
							BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
							String line = null;
							while ((line = br.readLine()) != null) {
								result.append(line);
								result.append("\n");
							}
						}
						else if(pathInfo[2].equals("b"))
						{
							result.append("CURRENT BOOT\n\n");
							
							pb.command("journalctl", "-b");
							for (int i = 1; i < 6; i++)
							{
								Process p = pb.start();
								
								BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder buff = new StringBuilder();
								String line = null;
								while ((line = br.readLine()) != null) {
									buff.append(line);
									buff.append("\n");
								}
								
								if (buff.length() != 0)
								{
									if (i != 1)
										result.append("\n-------------\nPREVIOUS BOOT\n\n");
									result.append(buff);
																		
									pb.command("journalctl", "-b", String.valueOf(-i));
								}
								else {
									break;
								}
							}
						}
						
						resp.getWriter().write(result.toString());
					}
				}
				catch(IOException e)
				{
					resp.sendError(500);
					Logger.error(LC.gi(), this, "Unable to read system journal logs.");
					return;
				}
			}
			else if(pathInfo[1].equals("systemEventsLogs"))
			{
				Collection<SystemEvent> systemEvents = systemEventObserver.getSystemEvents(100, SystemEvent.Severity.LOW);
				if (systemEvents != null)
				{
					JSONArray logs = new JSONArray();
					JSONObject data = new JSONObject();
					try
					{
						for (SystemEvent systemEvent : systemEvents)
						{
							JSONObject o = new JSONObject();
							o.put("date", systemEvent.getDate());
							o.put("message", systemEvent.getMessage());
							o.put("severity", systemEvent.getSeverity().getSeverity());
							logs.put(o);
						}
						data.put("logs", logs);
					}
					catch (JSONException e) {
						Logger.error(LC.gi(), this, "doGet/diagnosticsLogs: While building the response object.");
					}
					resp.getOutputStream().write(WebApiCommons.okMessage(data).getBytes("UTF8"));
				}
				else
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
			}
			else {
				resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
			}
		}
		else {
			resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
		}
	}
}