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

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.ConfigurationView;

import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager.AppstoreStatus;



final class AppstoreChecker
{
	private static final String APPSTORE_TEST_PATH = "../test";
	private static int connectionTimeout = 10000;

	private boolean changed = false;
	private final ConfigurationView configurationView;
	private AppstoreStatus appstoreStatus = null;

	final Logger logger = LoggerFactory.getLogger(AppstoreChecker.class);




	AppstoreChecker(ConfigurationView configurationView)
	{
		this.configurationView = configurationView;
	}






	synchronized AppstoreStatus check()
	{
		logger.debug("Testing appstore availability...");

		String appstoreURL = configurationView.getString(Property.webstoreURL);

		if (appstoreURL != null && !appstoreURL.isEmpty())
		{
			int responseCode = -1;

			try
			{
				URL url = new URL(appstoreURL + APPSTORE_TEST_PATH);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(connectionTimeout);
				connection.setReadTimeout(connectionTimeout);
				responseCode = connection.getResponseCode();
			}
			catch (UnknownHostException e)
			{
				setStatus(AppstoreStatus.NO_APPSTORE);
				logger.warn("Appstore host is unknown.", e);
			}
			catch (Exception e)
			{
				setStatus(AppstoreStatus.NO_APPSTORE);
				logger.error("While checking Appstore at '{}'.", appstoreURL, e);
			}

			if (responseCode == 200)
			{
				setStatus(AppstoreStatus.AVAILABLE);
				logger.debug("Appstore was found at {}.", appstoreURL);
			}
			else
			{
				setStatus(AppstoreStatus.NO_APPSTORE);
				logger.warn("Appstore '{}' is unreachable.", appstoreURL + APPSTORE_TEST_PATH);
			}
		}
		else
		{
			setStatus(AppstoreStatus.NO_APPSTORE);
			logger.debug("Appstore URL is null");
		}

		return appstoreStatus;
	}






	synchronized AppstoreStatus getStatus()
	{
		return appstoreStatus;
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






	private void setStatus(AppstoreStatus status)
	{
		if (this.appstoreStatus != status)
		{
			logger.info("APPSTORE status changed to {}.", status);
			this.appstoreStatus = status;
			changed = true;
		}
		else
		{
			changed = false;
		}
	}
}
