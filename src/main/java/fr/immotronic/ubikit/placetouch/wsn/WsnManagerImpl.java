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

package fr.immotronic.ubikit.placetouch.wsn;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.AbstractPhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentItem.Property;
import org.ubikit.PhysicalEnvironmentModelManager;
import org.ubikit.AbstractPhysicalEnvironmentModelEvent;
import org.ubikit.event.EventGate;
import org.ubikit.pem.event.AddItemEvent;
import org.ubikit.pem.event.EnterPairingModeEvent;
import org.ubikit.pem.event.ExitPairingModeEvent;
import org.ubikit.pem.event.NewItemEvent;
import org.ubikit.pem.event.ItemAddedEvent;
import org.ubikit.pem.event.ItemAddingFailedEvent;
import org.ubikit.pem.event.NewItemEvent.CapabilitySelection;
import org.ubikit.service.PhysicalEnvironmentModelService;
import org.ubikit.service.RootPhysicalEnvironmentModelService;

import org.ubikit.Logger;

import fr.immotronic.ubikit.placetouch.impl.LC;

public final class WsnManagerImpl implements WsnManager, NewItemEvent.Listener, ItemAddedEvent.Listener, ItemAddingFailedEvent.Listener
{
	private final class AddItemStatusImpl implements AddItemStatus
	{
		private final String reason;
		private final int errorCode;
		
		AddItemStatusImpl()
		{
			this(null, 0);
		}
		
		AddItemStatusImpl(String reason, int errorCode)
		{
			this.reason = reason;
			this.errorCode = errorCode;
		}

		@Override
		public boolean isSuccessful() 
		{
			return errorCode == 0;
		}

		@Override
		public String getFailureReason() 
		{
			return reason;
		}

		@Override
		public int getErrorCode() 
		{
			return errorCode;
		}
		
		
	}
	
	private final EventGate pemEventGate;
	private final PhysicalEnvironmentModelManager pemManager;
	private final Map<String, JSONObject> newItems;
	private final Map<String, JSONObject> items;
	private final BlockingQueue<AbstractPhysicalEnvironmentModelEvent> addingEvent;
	
	public WsnManagerImpl(EventGate pemEventGate, PhysicalEnvironmentModelManager pemManager)
	{
		this.pemEventGate = pemEventGate;
		this.pemManager = pemManager;
		newItems = Collections.synchronizedMap(new HashMap<String, JSONObject>());
		items = Collections.synchronizedMap(new HashMap<String, JSONObject>());
		addingEvent = new ArrayBlockingQueue<AbstractPhysicalEnvironmentModelEvent>(1);
		getExistingPemItems();
		pemEventGate.addListener(this);
	}
	
	@Override
	public void enterPairingMode()
	{
		pemEventGate.postEvent(new EnterPairingModeEvent());
	}
	
	@Override
	public void exitPairingMode()
	{
		pemEventGate.postEvent(new ExitPairingModeEvent());
		newItems.clear();
	}
	
	@Override
	public AddItemStatus addItem(String itemUID, String customName, String location, Map<String, String> customProperties, String[] capabilities)
	{
		if(itemUID == null || itemUID.isEmpty()) {
			return new AddItemStatusImpl("ITEM_UID_IS_EMPTY", 1);
		}
		
		if(customName == null || customName.isEmpty()) {
			return new AddItemStatusImpl("CUSTOM_NAME_IS_EMPTY", 1);
		}
		
		AddItemEvent addItemEvent = new AddItemEvent(itemUID);
		addItemEvent.addUserProperties(Property.CustomName.name(), customName);
		if(location != null) {
			addItemEvent.addUserProperties(Property.Location.name(), location);
		}
		
		if(customProperties != null) 
		{
			for(String key : customProperties.keySet())
			{
				addItemEvent.addUserProperties(key, customProperties.get(key));
			}
		}
		
		addItemEvent.addCapabilities(capabilities);
		
		Logger.debug(LC.gi(), this, "Posting AddItemEvent...");
		pemEventGate.postEvent(addItemEvent);
		AbstractPhysicalEnvironmentModelEvent addingResponseEvent = null;
		try {
			Logger.debug(LC.gi(), this, "Begin to Wait for an adding response event");
			while(addingResponseEvent == null || !addingResponseEvent.getSourceItemUID().equals(itemUID))
			{
				Logger.debug(LC.gi(), this, "Waiting...");
				addingResponseEvent = addingEvent.take();
				Logger.debug(LC.gi(), this, "got adding response event for "+addingResponseEvent.getSourceItemUID());
			}
		} 
		catch (InterruptedException e) { return new AddItemStatusImpl("INTERRUPTED_WHILE_WAITING_FOR_ADDING_RESPONSE_FROM_PEM", 1); }
		
		if(addingResponseEvent instanceof ItemAddedEvent)
		{
			return new AddItemStatusImpl();
		}
		else if(addingResponseEvent instanceof ItemAddingFailedEvent)
		{
			ItemAddingFailedEvent e = (ItemAddingFailedEvent)addingResponseEvent;
			return new AddItemStatusImpl(e.getReason(), e.getErrorCode());
		}
		
		return new AddItemStatusImpl("UNKNOWN_REASON", 1);
	}
	
