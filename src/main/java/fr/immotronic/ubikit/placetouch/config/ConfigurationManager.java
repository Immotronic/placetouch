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

package fr.immotronic.ubikit.placetouch.config;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.ubikit.DatabaseProxy;
import org.ubikit.Logger;

//import fr.immotronic.backoffice.gatewaymanagerservice.CustomerAccountInformation;
import fr.immotronic.ubikit.placetouch.impl.LC;

public final class ConfigurationManager 
{
	private static ConfigurationManager INSTANCE;
	
	private final Map<ConfigurationItem, String> items;

	private static final String TABLE_STRUCTURE = "CREATE TABLE IF NOT EXISTS configuration_data (" +
			"key VARCHAR(50) NOT NULL, " +
			"value VARCHAR(300)," +
			"PRIMARY KEY (key));";
	
	private static final String INSERT_QUERY = "INSERT INTO configuration_data (key, value) VALUES (?, ?);";
	private static final String UPDATE_QUERY = "UPDATE configuration_data SET value = ? WHERE key = ?;";
	private static final String QUERY_ALL_ITEMS = "SELECT * FROM configuration_data;";
	
	private final DatabaseProxy dbProxy;
	private final PreparedStatement getValues;
	private final PreparedStatement insertValue;
	private final PreparedStatement updateValue;
	
	private List<ConfigurationObserver> observers = new CopyOnWriteArrayList<ConfigurationObserver>();
	
	private final String fixedWebstoreURL;
	
	public static synchronized ConfigurationManager createInstance(	BundleContext bundleContext, 
	                                                            	DatabaseProxy dbProxy)
	{
		if(INSTANCE == null)
		{
			INSTANCE = new ConfigurationManager(bundleContext, dbProxy);
		}
		
		return INSTANCE;
	}
	
	public static synchronized ConfigurationManager getInstance()
	{
		if(INSTANCE == null)
		{
			throw new RuntimeException("An object is trying to use the ConfigurationManager "
				+ "instance, but this one has not been created yet.");
		}
		
		return INSTANCE;
	}
	
	private ConfigurationManager(BundleContext bundleContext, DatabaseProxy dbProxy) 
	{	
		this.dbProxy = dbProxy;
		dbProxy.executeUpdate(TABLE_STRUCTURE);
		getValues = dbProxy.getPreparedStatement(QUERY_ALL_ITEMS);
		insertValue = dbProxy.getPreparedStatement(INSERT_QUERY);
		updateValue = dbProxy.getPreparedStatement(UPDATE_QUERY);
		items = Collections.synchronizedMap(new HashMap<ConfigurationItem, String>());
		fixedWebstoreURL = bundleContext.getProperty("fr.immotronic.placetouch.test.webstoreURL");
		Logger.info(LC.gi(), this, "Using the test webstore at "+fixedWebstoreURL);
		loadConfiguration(bundleContext);
	}
	
	public void addObserver(ConfigurationObserver observer)
	{
		// Adding the observer to the list
		observers.add(observer);
		
		// Update this new observer with current data
		for(ConfigurationItem item : ConfigurationItem.values()) {
			observer.configurationUpdateHasOccured(item, items.get(item));
		}
	}
	
	public void removeObserver(ConfigurationObserver observer)
	{
		observers.remove(observer);
	}
	
	public JSONObject getConfigurationDataAsJSON()
	{
		JSONObject res = new JSONObject();
		
		try 
		{
			for(ConfigurationItem item : ConfigurationItem.values())
			{
				res.put(item.name(), items.get(item));
			}	
			return res;
		} 
		catch (JSONException e) 
		{
			Logger.error(LC.gi(), this, "While building a JSON object that contains Placetouch configuration data", e);
		}
		
		return null;
	}
	
	public String getConfigurationItem(ConfigurationItem item)
	{
		return items.get(item);
	}
	
	public void setAppstoreURL(String appstoreURL)
	{
		storeConfigurationItem(ConfigurationItem.appstoreURL, appstoreURL);
	}
	
	public String getAppstoreURL()
	{
		if(fixedWebstoreURL != null) {
			return fixedWebstoreURL;
		}
		return items.get(ConfigurationItem.appstoreURL);
	}
	
	public int getExternalWebServerPort()
	{
		return Integer.parseInt(items.get(ConfigurationItem.externalWebServerPort));
	}
	
	public void setExternalWebServerPort(int portNumber)
	{
		ConfigurationItem item = ConfigurationItem.externalWebServerPort; 
		String currentValue = items.get(item);
		String newValue = String.valueOf(portNumber);
		
		if(currentValue != null && !currentValue.equals(newValue))
		{
			storeConfigurationItem(item, newValue);
		}
	}
	
	public int getExternalSSHPort()
	{
		return Integer.parseInt(items.get(ConfigurationItem.externalSSHPort));
	}
	
	public void setExternalSSHPort(int portNumber)
	{
		ConfigurationItem item = ConfigurationItem.externalSSHPort; 
		String currentValue = items.get(item);
		String newValue = String.valueOf(portNumber);
		
		if(currentValue != null && !currentValue.equals(newValue))
		{
			storeConfigurationItem(item, newValue);
		}
	}
	
	public boolean isDistantAccessAllowed()
	{
		return Boolean.parseBoolean(items.get(ConfigurationItem.isDistantAccessAllowed));
	}
	
