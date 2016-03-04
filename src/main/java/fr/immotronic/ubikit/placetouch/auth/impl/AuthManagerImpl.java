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

package fr.immotronic.ubikit.placetouch.auth.impl;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.Configurable;
import org.ubikit.ConfigurationView;
import org.ubikit.DatabaseProxy;

import org.ubikit.ConfigurationManager;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;



public class AuthManagerImpl implements AuthController, Configurable
{
	private final ConfigurationView configurationView;
	//private final String hwid;

	final Logger logger = LoggerFactory.getLogger(AuthManagerImpl.class);






	public AuthManagerImpl(	ConfigurationManager configurationManager,
							DatabaseProxy databaseProxy/*,
							String hwid*/)
	{
		ConfigurationView configurationView = null;
		try
		{
			configurationView = configurationManager.createView(this, getClass().getSimpleName());
		}
		catch (ConfigurationException e)
		{
			logger.error("THIS SHOULD NEVER HAPPEN, because AuthManagerImpl.configurationUpdate() "
				+ "does not throw any exception");
		}
		
		this.configurationView = configurationView;
		
		//this.hwid = hwid;

	}






	@Override
	public String getUsername()
	{
		return User.getInstance().getUsername();
	}






	@Override
	public boolean updateCredentials(String login, String plainpassword)
	{
		Map<Enum<?>, Object> newCredentials = new HashMap<Enum<?>, Object>();
		newCredentials.put(Property.ui_login, login);
		newCredentials.put(Property.ui_password, plainpassword);
		try
		{
			configurationView.set(newCredentials);
			logger.info(
				"User credentials for UI access were modified from the UI. New username: '{}'",
				login);
		}
		catch (ConfigurationException e)
		{
			logger.error("AuthManager failed to update user credentials with username='{}' "
				+ "and password='{}'.", login, plainpassword);

			return false;
		}

		return true;
	}






	@Override
	public boolean checkCredentials(String login, String plainpassword)
	{
		return User.getInstance().checkLoginAndPlainPassword(login, plainpassword);
	}






	@Override
	public boolean checkHttpBasicAuth(String authzHeader)
	{
		return User.getInstance().checkHttpBasicAuth(authzHeader);
	}






	@Override
	public String getHttpBasicAuth()
	{
		return User.getInstance().getHttpBasicAuth();
	}





	/*
	@Override
	public String getHttpBasicAuthBasedOnHwid()
	{
		return User.getInstance().getHttpBasicAuthBasedOnHwid(hwid);
	}





*/
	@Override
	public String getHwid()
	{
		StringBuffer sbAltInterfaces = new StringBuffer();

		try
		{
			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

			if (nis != null)
			{
				int nb_mac = 0;

				while (nis.hasMoreElements() && nb_mac < 5)
				{
					NetworkInterface ni = nis.nextElement();

					byte[] mac = ni.getHardwareAddress();
					if (mac != null)
					{
						if (nb_mac != 0)
							sbAltInterfaces.append("_");

						for (byte b : mac) {
							sbAltInterfaces.append(String.format("%02X", b));
						}
						nb_mac++;
					}
				}

				logger.debug("read MAC addresses: {}", sbAltInterfaces.toString());
				return sbAltInterfaces.toString();
			}
		}
		catch(SocketException e)
		{
			logger.error("Cannot interact with network interfaces to read hardware addresses.", e);
		}

		return null;
	}






	@Override
	public boolean checkHttpAuthentification(	HttpServletRequest request,
												HttpServletResponse response) throws IOException
	{
		String authzHeader = request.getHeader("Authorization");
		if (authzHeader == null)
		{
			response.setHeader("WWW-Authenticate", "Basic realm=\""
				+ configurationView.getString(Property.ui_httpAuthRealm) + "\"");

			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}

		if (checkHttpBasicAuth(authzHeader))
		{
			return true;
		}
		else
		{
			response.setHeader("WWW-Authenticate", "Basic realm=\""
				+ configurationView.getString(Property.ui_httpAuthRealm) + "\"");

			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
	}






	@Override
	public void requireHttpAuthentification(HttpServletResponse response) throws IOException
	{
		response.setHeader("WWW-Authenticate", "Basic realm=\""
			+ configurationView.getString(Property.ui_httpAuthRealm) + "\"");

		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}






	@Override
	public void configurationUpdate(Enum<?>[] updatedProperties, ConfigurationView view)
	{
		if (view.hasChanged(Property.ui_login)
			|| view.hasChanged(Property.ui_password))
		{
			User.getInstance().update(
				view.getString(Property.ui_login),
				view.getString(Property.ui_password));

			logger.info("User credentials for UI access were updated from OSGi ConfigurationAdmin "
				+ "service. New username: '{}'", view.getString(Property.ui_login));
		}
	}
}
