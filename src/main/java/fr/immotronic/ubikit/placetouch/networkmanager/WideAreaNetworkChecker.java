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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.ConfigurationView;

import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.WideAreaNetworkStatus;



final class WideAreaNetworkChecker
{
	private boolean changed = false;
	private final ConfigurationView configurationView;
	private WideAreaNetworkStatus wanStatus = null;

	final Logger logger = LoggerFactory.getLogger(WideAreaNetworkChecker.class);






	WideAreaNetworkChecker(ConfigurationView configurationView)
	{
		this.configurationView = configurationView;
	}






	synchronized WideAreaNetworkStatus check()
	{
		String testIP = configurationView.getString(Property.network_internet_reachabilityTestIp);

		String testDomain = configurationView
			.getString(Property.network_internet_reachabilityTestHostName);

		try
		{
			InetAddress adr = InetAddress.getByName(testIP);
			if (adr.isReachable(3000))
			{
				InetAddress adr2 = InetAddress.getByName(testDomain);
				if (adr2.isReachable(3000))
				{
					setStatus(WideAreaNetworkStatus.CONNECTED);
					logger.debug("Internet is reachable");
				}
				else
				{
					setStatus(WideAreaNetworkStatus.CONNECTED_BUT_DNS_ISSUE);
					logger.warn("Cannot reach '{}', but '{}' was reachable.", testDomain, testIP);
				}
			}
			else
			{
				logger.debug("Cannot reach {}.", testIP);
				setStatus(WideAreaNetworkStatus.NOT_CONNECTED);
			}
		}
		catch (UnknownHostException e)
		{
			setStatus(WideAreaNetworkStatus.CONNECTED_BUT_DNS_ISSUE);
			logger.warn("Cannot resolve '{}', but '{}' was reachable.", testDomain, testIP);
		}
		catch (IOException e)
		{
			logger.error("Network error while checking internet connection: ", e);
			setStatus(null);
		}

		return wanStatus;
	}






	synchronized WideAreaNetworkStatus getStatus()
	{
		return wanStatus;
	}






	synchronized boolean hasChangedSincePreviousCheck()
	{
		return changed;
	}






	synchronized void invalidate()
	{
		setStatus(null);
	}






	synchronized void markStatusAsRead()
	{
		changed = false;
	}






	private void setStatus(WideAreaNetworkStatus status)
	{
		if (this.wanStatus != status)
		{
			logger.info("WAN status changed to {}.", status);
			this.wanStatus = status;
			changed = true;
		}
		else
		{
			changed = false;
		}
	}
}
