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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;


/** This class contains Utility methods to configure WiFi Connection on an embedded Placetouch platform. */
public class WiFiConnectionUtils
{
	private static final String wifiConfigurationDirPath = "/etc/netctl/";
	private static final String wifiConfigurationProfileFile = "wlan0-placetouch";
	private static final String wifiService = "netctl-auto@wlan0.service";
	
	private static Error error;
	
	public enum Error {
		BAD_CONFGURATION_PARAMETERS,
		INTERNAL_ERROR
	}
	
	public enum WifiSecurity {
		WPA,
		WEP
	}
	
	public static Error getLastError()
	{
		return error;
	}
	
	public static JSONObject getWiFiConfiguration()
	{
		String line = null;
		BufferedReader br = null;
		
		try
		{
			br = new BufferedReader(new FileReader(wifiConfigurationDirPath+wifiConfigurationProfileFile));
			
			WifiSecurity wifiSecurity = null;
			String SSID = null;
			
			while ((line = br.readLine()) != null) {
				if (line.contains("Security=")) {
					wifiSecurity = WifiSecurity.valueOf(line.split("=",2)[1].toUpperCase());
				}
				else if (line.contains("SSID=")) {
					SSID = line.split("=",2)[1];
				}
			}		
			
			if (wifiSecurity != null && SSID != null)
			{
				JSONObject o = new JSONObject();
				o.put("security", wifiSecurity.name());
				o.put("ssid", SSID);
				return o;
			}
			
			// Configuration file is not valid.
			return null;
		}
		catch (FileNotFoundException e)
		{
			// In null case, there is no configuration file. We return null.
			return null;
		}
		catch (IOException e)
		{
			Logger.info(LC.gi(), null, "Error while reading a file.");
			return null;
		}
		catch (JSONException e)
		{
			Logger.info(LC.gi(), null, "Error while writing a JSONObject. This should never happen.");
			return null;
		}
		finally
		{
			try {
				if (br != null) 
					br.close();
			}
			catch (IOException e) { }
		}
	}
	
	public static boolean getWiFiConnectionState()
	{
		String line = null;
		BufferedReader br = null;
		
		try
		{
			// Get WiFi connection status
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("ifconfig", "wlan0");
			Process p = pb.start();
						
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder buff = new StringBuilder();
			line = null;
			while ((line = br.readLine()) != null) {
				buff.append(line+"\n");
			}
							
			if (buff.length() != 0 && buff.toString().contains("inet")) {
				return true;
			}
			
			return false;
		}
		catch (IOException e)
		{
			Logger.info(LC.gi(), null, "Error while reading an Inputstream.");
			return false;
		}
		finally
		{
			try {
				if (br != null) 
					br.close();
			}
			catch (IOException e) { }
		}
	}
	
	public static boolean configureWiFiConnection(WifiSecurity wifiSecurity, String SSID, String passphrase, boolean SSIDIsHidden)
	{
		if (wifiSecurity == null || SSID == null || passphrase == null)
			return false;
		
		String key = null;
		StringBuilder buff = null;
		ProcessBuilder pb = new ProcessBuilder();
		pb.redirectErrorStream(true);
		
		try
		{
			// Stop previous WiFi connection (if any)
			pb.command("systemctl", "stop", wifiService);
			pb.start();
			
			
			// Generating key
			if (wifiSecurity == WifiSecurity.WPA)
			{
				// WPA : passphrase is encrypted for security (optional).
				// key = PBKDF2encrypt(SSID, passphrase); // Too long (10 secs on Raspberry Pi)
				
				// If encryption failed, we continue with plain text key.
				if (key == null) {
					key = passphrase;
				}
			}
			else 
			{
				// WEP : passphrase must be prefixed with '\"'
				key = "\\\""+passphrase;
			}
			
			// Write new WiFi configuration file
			buff = new StringBuilder();
			buff.append("Description='A simple wireless connection generated with Placetouch'\n")
				.append("Interface=wlan0\n")
				.append("Connection=wireless\n")
				.append("Security="+wifiSecurity.name().toLowerCase()+"\n")
				.append("IP=dhcp\n")
				.append("ESSID="+SSID+"\n")
				.append("Key="+key);
			
			if (SSIDIsHidden)
				buff.append("\nHidden=yes");
			
			FileWriter fw = null;
			try
			{
				fw = new FileWriter(wifiConfigurationDirPath+wifiConfigurationProfileFile);
				fw.write(buff.toString());
				fw.flush();
			}
			catch (IOException e)
			{
				Logger.info(LC.gi(), null, "Writing WiFi configuration file failed for unknown reason.");
				error = Error.INTERNAL_ERROR;
				return false;
			}
			finally
			{
				try {
					if (fw != null) 
						fw.close();
				}
				catch (IOException e) { }
			}
			
			// Try to establish a WiFi connection.
			pb.command("systemctl", "start", wifiService);
			Process p = pb.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			buff = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				buff.append(line+"\n");
			}
			
			if (buff.length() != 0) 
			{
				Logger.info(LC.gi(), null, "Unable to established "+wifiSecurity.name()+" connection to "+SSID);
				error = Error.BAD_CONFGURATION_PARAMETERS;
				return false;
			}
			
			// Enable WiFi connection at boot.
			pb.command("systemctl", "enable", wifiService);
			pb.start();
			return true;
		}
		catch (IOException e)
		{
			Logger.info(LC.gi(), null, "systemctl command has failed to be executed.");
			error = Error.INTERNAL_ERROR;
			return false;
		}
	}
	
	public static boolean removeWiFiConnectionFile()
	{
		ProcessBuilder pb = new ProcessBuilder();
		
		try
		{
			// Stop WiFi connection (if any)
			pb.command("systemctl", "stop", wifiService);
			pb.start();
			
			// Disable WiFi connection (if any)
			pb.command("systemctl", "disable", wifiService);
			pb.start();
			
			// Remove configuration file
			File f = new File(wifiConfigurationDirPath+wifiConfigurationProfileFile);
			f.delete();
			
			return true;
		}
		catch (IOException e)
		{
			Logger.info(LC.gi(), null, "systemctl command has failed to be executed.");
			return false;
		}
	}
	
	/*private static String PBKDF2encrypt(String SSID, String passphrase)
	{
		try
		{
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		    KeySpec keyspec = new PBEKeySpec(passphrase.toCharArray(), SSID.getBytes(), 4096, 256);
		    Key key = factory.generateSecret(keyspec);
		    return DatatypeConverter.printHexBinary(key.getEncoded()).toLowerCase();
		}
		catch(NoSuchAlgorithmException e)
		{
			Logger.debug(LC.gi(), null, "PBKDF2encrypt() : Unable to find PBKDF2WithHmacSHA1 algorithm.");
			return null;
		}
		catch (InvalidKeySpecException e)
		{
			Logger.debug(LC.gi(), null, "PBKDF2encrypt() : Given key specification is inappropriate.");
			return null;
		}
	}*/
	
}
