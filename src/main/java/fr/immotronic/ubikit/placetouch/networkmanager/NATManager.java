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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.ConfigurationView;

import fr.immotronic.commons.upnp.device.InternetGatewayDevice;
import fr.immotronic.commons.upnp.device.WANConnectionDevice;
import fr.immotronic.commons.upnp.service.WANxConnectionService;
import fr.immotronic.commons.upnp.type.PortMappingEntry;
import fr.immotronic.commons.upnp.type.PortMappingEntry.Protocol;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.NATStatus;



final class NATManager
{
	private static enum NATRuleNature
	{
		HTTP,
		SSH
	}

	private static int MAX_NAT_RETRY = 20;

	private boolean changed = false;
	private final ConfigurationView configurationView;
	private int distantHttpPort = 0;
	private int distantSshPort = 0;
	private String externalIPAddress = null;
	private boolean externalIPAddressChanged = false;
	private final String httpMappingDescription;
	private final InternetGatewayDeviceProvider internetGatewayDeviceProvider;
	private NATStatus natStatus = null;
	private final NetworkManager networkManager;
	private final String sshMappingDescription;
	private final int webUIhttpPort;

	final Logger logger = LoggerFactory.getLogger(NATManager.class);




	NATManager(	ConfigurationView configurationView,
						InternetGatewayDeviceProvider internetGatewayDeviceProvider,
						NetworkManager networkManager,
						int webUIhttpPort,
						String hardwareId)
	{
		this.configurationView = configurationView;
		this.internetGatewayDeviceProvider = internetGatewayDeviceProvider;
		this.webUIhttpPort = webUIhttpPort;
		this.networkManager = networkManager;

		httpMappingDescription = configurationView
			.getString(Property.network_nat_serviceDescription)
			+ "_" + hardwareId + "_WEB";

		sshMappingDescription = configurationView
			.getString(Property.network_nat_serviceDescription)
			+ "_" + hardwareId + "_SSH";

	}






	synchronized NATStatus check()
	{
		logger.debug("Testing NAT status access...");

		final boolean natAutomaticManagement = configurationView
			.getBoolean(Property.network_nat_automaticManagement);

		if (!natAutomaticManagement)
		{
			setStatus(NATStatus.NOT_MANAGED);
			logger.info("NAT is manually managed.");
		}
		else
		{
			final WANxConnectionService wcs = getWANConnectionService();
			if (wcs != null)
			{
				PortMappingEntry pme = null;
				int i = 0;
				boolean httpMapping = false;
				boolean sshMapping = false;
				final String hostIP = getHostIPAddress();

				while ((pme = wcs.getPortMappingEntryByIndex(i)) != null)
				{
					if (logger.isDebugEnabled())
					{
						logger.debug("Examining {} NAT rule: ext-port={}, int-port={}, host={}, descr={}",
							i, pme.getExternalPort(), pme.getInternalPort(), 
							pme.getInternalClient(), pme.getDescription());
					}

					if (pme.getInternalPort() == webUIhttpPort)
					{
						if (hostIP.equals(pme.getInternalClient()))
						{
							distantHttpPort = pme.getExternalPort();
							httpMapping = true;
							logger.debug("  --> This is required HTTP mapping.");
						}
						else if (httpMappingDescription.equals(pme.getDescription()))
						{
							logger.debug("  --> Removing obsolete HTTP mapping.");
							wcs.deletePortMapping(null, pme.getExternalPort(), Protocol.TCP);
							i--;
						}
					}
					else if (pme.getInternalPort() == configurationView.getInteger(Property.network_da_externalSSHPort))
					{
						if (hostIP.equals(pme.getInternalClient()))
						{
							distantSshPort = pme.getExternalPort();
							sshMapping = true;
							logger.debug("  --> This is required SSH mapping.");
						}
						else if (sshMappingDescription.equals(pme.getDescription()))
						{
							logger.debug("  --> Removing obsolete SSH mapping.");
							wcs.deletePortMapping(null, pme.getExternalPort(), Protocol.TCP);
							i--;
						}
					}

					if (httpMapping
						&& sshMapping)
					{
						break;
					}

					i++;
				}

				if (httpMapping
					&& sshMapping)
				{
					setStatus(NATStatus.ESTABLISHED);
					logger.debug("Required NAT rules exists.");
				}
				else
				{
					setStatus(NATStatus.NAT_ISSUE);
					logger.warn("Required NAT rules does NOT exist. They have to be created.");
				}
			}
			else
			{
				setStatus(NATStatus.NO_UPNP_GATEWAY);
				logger.warn("No UPNP Internet Gateway was found on the network.");
			}
		}

		return natStatus;
	}