	@Override
	public void dropItem(String itemUID)
	{
		JSONObject item = items.remove(itemUID);
		if(item != null)
		{
			try 
			{
				String pemUID = item.getString("pemUID");
				RootPhysicalEnvironmentModelService pem = (RootPhysicalEnvironmentModelService) pemManager.getModel(pemUID);
				
				assert pem != null : "At this point, pem SHOULD never be null";
				
				if(pem != null) {
					if(pem.removeItem(itemUID) == null)
					{
						Logger.warn(LC.gi(), this, "No item to remove in PEM: itemUID="+itemUID+", pemUID="+pemUID);
					}
				}
				else {
					Logger.warn(LC.gi(), this, "Cannot remove an item: PEM UID is unknown: pemUID="+pemUID);
				}
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "Error while removing an item SHOULD never happen", e);
			}
		}
		else {
			Logger.warn(LC.gi(), this, "Cannot removing an item that do NOT exist: itemUID="+itemUID);
		}
	}
	
	@Override
	public void updateProperty(String itemUID, String propertyName, String value)
	{
		JSONObject item = items.get(itemUID);
		if(item != null)
		{
			try 
			{
				String pemUID = item.getString("pemUID");
				PhysicalEnvironmentModelService pem = pemManager.getModel(pemUID);

				assert pem != null : "At this point, pem SHOULD never be null";
				
				if(pem != null) {
					PhysicalEnvironmentItem pemItem = pem.getItem(itemUID);
					pemItem.setPropertyValue(propertyName, value);
					item.put(propertyName, pemItem.getPropertyValue(propertyName));
				}
			}
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "Error while removing an item SHOULD never happen", e);
			}
		}
	}
	
	@Override
	public JSONObject getPEMbaseURLs()
	{
		JSONObject res = new JSONObject();
		for(String pemUID : pemManager.getRegisteredPemUIDs())
		{
			RootPhysicalEnvironmentModelService pem = (RootPhysicalEnvironmentModelService) pemManager.getModel(pemUID);
			if(/*p.isBinded()*/pem != null)
			{
				try 
				{
					res.put(pemUID, pem.getBaseURL());
				} 
				catch (JSONException e) 
				{
					assert false : "JSON object construction should never fail";
					return null;
				}
			}
		}
		
		return res;
	}
	
	@Override
	public JSONObject[] getNewItemList()
	{
		return newItems.values().toArray(new JSONObject[0]);
	}
	
	@Override
	public JSONObject[] getItemList()
	{
		return items.values().toArray(new JSONObject[0]);
	}
	
	@Override
	public JSONObject getItemValue(String itemUID)
	{
		JSONObject item = items.get(itemUID);
		if(item != null)
		{
			try 
			{
				String pemUID = item.getString("pemUID");
				PhysicalEnvironmentModelService pem = pemManager.getModel(pemUID);
				
				assert pem != null : "At this point, pem SHOULD never be null";
				
				if(pem != null) {
					JSONObject value = pem.getItem(itemUID).getValueAsJSON();
					if(value != null) {
						value.put("now", (new Date()).getTime());
						return value;
					}
					return null;
				}
			} 
			catch (JSONException e) { 
				Logger.error(LC.gi(), this, "Error while getting item value SHOULD never happen", e);
			}
			catch(Exception e) {
				Logger.error(LC.gi(), this, "did the item have been removed in the meanwhile ?", e);
			}
			
		}
		
		return null;
	}

	@Override
	public void onEvent(NewItemEvent event) 
	{
		if(newItems.get(event.getSourceItemUID()) == null) // If the new item is not yet in list
		{
			JSONObject item = new JSONObject();
			JSONArray capabilities = new JSONArray();
			
			try 
			{
				item.put("uid", event.getSourceItemUID());
				item.put("htmlID", AbstractPhysicalEnvironmentItem.getHTMLIDFromUID(event.getSourceItemUID()));
				item.put("pemUID", event.getPemUID());
				if(event.getItemType() != null) {
					item.put("type", event.getItemType().name());
				}
				
				if(event.getCapabilities() != null) {
					for(String c : event.getCapabilities())
					{
						capabilities.put(c);
					}
				}
				
				item.put("capabilities", capabilities);
				
				CapabilitySelection cs = event.doesCapabilitiesHaveToBeSelected();
				if(cs != CapabilitySelection.NO)
				{
					item.put("capabilitySelection", cs.name());
				}
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "Cannot build a new item JSON Object");
				return;
			}
			
			newItems.put(event.getSourceItemUID(), item);
		}
	}

	@Override
	public void onEvent(ItemAddedEvent event) 
	{
		if(items.get(event.getSourceItemUID()) == null) // If the new item is not yet in list
		{
			JSONObject item = new JSONObject();
			JSONArray capabilities = new JSONArray();
			
			try 
			{
				item.put("uid", event.getSourceItemUID());
				item.put("htmlID", AbstractPhysicalEnvironmentItem.getHTMLIDFromUID(event.getSourceItemUID()));
				item.put("pemUID", event.getPemUID());
				if(event.getItemType() != null) {
					item.put("type", event.getItemType().name());
				}
				
				if(event.getCapabilities() != null) {
					for(String c : event.getCapabilities())
					{
						capabilities.put(c);
					}
				}
				
				JSONObject userProperties = event.getUserProperties();
				
				item.put(Property.CustomName.name(), userProperties.get(Property.CustomName.name()));
				item.put(Property.Location.name(), userProperties.get(Property.Location.name()));
				
				userProperties.remove(Property.CustomName.name());
				userProperties.remove(Property.Location.name());
				
				item.put("customProperties", userProperties);
				
				item.put("capabilities", capabilities);
				
				item.put("added", true);
				
				JSONObject configuration = event.getConfiguration();
				if(configuration != null) {
					item.put("configuration", configuration);
				}
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "Cannot build a new item JSON Object.", e);
				return;
			}
			
			items.put(event.getSourceItemUID(), item);
			newItems.put(event.getSourceItemUID(), item);
			addingEvent.offer(event);
		}
	}
	
	@Override
	public void onEvent(ItemAddingFailedEvent event)
	{
		if(!addingEvent.offer(event)) {
			Logger.debug(LC.gi(), this, "ItemAddingFailedEvent offer failed.");
		}
		else {
			Logger.debug(LC.gi(), this, "ItemAddingFailedEvent offer succeed.");
		}
	}
	
	private void getExistingPemItems()
	{
		for(String pemUID : pemManager.getRegisteredPemUIDs())
		{
			PhysicalEnvironmentModelService p = pemManager.getModel(pemUID);
			Collection<PhysicalEnvironmentItem> items = p.getAllItems();
			for(PhysicalEnvironmentItem i : items)
			{
				JSONObject item = new JSONObject();
			
				try 
				{
					item.put("uid", i.getUID());
					item.put("htmlID", i.getHTMLID());
					item.put("pemUID", p.getUID());
					item.put("type", i.getType().name());
					
					JSONObject userProperties = i.getPropertiesAsJSONObject();
					
					item.put(Property.CustomName.name(), userProperties.getString(Property.CustomName.name()));
					item.put(Property.Location.name(), userProperties.getString(Property.Location.name()));
					
					userProperties.remove(Property.CustomName.name());
					userProperties.remove(Property.Location.name());
					
					item.put("customProperties", userProperties);
					
					item.put("capabilities", i.getCapabilitiesAsJSON());
					item.put("configuration", i.getConfigurationAsJSON());
				} 
				catch (JSONException e) 
				{
					Logger.error(LC.gi(), this, "Cannot build a new item JSON Object");
					return;
				}
				
				this.items.put(i.getUID(), item);
			}
		}
	}
}
