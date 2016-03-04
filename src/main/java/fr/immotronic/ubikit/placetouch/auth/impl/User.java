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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.immotronic.tools.Base64;



class User
{
	private String username = null;
	private String sha1EncyptedPassword = null;
	private String plainPassword = null;
	private final MessageDigest shaMessageDigest;

	private static User user = new User();

	final Logger logger = LoggerFactory.getLogger(User.class);






	private User()
	{
		MessageDigest md = null;
		try
		{
			md = MessageDigest.getInstance("SHA");
		}
		catch (NoSuchAlgorithmException e)
		{
			logger.error(
				"Error when creating a Message Digest. THIS EXCEPTION SHOULD NEVER HAPPEN.",
				e);
		}
		finally
		{
			shaMessageDigest = md;
		}
	}






	private String computeSHADigest(String password)
	{
		try
		{
			if (shaMessageDigest != null)
			{
				byte[] bDigest = shaMessageDigest.digest(password.getBytes("UTF-8"));
				StringBuffer sbDigest = new StringBuffer();
				for (byte b : bDigest)
				{
					sbDigest.append(String.format("%02x", b & 0xff));
				}
				return sbDigest.toString();
			}
		}
		catch (UnsupportedEncodingException e)
		{
			logger.error("Error when encoding a String. THIS EXCEPTION SHOULD NEVER HAPPEN.", e);
		}
		return null;
	}






	static User getInstance()
	{
		return user;
	}






	void update(String username, String plainPassword)
	{
		this.username = username;
		this.plainPassword = plainPassword;
		this.sha1EncyptedPassword = computeSHADigest(plainPassword);
	}






	public String getUsername()
	{
		return this.username;
	}






	boolean checkLoginAndEncryptedPassword(String username, String sha1EncyptedPassword)
	{
		if (this.username != null
			&& this.sha1EncyptedPassword != null && this.username.equals(username)
			&& this.sha1EncyptedPassword.equals(sha1EncyptedPassword))
		{

			return true;
		}

		return false;
	}






	boolean checkLoginAndPlainPassword(String username, String plainPassword)
	{
		if (this.username != null
			&& this.plainPassword != null && this.username.equals(username)
			&& this.plainPassword.equals(plainPassword))
		{

			return true;
		}

		return false;
	}






	boolean checkHttpBasicAuth(String authzHeader)
	{
		assert authzHeader != null
			&& authzHeader.length() > 6 : "authzHeader seems NOT be a valid HTTP authentification header";

		String usernameAndPassword = new String(Base64.decode(authzHeader.substring(6)));
		int userNameIndex = usernameAndPassword.indexOf(":");
		String username = usernameAndPassword.substring(0, userNameIndex);
		String password = usernameAndPassword.substring(userNameIndex + 1);

		logger.debug(
			"HTTP authentification String contains username='{}', password='{}'.",
			username,
			password);
		logger.debug(
			"Expected credentials are username='{}', password='{}'.",
			this.username,
			this.plainPassword);

		if (this.username != null
			&& this.plainPassword != null && this.username.equals(username)
			&& this.plainPassword.equals(password))
		{

			return true;
		}

		return false;
	}






	String getHttpBasicAuth()
	{
		String credentials = username
			+ ":" + plainPassword;
		return "Basic "
			+ new String(Base64.encode(credentials.getBytes()));
	}






	String getHttpBasicAuthBasedOnHwid(String hwid)
	{
		String credentials = hwid.replaceAll(":", "")
			+ ":" + plainPassword;
		return "Basic "
			+ new String(Base64.encode(credentials.getBytes()));
	}
}
