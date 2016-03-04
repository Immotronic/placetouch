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

package fr.immotronic.ubikit.placetouch.impl;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.ConfigurationView;
import org.ubikit.system.ExtensionManagerService;
import org.ubikit.system.ExtensionManagerService.ExtensionType;
import org.ubikit.system.ExtensionProperties;

//import fr.immotronic.license.LicenseName;
//import fr.immotronic.license.impl.LicenseManagerImpl;
import fr.immotronic.ubikit.pems.enocean.PEMEnocean;
import fr.immotronic.ubikit.placetouch.cloud.GenericNetworkManagerObserver;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager;
import fr.immotronic.ubikit.placetouch.config.AppListManager;

import org.ubikit.ConfigurationManager;

import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;
import fr.immotronic.ubikit.placetouch.license.LicenseInstaller;



public class LicenseExecutor /*implements Runnable, LicenseInstaller*/
{
	/*static final int MAX_APPS_DURATION_WITHOUT_LICENSE = 129600; // 36h

	final Logger logger = LoggerFactory.getLogger(LicenseExecutor.class);

	private final ExtensionManagerService extensionManagerService;
	private final AppListManager appListManager;
	private final LicenseManagerImpl licenseManager;
	private final ConfigurationView configurationView;
	private final PEMEnocean pemEnocean;






	LicenseExecutor(ExtensionManagerService extensionManagerService,
					AppListManager appListManager,
					LicenseManagerImpl licenseManager,
					ConfigurationManager configurationManager,
					ScheduledExecutorService executorService,
					NetworkManager networkManager,
					PEMEnocean pemEnocean)
	{
		this.extensionManagerService = extensionManagerService;
		this.appListManager = appListManager;
		this.licenseManager = licenseManager;
		
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
		
		this.pemEnocean = pemEnocean;
		
		executorService.scheduleWithFixedDelay(
			this,
			MAX_APPS_DURATION_WITHOUT_LICENSE,
			MAX_APPS_DURATION_WITHOUT_LICENSE,
			TimeUnit.SECONDS);
		
		networkManager.addNetworkManagerObserver(new GenericNetworkManagerObserver() {
			@Override
			public void noNetwork()
			{
				logger.debug("Check license after LAN is down.");
				check(false);
			}






			@Override
			public void localNetworkIsUp()
			{
				logger.debug("Check license after LAN is up.");
				check(false);
			}
		});
	}






	@Override
	public synchronized void run()
	{
		check(true);
	}






	public void check(boolean stopAppsIfNoLicense)
	{
		logger.debug("check({})", stopAppsIfNoLicense);

		if (pemEnocean != null)
		{
			pemEnocean.checkLicense();
		}

		Collection<ExtensionProperties> apps = appListManager.getAllApps();

		LicenseName lt = licenseManager.checkLicenceFile();
		logger.debug("licenseManager returned '{}'", lt);
		try
		{
			String webstoreURL = licenseManager.getWebstoreURL();

			configurationView.set(Property.webstoreURL, ((webstoreURL == null) ? "" : webstoreURL));
		}
		catch (ConfigurationException e)
		{
			logger.error(
				"Failed to update the webstore URL in configuration. Candidate value='{}'",
				licenseManager.getWebstoreURL(),
				e);
		}
		if ((lt == LicenseName.NONE)
			|| (lt == LicenseName.INVALID))
		{
			for (ExtensionProperties app : apps)
			{
				if (stopAppsIfNoLicense)
				{
					extensionManagerService.stop(ExtensionType.APP, app.getUID());
				}

				if (lt == LicenseName.INVALID)
				{ // if license is invalid, remove apps.
					extensionManagerService.uninstall(ExtensionType.APP, app.getUID());
				}
			}
		}
	}






	@Override
	public LicenseName setLicense(String licenseUID)
	{
		try
		{
			if (licenseManager.licenceDownload(licenseUID))
			{
				configurationView.set(Property.webstoreURL, licenseManager
					.getWebstoreURL());
				return licenseManager.getInstalledLicenseName();
			}

			configurationView.set(Property.webstoreURL, "");
		}
		catch (ConfigurationException e)
		{
			logger.error("Failed to update the webstore URL in configuration.", e);
		}
		return null;
	}*/
}