	synchronized int getDistantHttpPort()
	{
		return distantHttpPort;
	}






	synchronized int getDistantSshPort()
	{
		return distantSshPort;
	}






	synchronized String getExternalIPAddress()
	{
		String ip = null;
		final WANxConnectionService wcs = getWANConnectionService();
		if (wcs != null)
		{
			ip = wcs.getExternalIPAddress();
		}

		if ((ip == null && externalIPAddress != null)
			|| (ip != null && !ip.equals(externalIPAddress)))
		{
			externalIPAddressChanged = true;
			externalIPAddress = ip;
		}

		return ip;
	}






	synchronized String getInternetGatewayDeviceReferences()
	{
		final InternetGatewayDevice igd = internetGatewayDeviceProvider.getInternetGatewayDevice();
		if (igd != null)
		{
			return igd.getFriendlyName()
				+ " by " + igd.getManufacturer();
		}

		return null;
	}






	synchronized NATStatus getStatus()
	{
		return natStatus;
	}






	synchronized boolean hasChangedSincePreviousCheck()
	{
		return changed;
	}






	synchronized boolean hasExternalIPAddressChanged()
	{
		getExternalIPAddress();
		boolean res = externalIPAddressChanged;
		externalIPAddressChanged = false;

		return res;
	}






	synchronized void invalidate()
	{
		setStatus(null);
	}






	synchronized void manuallyEstablished()
	{
		setStatus(NATStatus.MANUALLY_ESTABLISHED);
		logger.info("Required NAT rules have been manually created.");
	}






	synchronized void markStatusAsRead()
	{
		changed = false;
	}






	synchronized NATStatus setupNAT()
	{
		try
		{
			distantHttpPort = configurationView
				.getInteger(Property.network_da_externalWebServerPort);
		}
		catch (final NumberFormatException e)
		{
			logger.error("In the configuration file, externalWebServerPort is NOT a valid integer.");

			return null;
		}

		final NATStatus httpNatStatus = setupNAT(
			NATRuleNature.HTTP,
			distantHttpPort,
			webUIhttpPort,
			httpMappingDescription);

		try
		{
			distantSshPort = configurationView.getInteger(Property.network_da_externalSSHPort);
		}
		catch (final NumberFormatException e)
		{
			logger.error("In the configuration file, externalSSHPort is NOT a valid integer.");

			return null;
		}

		final NATStatus sshNatStatus = setupNAT(
			NATRuleNature.SSH,
			distantSshPort,
			configurationView.getInteger(Property.network_da_externalSSHPort),
			sshMappingDescription);

		if (httpNatStatus != sshNatStatus)
		{
			logger.error("NAT creation for HTTP and SSH differs. Weird.");
		}

		return natStatus;
	}






	private String getHostIPAddress()
	{
		String res = networkManager.getIPAddressForEthernet();
		if (res == null)
		{
			res = networkManager.getIPAddressForWiFi();
		}

		return res;
	}






	private WANxConnectionService getWANConnectionService()
	{
		final InternetGatewayDevice igd = internetGatewayDeviceProvider.getInternetGatewayDevice();
		if (igd != null)
		{
			final WANConnectionDevice wcd = igd.getDefaultWANConnectionDevice();
			if (wcd != null)
			{
				return wcd.getDefaultService();
			}
		}

		return null;
	}






	private void setStatus(NATStatus status)
	{
		if (natStatus != status)
		{
			logger.info("NAT status changed to {},", status);
			natStatus = status;
			changed = true;
		}
		else
		{
			changed = false;
		}
	}






	private NATStatus setupNAT(	NATRuleNature nature,
								int distantPort,
								int internalPort,
								String description)
	{
		final WANxConnectionService wcs = getWANConnectionService();
		if (wcs != null)
		{
			int retry = 0;

			while (retry < NATManager.MAX_NAT_RETRY
				&& !wcs.addPortMapping(
					null,
					distantPort,
					Protocol.TCP,
					internalPort,
					getHostIPAddress(),
					true,
					description,
					3600L))
			{

				logger.info("Cannot map port : ", distantPort);

				retry++;
				distantPort++;
			}

			if (retry < NATManager.MAX_NAT_RETRY)
			{
				logger.info("Port Mapping for web access has been set to {}.", distantPort);

				setStatus(NATStatus.ESTABLISHED);
			}
			else
			{
				logger.error("Port Mapping for web access failed.");
				setStatus(NATStatus.NAT_ISSUE);
			}
		}
		else
		{
			setStatus(NATStatus.NO_UPNP_GATEWAY);
		}

		switch (nature)
		{
			case HTTP:
				distantHttpPort = distantPort;
				break;

			case SSH:
				distantSshPort = distantPort;
		}

		return natStatus;
	}
}
