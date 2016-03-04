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

package fr.immotronic.ubikit.placetouch.system.impl;

import java.util.Collection;

import org.ubikit.DatabaseProxy;
import org.ubikit.event.EventGate;
import org.ubikit.pem.event.HardwareLinkStatusEvent;

import fr.immotronic.ubikit.placetouch.cloud.GenericNetworkManagerObserver;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager;
import fr.immotronic.ubikit.placetouch.system.SystemEvent;
import fr.immotronic.ubikit.placetouch.system.SystemEvent.Severity;
import fr.immotronic.ubikit.placetouch.system.SystemEventObserver;

public class SystemEventObserverImpl implements SystemEventObserver
{	
	private final SystemEventDatabase systemEventDatabase;
	private BatteryObserver batteryObserver = null;
	
	private long lastInsertedTime = 0;
	
	public SystemEventObserverImpl(DatabaseProxy databaseProxy, NetworkManager networkManager, EventGate pemsEventGate, boolean isBatteryObserverEnabled)
	{
		systemEventDatabase = new SystemEventDatabase(databaseProxy);
		
		if (isBatteryObserverEnabled) {
			batteryObserver = new BatteryObserver(this);
		}
		
		logSystemEvent("Starting Placetouch.", Severity.NORMAL);
		pemsEventGate.addListener(new HardwareLinkStatusEvent.Listener() {
			
			@Override
			public void onEvent(HardwareLinkStatusEvent event)
			{
				switch(event.getStatus())
				{
					case CONNECTED:
						logSystemEvent("Hardware link of "+event.getPemName()+" is connected", Severity.NORMAL);
						break;
						
					case DISCONNECTED:
						logSystemEvent("Hardware link of "+event.getPemName()+" is disconnected", Severity.HIGH);
						break;
						
					case UNKNOWN:
						logSystemEvent("Hardware link of "+event.getPemName()+" is in an unknonw state", Severity.HIGH);
						break;
						
					default:
						break;
				}
			}
		});
		addNetworkManagerObserver(networkManager);
	}
	
	protected synchronized void logSystemEvent(String message, Severity severity)
	{
		long now = System.currentTimeMillis();
		if (now <= lastInsertedTime) {
			// This case permits to preserve events logging order.
			systemEventDatabase.insertSystemEvent(lastInsertedTime+1, message, severity.getSeverity());
			lastInsertedTime++;
		}
		else {
			systemEventDatabase.insertSystemEvent(now, message, severity.getSeverity());
			lastInsertedTime = now;
		}
	}
	
	@Override
	public synchronized Collection<SystemEvent> getSystemEvents(int maxEvents)
	{
		if (maxEvents >= 0)
			return systemEventDatabase.getSystemEvents(maxEvents, Severity.NONE.getSeverity());
		
		return null;
	}
	
	public synchronized Collection<SystemEvent> getSystemEvents(int maxEvents, Severity minSeverity)
	{
		if (maxEvents >= 0)
			return systemEventDatabase.getSystemEvents(maxEvents, minSeverity.getSeverity());
		
		return null;
	}
	
	private void addNetworkManagerObserver(NetworkManager networkManager)
	{
		networkManager.addNetworkManagerObserver(new GenericNetworkManagerObserver() {

			private Boolean internetWasReachable;
			private Boolean distantAccessWasAvailable;
			private Boolean appstoreWasReachable;
			
			@Override
			public void noNetwork() { 
				logSystemEvent("Ethernet cable is unplugged.", Severity.HIGH);
				internetWasReachable = null; distantAccessWasAvailable = null; appstoreWasReachable = null;
			}

			@Override
			public void noIPAddressAssigned() { logSystemEvent("No IP address assigned.", Severity.HIGH); }

			@Override
			public void localNetworkIsUp() { 
				logSystemEvent("IP address assigned.", Severity.LOW);
				internetWasReachable = null; distantAccessWasAvailable = null; appstoreWasReachable = null;
			}
			
			@Override
			public void networkIsUp() { 
				logSystemEvent("Network is up.", Severity.NORMAL);
			}
			
			@Override
			public void internetIsReachable() {
				if (internetWasReachable == null || !internetWasReachable)
					logSystemEvent("Internet is reachable.", Severity.LOW);
				else
					logSystemEvent("Internet is reachable.", Severity.NONE);
				internetWasReachable = true;
			}
			
			@Override
			public void noInternetAccess() { 
				if (internetWasReachable == null || internetWasReachable)
					logSystemEvent("Internet is not reachable.", Severity.HIGH);
				else
					logSystemEvent("Internet is not reachable.", Severity.NONE);
				internetWasReachable = false;
			}
			
			@Override
			public void distantAccessIsAvailable() {
				if (distantAccessWasAvailable == null || !distantAccessWasAvailable)
					logSystemEvent("Distant access is available.", Severity.LOW);
				else
					logSystemEvent("Distant access is available.", Severity.NONE);
				distantAccessWasAvailable = true;
			}
			
			@Override
			public void noDistantAccess() { 
				if (distantAccessWasAvailable == null || distantAccessWasAvailable)
					logSystemEvent("No distant access.", Severity.NORMAL);
				else
					logSystemEvent("No distant access.", Severity.NONE);
				distantAccessWasAvailable = false;
			}
			
			@Override
			public void appstoreConnected()	{
				if (appstoreWasReachable == null || !appstoreWasReachable)
					logSystemEvent("Appstore is reachable.", Severity.LOW);
				else
					logSystemEvent("Appstore is reachable.", Severity.NONE);
				appstoreWasReachable = true;
			}
			
			@Override
			public void noAppstore() {
				if (appstoreWasReachable == null || appstoreWasReachable)
					logSystemEvent("Appstore is not reachable.", Severity.NORMAL);
				else {
					// Nothing is logged to avoid flooding log database.
					// logSystemEvent("Appstore is not reachable.", Severity.NONE);
				}
				appstoreWasReachable = false;
			}
			
		});
	}
	
	public void stop()
	{
		if (batteryObserver != null) {
			batteryObserver.stop();
			batteryObserver = null;
		}
		
		logSystemEvent("Stopping Placetouch.", Severity.NORMAL);		
	}
}
