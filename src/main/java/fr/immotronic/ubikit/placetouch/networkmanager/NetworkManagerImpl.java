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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.AbstractApplication;
import org.ubikit.ConfigurationView;

import fr.immotronic.ubikit.placetouch.auth.AuthManager;
import fr.immotronic.ubikit.placetouch.cloud.NetworkManagerObserver;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;

import org.ubikit.ConfigurationManager;

import fr.immotronic.ubikit.placetouch.gatewaymanager.GatewayManager;



public class NetworkManagerImpl implements Runnable, NetworkManager
{
	/**
	 * Nb of seconds between two runs of the network task
	 */
	private static final int NETWORK_TASK_PERIOD = 3;

	/**
	 * In terms of nb of runs of the network task. For instance, if NETWORK_TASK_PERIOD == 3 and
	 * NETWORK_TASK_LEVEL_2_PERIOD == 10, then level 2 tasks will run every 30 seconds.
	 */
	private static final int NETWORK_TASK_LEVEL_2_PERIOD = 10;

	/**
	 * In terms of nb of runs of the network task. For instance, if NETWORK_TASK_PERIOD == 3 and
	 * NETWORK_TASK_LEVEL_3_PERIOD == 40, then level 3 tasks will run every 120 seconds.
	 * 
	 * IMPORTANT: NETWORK_TASK_LEVEL_3_PERIOD must be a multiple of NETWORK_TASK_LEVEL_2_PERIOD.
	 * 
	 */
	private static final int NETWORK_TASK_LEVEL_3_PERIOD = 40;

	private final AppstoreChecker appstoreChecker;
	private final ConfigurationView configurationView;
	//private final DistantAccessManager distantAccessManager;
	private final ScheduledExecutorService executor;

	private final GatewayManager gatewayManager;

	private final LocalNetworkChecker localNetworkChecker;
	private final NATManager natManager;
	private final List<NetworkManagerObserver> networkManagerObservers;
	private long noNetworkTimestamp;
	private byte runCount;

	private ScheduledFuture<?> taskFuture = null;
	private final WideAreaNetworkChecker wanChecker;

	final Logger logger = LoggerFactory.getLogger(NetworkManagerImpl.class);






	public NetworkManagerImpl(	AbstractApplication application,
								InternetGatewayDeviceProvider internetGatewayDeviceProvider,
								ConfigurationManager configurationManager,
								AuthManager authManager,
								BundleContext bundleContext,
								GatewayManager gatewayManager)
	{
		this.executor = application.getExecutorService();

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

		networkManagerObservers = new CopyOnWriteArrayList<NetworkManagerObserver>();
		this.gatewayManager = gatewayManager;

		localNetworkChecker = new LocalNetworkChecker();
		wanChecker = new WideAreaNetworkChecker(configurationView);
		//distantAccessManager = new DistantAccessManager(configurationView, authManager);

		natManager = new NATManager(
			configurationView,
			internetGatewayDeviceProvider,
			this,
			application.getHttpServicePort(),
			authManager.getHwid());

		appstoreChecker = new AppstoreChecker(configurationView);
	}






	@Override
	public void addNetworkManagerObserver(NetworkManagerObserver observer)
	{
		if (observer != null)
		{
			networkManagerObservers.add(observer);
			updateNetworkObserver(observer, true);
		}
	}






	@Override
	public AppstoreStatus getAppstoreStatus()
	{
		return appstoreChecker.getStatus();
	}






	@Override
	public DomainStatus getDomainStatus()
	{
		return /*distantAccessManager.getDomainStatus();*/DomainStatus.DEACTIVATED;
	}






	@Override
	public String getExternalIPAddress()
	{
		return natManager.getExternalIPAddress();
	}






	@Override
	public WideAreaNetworkStatus getInternetStatus()
	{
		return wanChecker.getStatus();
	}






	@Override
	public String getIPAddressForEthernet()
	{
		return localNetworkChecker.getEthernetIPv4();
	}






	@Override
	public String getIPAddressForWiFi()
	{
		return localNetworkChecker.getWLANIPv4();
	}






	@Override
	public NATStatus getNATStatus()
	{
		return natManager.getStatus();
	}






	@Override
	public TunnelStatus getTunnelStatus()
	{
		return /*distantAccessManager.getTunnelStatus()*/TunnelStatus.CLOSED;
	}






