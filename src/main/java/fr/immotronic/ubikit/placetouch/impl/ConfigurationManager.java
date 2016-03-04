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

import java.util.HashMap;
import java.util.Map;

import org.ubikit.AbstractApplication;
import org.ubikit.AbstractConfigurationManager;



public final class ConfigurationManager extends AbstractConfigurationManager
{
	public static enum Property
	{
		batteryObserver,
		cloud_baseURL,
		cloud_gatewayManagerPath,
		customerId,
		customerName,
		customerSiteId,
		customerSiteName,
		customerGatewayId,
		customerGatewayLogin,
		customerGatewayPassword,
		downloadFolder,
		isAutomaticBackupEnabled,
		isAutomaticUpdateEnabled,
		network_da_enabled,
		network_da_externalWebServerPort,
		network_da_externalSSHPort,
		network_da_openTunnelScript,
		network_da_closeTunnelScript,
		network_da_checkTunnelScript,
		network_da_tunnelOutputFile,
		network_internet_reachabilityTestIp,
		network_internet_reachabilityTestHostName,
		network_nat_automaticManagement,
		network_nat_automaticPortSelection,
		network_nat_serviceDescription,
		network_timeout,
		test_webstoreURL,
		ui_httpAuthRealm,
		ui_login,
		ui_password,
		webstoreURL
	}




	public ConfigurationManager(AbstractApplication application)
	{
		super(application, "fr.immotronic.placetouch.", Property.values());
	}






	@Override
	protected Map<Enum<?>, Object> defaultConfiguration()
	{
		Map<Enum<?>, Object> defaultConfiguration = new HashMap<Enum<?>, Object>();
		defaultConfiguration.put(Property.batteryObserver, false);

		defaultConfiguration.put(
			Property.cloud_baseURL,
			"http://ws.placetouch.immotronic.fr/evolugreen/");

		defaultConfiguration.put(Property.cloud_gatewayManagerPath, "gateway-manager/gateway-api/");
		defaultConfiguration.put(Property.customerId, 0);
		defaultConfiguration.put(Property.customerName, "");
		defaultConfiguration.put(Property.customerSiteId, 0);
		defaultConfiguration.put(Property.customerSiteName, "");
		defaultConfiguration.put(Property.customerGatewayId, 0);
		defaultConfiguration.put(Property.customerGatewayLogin, "");
		defaultConfiguration.put(Property.customerGatewayPassword, "");
		defaultConfiguration.put(Property.downloadFolder, "../download/");
		defaultConfiguration.put(Property.isAutomaticBackupEnabled, false);
		defaultConfiguration.put(Property.isAutomaticUpdateEnabled, false);
		defaultConfiguration.put(Property.network_da_enabled, true);
		defaultConfiguration.put(Property.network_da_externalWebServerPort, 10080);
		defaultConfiguration.put(Property.network_da_externalSSHPort, 10022);
		defaultConfiguration.put(Property.network_da_openTunnelScript, "../scripts/open-tunnel");
		defaultConfiguration.put(Property.network_da_closeTunnelScript, "../scripts/close-tunnel");
		defaultConfiguration.put(Property.network_da_checkTunnelScript, "../scripts/check-tunnel");
		defaultConfiguration.put(Property.network_da_tunnelOutputFile, "/var/tmp/tunneloutput");
		defaultConfiguration.put(Property.network_internet_reachabilityTestIp, "92.243.17.121");

		defaultConfiguration.put(
			Property.network_internet_reachabilityTestHostName,
			"server02.immotronic.fr");

		defaultConfiguration.put(Property.network_nat_automaticManagement, true);
		defaultConfiguration.put(Property.network_nat_automaticPortSelection, true);
		defaultConfiguration.put(Property.network_nat_serviceDescription, "Placetouch");
		defaultConfiguration.put(Property.network_timeout, 86400); // Seconds
		defaultConfiguration.put(Property.test_webstoreURL, "");
		defaultConfiguration.put(Property.ui_httpAuthRealm, "Placetouch Gateway");
		defaultConfiguration.put(Property.ui_login, "admin");
		defaultConfiguration.put(Property.ui_password, "password");
		defaultConfiguration.put(Property.webstoreURL, "");

		return defaultConfiguration;
	}
}
