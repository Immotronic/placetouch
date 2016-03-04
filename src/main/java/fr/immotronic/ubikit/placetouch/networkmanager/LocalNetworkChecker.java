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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.LocalNetworkStatus;



final class LocalNetworkChecker
{
	private boolean changed = false;
	private NetworkInterface ethernet = null;
	private String ethernetIPv4 = null;
	private LocalNetworkStatus localNetworkStatus = null;
	private NetworkInterface wlan = null;
	private String wlanIPv4 = null;

	final Logger logger = LoggerFactory.getLogger(LocalNetworkChecker.class);






	synchronized LocalNetworkStatus check()
	{
		try
		{
			ethernet = null;
			wlan = null;
			ethernetIPv4 = null;
			wlanIPv4 = null;

			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements())
			{
				NetworkInterface ni = nis.nextElement();
				if (ni.getHardwareAddress() != null)
				{
					if (ni.getDisplayName().equals("eth0")
						|| ni.getDisplayName().equals("en0"))
					{
						ethernet = ni;
					}
					else if (ni.getDisplayName().equals("wlan0")
						|| ni.getDisplayName().equals("en1"))
					{
						wlan = ni;
					}

					logger.debug(
						"A connection to a network found: {}.",
						((ethernet == ni) ? "Ethernet"
							: ((wlan == ni) ? "Wi-Fi" : "Unknown medium")));

					Enumeration<InetAddress> addresses = ni.getInetAddresses();
					while (addresses.hasMoreElements())
					{
						InetAddress inetaddress = addresses.nextElement();
						if (inetaddress.isSiteLocalAddress()
							&& inetaddress instanceof Inet4Address)
						{
							if (ethernet == ni)
							{
								if (ethernetIPv4 != null)
								{
									logger.warn("Host has more than one IP address for ethernet "
										+ "network interface.");
								}

								ethernetIPv4 = inetaddress.getHostAddress();
							}
							else if (wlan == ni)
							{
								if (wlanIPv4 != null)
								{
									logger.warn("Host has more than one IP address for Wi-Fi "
										+ "network interface.");
								}

								wlanIPv4 = inetaddress.getHostAddress();
							}
						}
					}
				}
			}

			if (wlan == null
				&& ethernet == null)
			{
				setStatus(LocalNetworkStatus.NO_NETWORK);
			}
			else if (wlanIPv4 == null
				&& ethernetIPv4 == null)
			{
				setStatus(LocalNetworkStatus.NO_IP_ADDRESS);
			}
			else
			{
				setStatus(LocalNetworkStatus.NETWORK_OK);
			}
		}
		catch (IOException e)
		{
			logger.error("Testing local network: Cannot check network interfaces", e);

			setStatus(null);
		}

		return localNetworkStatus;
	}






	synchronized String getEthernetIPv4()
	{
		return ethernetIPv4;
	}






	synchronized LocalNetworkStatus getStatus()
	{
		return localNetworkStatus;
	}






	synchronized String getWLANIPv4()
	{
		return wlanIPv4;
	}






	synchronized boolean hasChangedSincePreviousCheck()
	{
		return changed;
	}






	private void setStatus(LocalNetworkStatus status)
	{
		if (this.localNetworkStatus != status)
		{
			logger.info("LAN status changed to {}.", status);
			this.localNetworkStatus = status;
			changed = true;
		}
		else
		{
			changed = false;
		}
	}
}
