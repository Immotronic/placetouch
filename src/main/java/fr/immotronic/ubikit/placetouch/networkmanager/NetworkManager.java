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

import fr.immotronic.ubikit.placetouch.cloud.NetworkManagerObserver;



public interface NetworkManager
{
	public enum AppstoreStatus
	{
		AVAILABLE,
		NO_APPSTORE
	};

	public enum DomainStatus
	{
		CANNOT_REGISTER,
		DEACTIVATED,
		INVALID_CREDENTIALS,
		LINK_ISSUE,
		LINKED,
		NOT_REGISTERED
	}

	public enum LocalNetworkStatus
	{
		NETWORK_OK, // At least, one network interface got a valid site IP address,
		NO_IP_ADDRESS, // Network cable plugged or Wi-Fi connection, but no IP address assigned
		NO_NETWORK, // No network cable plugged, or no Wi-Fi connection
		SELF_ASSIGNED_IP // IP address was self-assigned, connectivity may not be ensured
	};

	public enum NATStatus
	{
		ESTABLISHED,
		MANUALLY_ESTABLISHED,
		NAT_ISSUE,
		NO_UPNP_GATEWAY,
		NOT_MANAGED
	}

	public enum TunnelStatus
	{
		CLOSED,
		OPEN
	}

	public enum WideAreaNetworkStatus
	{
		CONNECTED,
		CONNECTED_BUT_DNS_ISSUE,
		NOT_CONNECTED
	}






	public void addNetworkManagerObserver(NetworkManagerObserver listener);






	public AppstoreStatus getAppstoreStatus();






	//public String getAppstoreURL();






	public DomainStatus getDomainStatus();






	public String getExternalIPAddress();






	public WideAreaNetworkStatus getInternetStatus();






	public String getIPAddressForEthernet();






	public String getIPAddressForWiFi();






	public NATStatus getNATStatus();






	public TunnelStatus getTunnelStatus();






	public String getUPnPGatewayReferences();






	public void start();






	public void stop();
}
