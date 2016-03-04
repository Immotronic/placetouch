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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.tools.http.WebApiCommons;

import org.ubikit.Logger;

import fr.immotronic.ubikit.placetouch.impl.LC;
import fr.immotronic.ubikit.placetouch.wsn.WsnManager;
import fr.immotronic.ubikit.placetouch.wsn.WsnManager.AddItemStatus;

public class APIServletWsn  extends HttpServlet  
{
	private static final long serialVersionUID = -657745447491297253L;

	private WsnManager wsnManager;
	
	public APIServletWsn(WsnManager wsnManager)
	{
		if(wsnManager == null) {
			throw new IllegalArgumentException("wsnManager cannot be null");
		}
		
		assert (wsnManager != null);
		this.wsnManager = wsnManager;
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
			if(pathInfo[1].equals("pemBaseURLs"))
			{
				JSONObject o = new JSONObject();
				
				try 
				{
					o.put("urls", wsnManager.getPEMbaseURLs());
				}
				catch (JSONException e) {
					Logger.error(LC.gi(), this, "doGet/pemBaseURLs: While building the response object.");
				}
				
				resp.getWriter().write(WebApiCommons.okMessage(o));
			}
			else if(pathInfo[1].equals("getNewItems"))
			{
				JSONObject o = new JSONObject();
				JSONArray items = new JSONArray();
				for(JSONObject item : wsnManager.getNewItemList())
				{
					items.put(item);
				}
				
				try 
				{
					o.put("items", items);
				}
				catch (JSONException e) {
					Logger.error(LC.gi(), this, "doGet/getNewItems: While building the response object.");
				}
				
				resp.getWriter().write(WebApiCommons.okMessage(o));
			}
			else if(pathInfo[1].equals("getItems"))
			{
				JSONObject o = new JSONObject();
				JSONArray items = new JSONArray();
				for(JSONObject item : wsnManager.getItemList())
				{
					items.put(item);
				}
				
				try 
				{
					o.put("items", items);
				}
				catch (JSONException e) {
					Logger.error(LC.gi(), this, "doGet/getNewItems: While building the response object.");
				}
				
				resp.getOutputStream().write(WebApiCommons.okMessage(o).getBytes("UTF8"));
			}
			else if(pathInfo[1].equals("getItemValue"))
			{
				resp.getWriter().write(WebApiCommons.okMessage(wsnManager.getItemValue(pathInfo[2])));
			}
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String[] pathInfo = req.getPathInfo().split("/"); // Expected path info is /command/command_param_1/command_param_2/...etc. 	
		// Notes: if the request is valid (e.g. looks like /command/command_params), 
		//		pathInfo[0] contains an empty string,
		//		pathInfo[1] contains "command",
		//		pathInfo[2] and next each contains a command parameter. Command parameters are "/"-separated.
		
		if(pathInfo != null && pathInfo.length > 1)
		{
			if(pathInfo[1].equals("enterPairingMode"))
			{
				wsnManager.enterPairingMode();
				resp.getWriter().write(WebApiCommons.okMessage(null));
			}
			else if(pathInfo[1].equals("exitPairingMode"))
			{
				wsnManager.exitPairingMode();
				resp.getWriter().write(WebApiCommons.okMessage(null));
			}
			else if(pathInfo[1].equals("addItem"))
			{
				String itemUID = null;
				String name = null;
				String location = null;
				Map<String, String> customProperties = new HashMap<String, String>();
				List<String> capabilities = new ArrayList<String>();

				for(Object key : req.getParameterMap().keySet())
				{
					String pname = (String)key;
					Logger.debug(LC.gi(), this, pname+"="+req.getParameter(pname));
					if(pname.equals("itemUID")) {
						itemUID = req.getParameter(pname);
					}
					else if(pname.equals("customName")) {
						name = req.getParameter(pname);
					}
					else if(pname.equals("location")) {
						location = req.getParameter(pname);
					}
					else if(pname.contains("customProperties"))
					{
						String cpn = pname.substring(pname.indexOf('[')+1, pname.indexOf(']'));
						customProperties.put(cpn, req.getParameter(pname));
					}
					else if(pname.contains("capabilities"))
					{
						capabilities.add(req.getParameter(pname));
					}
				}
				
				AddItemStatus status = wsnManager.addItem(itemUID, name, location, customProperties, capabilities.toArray(new String[0]));
				Logger.debug(LC.gi(), this, "addItem will respond: success="+status.isSuccessful());
				if(status.isSuccessful())
				{
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				else {
					resp.getWriter().write(WebApiCommons.errorMessage(status.getFailureReason(), status.getErrorCode()));
				}
			}
			else if(pathInfo[1].equals("dropItem"))
			{
				String itemUID = pathInfo[2];
				wsnManager.dropItem(itemUID);
				resp.getWriter().write(WebApiCommons.okMessage(null));
			}
			else if(pathInfo[1].equals("updateProperty"))
			{
				String itemUID = pathInfo[2];
				String propertyName = req.getParameter("propertyName");
				String value = req.getParameter("value");
				
				wsnManager.updateProperty(itemUID, propertyName, value);
				
				resp.getWriter().write(WebApiCommons.okMessage(null));
			}
		}
	}
}