	public boolean isAutomaticNatManagementEnabled()
	{
		return Boolean.parseBoolean(items.get(ConfigurationItem.natAutomaticManagement));
	}
	
	public String getNatServiceDescription()
	{
		return items.get(ConfigurationItem.natServiceDescription);
	}
	
	public boolean isNatAutomaticPortSelection()
	{
		return Boolean.parseBoolean(items.get(ConfigurationItem.natAutomaticPortSelection));
	}
	
	public String getDownloadFolder()
	{
		return items.get(ConfigurationItem.downloadFolder);
	}
	
	/*public void setCustomerAccountInformation(CustomerAccountInformation customerAccountInformation)
	{
		storeConfigurationItem(ConfigurationItem.customerId, Integer.toString(customerAccountInformation.getCustomerId()));
		storeConfigurationItem(ConfigurationItem.customerName, customerAccountInformation.getCustomerName());
		storeConfigurationItem(ConfigurationItem.customerSiteId, Integer.toString(customerAccountInformation.getSiteId()));
		storeConfigurationItem(ConfigurationItem.customerSiteName, customerAccountInformation.getSiteName());
		storeConfigurationItem(ConfigurationItem.customerGatewayId, Integer.toString(customerAccountInformation.getGatewayId()));
		storeConfigurationItem(ConfigurationItem.customerGatewayLogin, customerAccountInformation.getGatewayLogin());
		storeConfigurationItem(ConfigurationItem.customerGatewayPassword, customerAccountInformation.getGatewayPassword());
	}*/
	
	public void setConfigurationData(JSONObject data) throws IllegalArgumentException
	{
		if(data != null)
		{
			Iterator<?> it = data.keys();
			while(it.hasNext())
			{
				String key = (String) it.next();
				ConfigurationItem item = null;
				
				try 
				{
					String value = data.getString(key);
					item = ConfigurationItem.valueOf(key);
					String currentValue = items.get(item);
					if(value != null && !value.equals(currentValue)) // update is necessary ONLY if current and new values are different
					{
						storeConfigurationItem(item, value);
					}
				}
				catch(IllegalArgumentException e) {
					continue; // if the entry of the JSON object is NOT a configuration item, this entry is ignored.
				}
				catch(JSONException e)
				{
					Logger.error(LC.gi(), this, "setConfigurationData(): Illegal Argument", e);
					throw new IllegalArgumentException(key+" is not a valid configuration data item.");
				}
			}
		}
	}
	
	private void loadConfiguration(BundleContext bundleContext)
	{
		// First, load configuration from database.
		ResultSet rs = dbProxy.executePreparedQuery(getValues);
		if(rs != null) 
		{
			try 
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "--");
					Logger.debug(LC.gi(), this, "-- Configuration items read from config file");
					Logger.debug(LC.gi(), this, "--");
				}
				
				for(ConfigurationItem item : ConfigurationItem.values())
				{
					String value = bundleContext.getProperty("fr.immotronic.placetouch."+item.name());
					
					if(item.name().contains("Folder")) {
						value = System.getProperty("user.dir")+"/"+value;
					}
					
					items.put(item, value);
						
					Logger.debug(LC.gi(), this, "  - "+ item.name()+" = '"+value+"'");
				}
				
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "--");
					Logger.debug(LC.gi(), this, "-- Configuration items read from DB (overwritting values from config file)");
					Logger.debug(LC.gi(), this, "--");
				}
				
				// Read items from DB and built the RAM image of configuration data.
				while(rs.next())
				{
					ConfigurationItem item = ConfigurationItem.valueOf(rs.getString(1));
					String value = rs.getString(2);
					items.put(item, value);
					
					Logger.debug(LC.gi(), this, "  - "+item.name()+" = '"+value+"'");
				}
				
				Logger.debug(LC.gi(), this, "--end--");
			} 
			catch (Exception e) 
			{
				Logger.error(LC.gi(), this, "While loading configuration data from DB.", e);
			}
		}
		else {
			Logger.error(LC.gi(), this, "While loading configuration data from DB: query result set was null.");
		}
	}
	
	private void storeConfigurationItem(ConfigurationItem item, String value)
	{
		if(item == null || value == null) {
			return;
		}
		
		String currentValue = items.get(item);
		boolean valueHasChanged = ((currentValue != null && currentValue.equals(value)) 
			|| (currentValue == null && value != null));
		
		try 
		{			
			Logger.debug(LC.gi(), this, "updateConfigurationItem(): item="+item+", new value='"+value+"'");
			
			items.put(item, value);
			
			updateValue.setString(2, item.name());
			updateValue.setString(1, value);
			if(dbProxy.executePreparedUpdate(updateValue) <= 0)
			{
				insertValue.setString(1, item.name());
				insertValue.setString(2, value);
				if(dbProxy.executePreparedUpdate(insertValue) < 0)
				{
					Logger.error(LC.gi(), this, "While inserting a new configuration item in database");
					return;
				}
			}
		} 
		catch (SQLException e) 
		{
			Logger.error(LC.gi(), this, "While (inserting a new / updating a) configuration item in database", e);
		}
		
		if(valueHasChanged)
		{
			for(ConfigurationObserver co : observers) {
				co.configurationUpdateHasOccured(item, value);
			}
		}
	}
}