	@Override
	public String getUPnPGatewayReferences()
	{
		return natManager.getInternetGatewayDeviceReferences();
	}






	@Override
	public void run()
	{
		logger.debug(" --- Running Network Inspection Task (v2) ---");
		final long currentDate = System.nanoTime();

		if (localNetworkChecker.check() == LocalNetworkStatus.NETWORK_OK)
		{
			noNetworkTimestamp = 0;

			if ((runCount
				% NETWORK_TASK_LEVEL_2_PERIOD == 0)
				&& wanChecker.check() == WideAreaNetworkStatus.CONNECTED)
			{
				if (runCount
					% NETWORK_TASK_LEVEL_3_PERIOD == 0)
				{
					appstoreChecker.check();
				}

				final boolean isDistantAccessAllowedHasChanged = configurationView
					.hasChanged(Property.network_da_enabled);

				final boolean isDistantAccessAllowed = configurationView
					.getBoolean(Property.network_da_enabled);

				if (((runCount
					% NETWORK_TASK_LEVEL_3_PERIOD == 0) || isDistantAccessAllowedHasChanged)
					&& isDistantAccessAllowed)
				{
					NATStatus ns = natManager.getStatus();

					if (ns == null
						|| ns == NATStatus.NO_UPNP_GATEWAY)
					{
						ns = natManager.check();
					}

					DomainStatus ds = DomainStatus.DEACTIVATED;
					/*DomainStatus ds = distantAccessManager.check();

					if (ds == DomainStatus.NOT_REGISTERED)
					{
						ns = natManager.check();

						switch (ns)
						{
							case NOT_MANAGED:
								ds = distantAccessManager.registerDomain(
									Integer.toString(configurationView
										.getInteger(Property.network_da_externalWebServerPort)),
									Integer.toString(configurationView
										.getInteger(Property.network_da_externalSSHPort)));
								break;

							case NAT_ISSUE:
								natManager.setupNAT();

								ds = distantAccessManager.registerDomain(Integer
									.toString(natManager.getDistantHttpPort()), Integer
									.toString(natManager.getDistantSshPort()));
								break;

							case ESTABLISHED:
								ds = distantAccessManager.registerDomain(Integer
									.toString(natManager.getDistantHttpPort()), Integer
									.toString(natManager.getDistantSshPort()));
								break;

							case NO_UPNP_GATEWAY:
								break;

							case MANUALLY_ESTABLISHED:
								break;
						}
					}
					else if (ds == DomainStatus.LINK_ISSUE)
					{
						ns = natManager.check();

						if (ns == NATStatus.NAT_ISSUE)
						{
							ns = natManager.setupNAT();
						}

						if ((ns == NATStatus.ESTABLISHED
							|| ns == NATStatus.MANUALLY_ESTABLISHED || ns == NATStatus.NOT_MANAGED))
						{

							logger.info("External IP address may have changed."
								+ " Registering domain again.");

							if (ns == NATStatus.ESTABLISHED)
							{
								ds = distantAccessManager.registerDomain(Integer
									.toString(natManager.getDistantHttpPort()), Integer
									.toString(natManager.getDistantSshPort()));
							}
							else
							{
								ds = distantAccessManager.registerDomain(
									Integer.toString(configurationView
										.getInteger(Property.network_da_externalWebServerPort)),
									Integer.toString(configurationView
										.getInteger(Property.network_da_externalSSHPort)));
							}

							if (ds != DomainStatus.LINKED)
							{
								logger.warn("NAT configuration issue: a manual"
									+ " action on the gateway is required, or UPnP can be tried.");
							}
						}
					}
					else if (ds == DomainStatus.LINKED
						&& ns != NATStatus.ESTABLISHED && ns != NATStatus.MANUALLY_ESTABLISHED)
					{
						if (ns == null
							|| ns == NATStatus.NO_UPNP_GATEWAY)
						{
							ns = natManager.check();
						}
						else
						{
							natManager.manuallyEstablished();
						}
					}
					else*/ if (ds == DomainStatus.DEACTIVATED)
					{
						logger.info("Distant access management is deactivated.");
					}
				}
			}
		}
		else
		// No network
		{
			long networkTimeout = configurationView.getInteger(Property.network_timeout);
			if (networkTimeout != 0)
			{
				if (noNetworkTimestamp == 0) // If previous network state was "OK"
				{
					noNetworkTimestamp = System.currentTimeMillis();
				}

				if ((System.currentTimeMillis() - noNetworkTimestamp) > (networkTimeout * 1000))
				{
					logger.error(
						"### No network for more than {} seconds.",
						(networkTimeout));

					logger.error("### NETWORK TIMEOUT, ASKING FOR SYSTEM REBOOT !");

					noNetworkTimestamp = Long.MAX_VALUE; // Prevent several call to reboot.
					gatewayManager.reboot();
				}
			}

			wanChecker.invalidate();
		//	distantAccessManager.invalidate();
			natManager.invalidate();
			appstoreChecker.invalidate();
			runCount = -1;
		}

		for (final NetworkManagerObserver o : networkManagerObservers)
		{
			updateNetworkObserver(o, false);
		}

		wanChecker.markStatusAsRead();
		//distantAccessManager.markDomainStatusAsRead();
		natManager.markStatusAsRead();
		appstoreChecker.markStatusAsRead();

		runCount++;
		if (runCount > 40)
		{
			runCount = 1;
		}

		if (logger.isDebugEnabled())
		{
			final long endAt = System.nanoTime();
			logger.debug(
				" --- END OF Network Inspection Task after {} ms.",
				((endAt - currentDate) / 1000000));
		}
	}






	@Override
	public void start()
	{
		logger.debug("Starting Network Inspection Task (v2)...");
		runCount = 0;
		noNetworkTimestamp = System.currentTimeMillis();
		taskFuture = executor
			.scheduleWithFixedDelay(this, 0, NETWORK_TASK_PERIOD, TimeUnit.SECONDS);
	}






	@Override
	public synchronized void stop()
	{
		logger.debug("Stopping Network Inspection Task (v2)...");

		if (taskFuture != null)
		{
			taskFuture.cancel(false);
			taskFuture = null;
		}

		networkManagerObservers.clear();

	//	distantAccessManager.stop();

		wanChecker.invalidate();
	//	distantAccessManager.invalidate();
		natManager.invalidate();
		appstoreChecker.invalidate();
	}






	private void updateNetworkObserver(NetworkManagerObserver observer, boolean forceUpdate)
	{
		final LocalNetworkStatus lns = localNetworkChecker.getStatus();
		if (lns != null
			&& (localNetworkChecker.hasChangedSincePreviousCheck() || forceUpdate))
		{
			switch (lns)
			{
				case NO_NETWORK:
					observer.noNetwork();
					break;

				case NO_IP_ADDRESS:
				case SELF_ASSIGNED_IP:
					observer.noIPAddressAssigned();
					break;

				case NETWORK_OK:
					observer.localNetworkIsUp();
					break;
			}
		}

		final WideAreaNetworkStatus wans = wanChecker.getStatus();
		if (wans != null
			&& (wanChecker.hasChangedSincePreviousCheck() || forceUpdate))
		{
			switch (wans)
			{
				case CONNECTED:
				case CONNECTED_BUT_DNS_ISSUE:
					observer.internetIsReachable();
					break;

				case NOT_CONNECTED:
					observer.noInternetAccess();
					break;
			}
		}

		/*final DomainStatus ds = distantAccessManager.getDomainStatus();
		if (ds != null
			&& (distantAccessManager.hasDomainChangedSincePreviousCheck() || forceUpdate))
		{
			switch (ds)
			{
				case LINKED:
					observer.distantAccessIsAvailable();
					break;

				case LINK_ISSUE:
					observer.noDistantAccess();
					break;

				case CANNOT_REGISTER:
					observer.noDistantAccess();
					break;

				case INVALID_CREDENTIALS:
					break;

				case NOT_REGISTERED:
					break;
			}
		}*/

		final AppstoreStatus as = appstoreChecker.getStatus();
		if (as != null
			&& (appstoreChecker.hasChangedSincePreviousCheck() || forceUpdate))
		{
			switch (as)
			{
				case AVAILABLE:
					observer.appstoreConnected();
					break;

				case NO_APPSTORE:
					observer.noAppstore();
					break;
			}
		}
	}
}
