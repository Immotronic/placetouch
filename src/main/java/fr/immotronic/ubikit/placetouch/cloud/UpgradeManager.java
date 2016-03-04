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

package fr.immotronic.ubikit.placetouch.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.jcip.annotations.*;
//import net.lingala.zip4j.core.ZipFile;
//import net.lingala.zip4j.exception.ZipException;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.ubikit.ConfigurationManager;
import org.ubikit.ConfigurationView;
import org.ubikit.Logger;

import fr.immotronic.tools.Base64;
//import fr.immotronic.license.LicenseManager;
//import fr.immotronic.license.LicenseName;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;
import fr.immotronic.ubikit.placetouch.impl.LC;

/**
 * This class manage the framework upgrade mechanisms.
 * It need one thread to check availability of new upgrade every 24h hours.
 * 
 * This class is designed to be thread safe.
 * 
 * @author Lionel Balme
 * 
 */
@ThreadSafe
public class UpgradeManager 
{
	@Immutable
	public enum State
	{ 
		TO_BE_DETERMINE,
		NOTHING_TO_DO,
		UPGRADE_AVAILABLE,
		DOWNLOADING,
		UPGRADE_READY_TO_INSTALL,
		INSTALLING,
		READY_TO_REBOOT
	}
	
	@Immutable
	public enum Error
	{
		NO_ERROR,
		NO_UPGRADE_FOUND_ON_SERVER,
		UPGRADE_SERVER_ERROR,
		CORRUPTED_DATA,
		NETWORK_ERROR,
		INVALID_CREDENTIALS,
		CANNOT_CREATE_LOCAL_FILES,
		UPGRADE_EXTRACTION_FAILED,
		UNHANDLED_ERROR
	}
	
	private static final String UPGRADE_ARCHIVE_FILENAME = "upgrade.zip";
	private static final String UPGRADE_ARCHIVE_MD5_FILENAME = "upgrade.zip.md5";
	
	private final BundleContext bundleContext;
	private final ScheduledExecutorService executor;
	//private final LicenseManager licenseManager;
	private final String systemVersion;
	private final String upgradeDownloadFolder;
	private final String upgradeInstallationFlagFilePath;
	private final String shutdownFlagFilePath;
	private final MessageDigest md;
	private final Runnable checkForUpdateTask = new Runnable() {
		@Override
		public void run() 
		{
			evaluateCurrentState();
		}
	};
	
	private interface Downloader extends Runnable {
		public void setParameters(URL upgradeURL, File upgradeHostFile, File upgradeMD5HostFile);
	};
	
	private final Downloader downloadTask = new Downloader() {
		@Override
		public void run() 
		{
			download(upgradeURL, upgradeHostFile, upgradeMD5HostFile);
		}
		
		public void setParameters(URL upgradeURL, File upgradeHostFile, File upgradeMD5HostFile)
		{
			this.upgradeURL = upgradeURL;
			this.upgradeHostFile = upgradeHostFile;
			this.upgradeMD5HostFile = upgradeMD5HostFile;
		}
		
		private File upgradeHostFile;
		private File upgradeMD5HostFile;
		private URL upgradeURL;
	};
	
	private final Runnable frameworkRebootTask = new Runnable() {
		@Override
		public void run() 
		{
			currentStateLock.lock();
			try 
			{
				Logger.info(LC.gi(), UpgradeManager.this, "Rebooting after an upgrade installation.");
				Thread.sleep(1000); // 1 sec tempo, to let the servlet send an HTTP/1.1 200 OK status code to the UI
				bundleContext.getBundle(0).stop();
			}
			catch(Exception e) 
			{
				System.exit(0);
			}
			finally
			{
				currentStateLock.unlock();
			}
		}
	};
	
	private final Runnable frameworkShutdownTask = new Runnable() {
		
		@Override
		public void run() 
		{
			currentStateLock.lock();
			try 
			{
				Logger.info(LC.gi(), UpgradeManager.this, "Shutdowning the framework.");
				
				FileOutputStream fosFlagFile = null;
				try
				{
					File shutdownFlagFile = new File(shutdownFlagFilePath);				
					if(createFile(shutdownFlagFile))
					{
						fosFlagFile = new FileOutputStream(shutdownFlagFile);
						fosFlagFile.write(shutdownTaskAction.getBytes());
					}
				}
				catch (IOException e) 
				{
					Logger.error(LC.gi(), this, "While writing the shutdown flag file", e);
					currentState = State.TO_BE_DETERMINE;
					lastError = Error.CANNOT_CREATE_LOCAL_FILES;
				}
				finally
				{
					try 
					{
						if(fosFlagFile != null) {
							fosFlagFile.close();
						}
					} 
					catch (IOException e) 
					{
						Logger.error(LC.gi(), this, "While closing streams");
					}
				}
				
				Thread.sleep(1000); // 1 sec tempo, to let the servlet send an HTTP/1.1 200 OK status code to the UI
				bundleContext.getBundle(0).stop();
			}
			catch(Exception e) 
			{
				System.exit(0);
			}
			finally
			{
				currentStateLock.unlock();
			}
		}
	};
	
	private final Lock currentStateLock;
	
	/* currentState could be modified by 3 threads: checkForUpdateTask, downloadTask and intallationTask.
	 * It could be read only by servlet threads.
	 */
	@GuardedBy("currentStateLock AND this")
	private volatile State currentState = State.TO_BE_DETERMINE;
	
	/* downloadCompleteness and lastDownloadErrorcould be modified by 1 thread only: downloadTask.
	 * It could be read only by servlet threads.
	 */
	@GuardedBy("this") private volatile int downloadCompleteness = 0; // This value is a percentage
	@GuardedBy("this") private volatile Error lastError = Error.NO_ERROR;
	@GuardedBy("this") private volatile String availableUpgradeVersion = null;
	
	private boolean running = false;
	private String shutdownTaskAction = null;
	
	public UpgradeManager(	BundleContext bundleContext, 
							ConfigurationManager configurationManager, 
							String systemVersion, 
							ScheduledExecutorService executor, 
							NetworkManager networkManager/*,
							LicenseManager licenseManager*/)
	{
		assert bundleContext != null : "bundleContext parameter cannot be null.";
		assert configurationManager != null : "configurationManager parameter cannot be null.";
		assert systemVersion != null : "systemVersion parameter cannot be null";
		assert executor != null : "executor parameter cannot be null.";
	//	assert licenseManager != null : "licenseManager parameter cannot be null.";
		assert WebAPI.UpgradeServer.v1_0.download.getUrl() != null;
		assert WebAPI.UpgradeServer.v1_0.upgrade_availability.getUrl() != null;
		
		ConfigurationView configurationView = null;
		try
		{
			configurationView = configurationManager.createView(null, getClass().getSimpleName());
		}
		catch (ConfigurationException e)
		{
			Logger.error(LC.gi(), this, "THIS SHOULD NEVER HAPPEN, because first argument of createView is null");
		}
		
		upgradeDownloadFolder = configurationView.getString(Property.downloadFolder);
		
		if(upgradeDownloadFolder == null) {
			throw new IllegalArgumentException("The 'fr.immotronic.placetouch.downloadFolder' property MUST be set in the config.properties file.");
		}
		
		if(!upgradeDownloadFolder.endsWith("/")) {
			throw new IllegalArgumentException("The 'fr.immotronic.placetouch.downloadFolder' property value MUST end with a '/' character.");
		}
		
		upgradeInstallationFlagFilePath = bundleContext.getProperty("fr.immotronic.placetouch.upgradeInstallationFlagFile");
		
		if(upgradeInstallationFlagFilePath == null) {
			throw new IllegalArgumentException("The 'fr.immotronic.placetouch.upgradeInstallationFlagFile' property MUST be set in the config.properties file.");
		}
		
		shutdownFlagFilePath = bundleContext.getProperty("fr.immotronic.placetouch.shutdownFlagFile");
		
		if(shutdownFlagFilePath == null) {
			throw new IllegalArgumentException("The 'fr.immotronic.placetouch.shutdownFlagFile' property MUST be set in the config.properties file.");
		}
		
		MessageDigest md = null;
		try 
		{
			md = MessageDigest.getInstance("MD5");
		} 
		catch (NoSuchAlgorithmException e) 
		{
			Logger.error(LC.gi(), this, "While building the upgrade download Url. This is a bug and SHOULD NEVER HAPPEN.", e);
		}
		
		this.md = md;
		
		if(md == null) {
			throw new IllegalArgumentException("This software cannot run properly without MD5 algorithm available in Java");
		}
		
		currentStateLock = new ReentrantLock();
		this.bundleContext = bundleContext;
		this.systemVersion = systemVersion;
		this.executor = executor;
		//this.licenseManager = licenseManager;
		
		networkManager.addNetworkManagerObserver(new GenericNetworkManagerObserver() {

			@Override
			public void internetIsReachable() 
			{
				start();
			}
		});
	}
	
	public void start()
	{
		if(!running) {
			running = true;
			executor.scheduleAtFixedRate(checkForUpdateTask, 0, 1, TimeUnit.DAYS); // First execution right now, the next one 24h later.
		}
	}
	
	public synchronized State getState()
	{
		if(currentState == State.TO_BE_DETERMINE)
		{
			evaluateCurrentState();
		}
		
		return currentState;
	}
	
	public synchronized int getDownloadCompleteness()
	{
		return downloadCompleteness;
	}
	
	public synchronized Error getLastError()
	{
		return lastError;
	}
	
	public synchronized String getAvailableUpgradeVersion()
	{
		return availableUpgradeVersion;
	}
	
	public synchronized boolean downloadUpgrade(String upgradeId)
	{
		assert currentState != null : "currentState MUST be initialized.";
		assert downloadTask != null : "The download task MUST be initialized";
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "downloadUpgrade("+upgradeId+")");
		}
		
		currentStateLock.lock();
		try 
		{
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "downloadUpgrade has aquired the lock. UpgradeManager current state is "+currentState);
			}
			
			lastError = Error.NO_ERROR;
			if(currentState == State.UPGRADE_AVAILABLE)
			{
				
				URL url = new URL(WebAPI.UpgradeServer.v1_0.download.getUrl() + "?v="+upgradeId);
				
				File upgradeHostFile = new File(upgradeDownloadFolder + UPGRADE_ARCHIVE_FILENAME);
				File upgradeMD5HostFile = new File(upgradeDownloadFolder + UPGRADE_ARCHIVE_MD5_FILENAME);
				
				if(createFile(upgradeHostFile) && createFile(upgradeMD5HostFile))
				{
					if(LC.debug) {
						Logger.debug(LC.gi(), this, "downloadUpgrade starts the download of "+url);
					}
					
					downloadCompleteness = 0;
					downloadTask.setParameters(url, upgradeHostFile, upgradeMD5HostFile);
					executor.execute(downloadTask);
					
					return true;
				}
			}
		} 
		catch (MalformedURLException e) 
		{
			Logger.error(LC.gi(), this, "While building the upgrade download Url. This is a bug and SHOULD NEVER HAPPEN.", e);
		}
		catch(RejectedExecutionException e)
		{
			Logger.error(LC.gi(), this, "The download task had NOT be launched.", e);
			lastError = Error.UNHANDLED_ERROR;
		}
		catch(Exception e)
		{
			Logger.error(LC.gi(), this, "Unhandled exception.", e);
			lastError = Error.UNHANDLED_ERROR;
		}
		finally
		{
			currentStateLock.unlock();
		}
		
		return false;
	}
	
	public synchronized boolean installUpgrade()
	{
		assert currentState != null : "currentState MUST be initialized.";
		
		FileOutputStream fosFlagFile = null;
		/*
		currentStateLock.lock();
		try
		{
			lastError = Error.NO_ERROR;
			if(checkUpgradeArchive())
			{
				// Unzip the archive
				ZipFile zipFile = new ZipFile(upgradeDownloadFolder + UPGRADE_ARCHIVE_FILENAME);
				zipFile.extractAll(upgradeDownloadFolder);
				
				// set a flag to make the launch script upgrading the system at reboot.
				File upgradeInstallationFlagFile = new File(upgradeInstallationFlagFilePath);				
				if(createFile(upgradeInstallationFlagFile))
				{
					fosFlagFile = new FileOutputStream(upgradeInstallationFlagFile);
					fosFlagFile.write("INSTALL_UPGRADE".getBytes());
					
					// removing the upgrade archive and its MD5 file, it is useless now.
					File upgradeArchiveFile = new File(upgradeDownloadFolder + UPGRADE_ARCHIVE_FILENAME);
					File upgradeArchiveMD5File = new File(upgradeDownloadFolder + UPGRADE_ARCHIVE_MD5_FILENAME);
					
					upgradeArchiveFile.delete();
					upgradeArchiveMD5File.delete();
					
					// force the filesystem to sync
					try 
					{
						Runtime.getRuntime().exec("sync");
					}
					catch(Exception e) 
					{
						Logger.error(LC.gi(), this, "While trying to sync things on the mass storage support.", e);
					}
					
					// ask for reboot
					currentState = State.READY_TO_REBOOT;
					return true;
				}
				
				lastError = Error.CANNOT_CREATE_LOCAL_FILES;
			}
			
			currentState = State.TO_BE_DETERMINE;
		} 
		catch (ZipException e) 
		{
			Logger.error(LC.gi(), this, "While extracting the upgrade archive", e);
			currentState = State.TO_BE_DETERMINE;
			lastError = Error.UPGRADE_EXTRACTION_FAILED;
		} 
		catch (IOException e) 
		{
			Logger.error(LC.gi(), this, "While writing the upgrade installation flag file", e);
			currentState = State.TO_BE_DETERMINE;
			lastError = Error.CANNOT_CREATE_LOCAL_FILES;
		}
		finally
		{
			try 
			{
				if(fosFlagFile != null) {
					fosFlagFile.close();
				}
			} 
			catch (IOException e) 
			{
				Logger.error(LC.gi(), this, "While closing streams");
			}
			
			currentStateLock.unlock();
		}
		*/
		return false;
	}
	
	public synchronized boolean reboot()
	{
		Logger.debug(LC.gi(), this, "In reboot()");
		currentStateLock.lock();	
		try
		{
			Logger.debug(LC.gi(), this, "In reboot(): lock acquired, launching reboot task");
			
			// Rebooting the system
			executor.execute(frameworkRebootTask);
			return true;
		}
		catch(RejectedExecutionException e)
		{
			Logger.error(LC.gi(), this, "Cannot launch the reboot task.", e);
			return false;
		}
		finally
		{
			Logger.debug(LC.gi(), this, "In reboot(): unlocking");
			currentStateLock.unlock();
		}
	}
	
	public synchronized boolean shutdown(boolean reboot)
	{
		Logger.debug(LC.gi(), this, "In shutdown()");
		currentStateLock.lock();	
		try
		{
			Logger.debug(LC.gi(), this, "In shutdown(): lock acquired, launching shutdown task");
			
			if(reboot) {
				shutdownTaskAction = "REBOOT";
			}
			else {
				shutdownTaskAction = "SHUTDOWN";
			}
			
			// Rebooting or shutdowning the system
			executor.execute(frameworkShutdownTask);
			return true;
		}
		catch(RejectedExecutionException e)
		{
			Logger.error(LC.gi(), this, "Cannot launch the shutdown shutdown.", e);
			return false;
		}
		finally
		{
			Logger.debug(LC.gi(), this, "In shutdown(): unlocking");
			currentStateLock.unlock();
		}
	}
	
	private void evaluateCurrentState()
	{
		currentStateLock.lock();		
		try
		{
			//if(licenseManager.getInstalledLicenseName() == LicenseName.NONE || licenseManager.getInstalledLicenseName() == LicenseName.INVALID)
			//{
				Logger.debug(LC.gi(), this, "No valid license installed, automatic upgrade disabled.");
				currentState = State.NOTHING_TO_DO;
				return;
			//}
			/*
			// Is an upgrade have already been installed, and the system is ready to reboot ?
			File upgradeFlagFile = new File(upgradeInstallationFlagFilePath);
			if(upgradeFlagFile.exists())
			{
				// Yes, the system is ready to be rebooted
				currentState = State.READY_TO_REBOOT;
			}
			else
			{
				// Is there an upgrade archive already downloaded and ready to install ?
				if(checkUpgradeArchive())
				{
					// Yes, an upgrade archive is ready to install
					currentState = State.UPGRADE_READY_TO_INSTALL;
				}
				else
				{
					// No upgrade archive ready to install. Check if an upgrade is available online.
					
					availableUpgradeVersion = this.checkUpgradeAvailability();
					if(LC.debug) 
					{
						if(availableUpgradeVersion != null) 
						{
							Logger.debug(LC.gi(), this, "current system verion: "+systemVersion+", available version: "+availableUpgradeVersion+", comparision="+availableUpgradeVersion.compareTo(systemVersion));
						}
						else
						{
							Logger.debug(LC.gi(), this, "current system verion: "+systemVersion+", available version: "+availableUpgradeVersion);
						}
					}
					
					if(availableUpgradeVersion != null && availableUpgradeVersion.compareTo(systemVersion) > 0) 
					{
						currentState = State.UPGRADE_AVAILABLE;
					}
					else if(availableUpgradeVersion != null)
					{
						currentState = State.NOTHING_TO_DO;
					}
				}
			}*/
		}
		catch(Exception e)
		{
			Logger.error(LC.gi(), this, "While evaluating the upgrader current state", e);
			currentState = State.TO_BE_DETERMINE;
		}
		finally
		{
			currentStateLock.unlock();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean checkUpgradeArchive()
	{
		// Is there an upgrade already downloaded and ready to install ?
		File upgradeArchiveFile = new File(upgradeDownloadFolder + UPGRADE_ARCHIVE_FILENAME);
		File upgradeArchiveMD5File = new File(upgradeDownloadFolder + UPGRADE_ARCHIVE_MD5_FILENAME);
		
		FileInputStream archiveStream = null;
		FileInputStream md5Stream = null;
		
		// An upgrade archive was found. Is it valid ? We check with MD5 digest
		try 
		{
			// Getting the expected MD5 for the archive file
			md5Stream = new FileInputStream(upgradeArchiveMD5File);
			int md5FileSize = (int) upgradeArchiveMD5File.length();
			byte[] expectedDigest = new byte[md5FileSize];
			md5Stream.read(expectedDigest, 0, md5FileSize);
			
			// Compute the archive file MD5
			archiveStream = new FileInputStream(upgradeArchiveFile);
			DigestInputStream dis = new DigestInputStream(archiveStream, md);
			byte[] buffer = new byte[2048];
			while(dis.read(buffer, 0, buffer.length) >= 0) { }
			
			// Comparing computed MD5 and expected one
			if(!MessageDigest.isEqual(dis.getMessageDigest().digest(), expectedDigest))
			{
				return false;
			}
			
			return true;
		} 
		catch (IOException e) 
		{
			// No upgrade archive OR no upgrade archive MD5 file to validate it OR cannot read the archive file or its MD5. 
			return false;
		}
		finally
		{
			try 
			{
				if(archiveStream != null) {
					archiveStream.close();
				}
				
				if(md5Stream != null) {
					md5Stream.close();
				}
			}
			catch(IOException e) 
			{
				Logger.error(LC.gi(), this, "While closing archive & md5 streams", e);
			}
		}
	}
	
	/**
	 * Synchronization policy requires that this method could ONLY be called by the evaluateCurrentState() method. 
	 */
	private String checkUpgradeAvailability()
	{
		HttpURLConnection connection = null;
		InputStream response = null;
		
		try 
		{
			URL url = new URL(WebAPI.UpgradeServer.v1_0.upgrade_availability.getUrl() + "?from="+systemVersion);
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "checkUpgradeAvailability: connecting to "+url);
			}
			
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			
			if(responseCode != 200)
			{
				switch(responseCode)
				{
					case 404:
						Logger.error(LC.gi(), this, "The distant ressouce was not found (404)");
						synchronized(this) { lastError = Error.NO_UPGRADE_FOUND_ON_SERVER; currentState = State.TO_BE_DETERMINE; }
						return null;
					case 401:
						Logger.error(LC.gi(), this, "Provided credentials were rejected by the upgrade server (401)");
						synchronized(this) { lastError = Error.INVALID_CREDENTIALS; currentState = State.TO_BE_DETERMINE; }
						return null;
					default:
						Logger.error(LC.gi(), this, "Unexpected error while interacting with the upgrade server: "+responseCode+" "+connection.getResponseMessage());
						synchronized(this) { lastError = Error.UPGRADE_SERVER_ERROR; currentState = State.TO_BE_DETERMINE; }
						return null;
				}
			}
			
			// Reading headers
			int contentLength = connection.getContentLength();
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "checkUpgradeAvailability: HTTP response code: "+responseCode+", content-length: "+contentLength);
			}
			
			if(contentLength <= 0)
			{
				return null;
			}
			else
			{
				byte[] buffer = new byte[contentLength];
				connection.getInputStream().read(buffer, 0, contentLength);
				
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "checkUpgradeAvailability: available upgrade version: "+new String(buffer));
				}
				
				return new String(buffer);
			}
		}
		catch (MalformedURLException e) 
		{
			Logger.error(LC.gi(), this, "While building the upgrade-available Url. This is a bug and SHOULD NEVER HAPPEN.", e);
		} 
		catch (IOException e) 
		{
			Logger.error(LC.gi(), this, "While interacting with the upgrade server", e);
			synchronized(this) { lastError = Error.NETWORK_ERROR; currentState = State.TO_BE_DETERMINE; }
		}
		catch (Exception e) 
		{
			Logger.error(LC.gi(), this, "(Unhandled) While interacting with the upgrade server", e);
			synchronized(this) { lastError = Error.NETWORK_ERROR; currentState = State.TO_BE_DETERMINE; }
		}
		finally
		{
			try 
			{
				if(response != null) {
					response.close();
				}
				
				if(connection != null) {
					connection.disconnect();
				}
			}
			catch(IOException e)
			{
				Logger.error(LC.gi(), this, "While closing streams", e);
			}
		}
		
		return null;
	}
	
	private void download(URL upgradeURL, File upgradeHostFile, File upgradeMD5HostFile)
	{ 
		assert md != null : "An MD5 message digest object is required for downloadUpgrade().";
		assert upgradeHostFile != null : "upgradeHostFile cannot be null";
		assert upgradeMD5HostFile != null : "upgradeHostFile cannot be null";
		assert upgradeURL != null : "upgradeURL cannot be null";
		
		currentStateLock.lock();
		
		synchronized(this) { currentState = State.DOWNLOADING; }
		
		HttpURLConnection connection = null;
		DigestInputStream response = null;
		FileOutputStream fosArchive = null;
		FileOutputStream fosMD5 = null;
		
		try
		{
			fosArchive = new FileOutputStream(upgradeHostFile);
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "download(): connecting to "+upgradeURL);
			}
			
			connection = (HttpURLConnection) upgradeURL.openConnection();
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			
			if(responseCode != 200)
			{
				switch(responseCode)
				{
					case 404:
						Logger.error(LC.gi(), this, "The distant ressouce was not found (404)");
						synchronized(this) { lastError = Error.NO_UPGRADE_FOUND_ON_SERVER; currentState = State.TO_BE_DETERMINE; }
						return;
					case 401:
						Logger.error(LC.gi(), this, "Provided credentials were rejected by the upgrade server (401)");
						synchronized(this) { lastError = Error.INVALID_CREDENTIALS; currentState = State.TO_BE_DETERMINE; }
						return;
					default:
						Logger.error(LC.gi(), this, "Unexpected error while interacting with the upgrade server: "+responseCode+" "+connection.getResponseMessage());
						synchronized(this) { lastError = Error.UPGRADE_SERVER_ERROR; currentState = State.TO_BE_DETERMINE; }
						return;
				}
			}
			
			// Reading headers
			int contentLength = connection.getContentLength();
			byte[] expectedMd5Digest = Base64.decode(connection.getHeaderField("Content-MD5"));
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "download: HTTP response code: "+responseCode+", content-length: "+contentLength+", MD5: "+connection.getHeaderField("Content-MD5"));
			}
			
			// Get response
			response = new DigestInputStream(connection.getInputStream(), md);
			
			int byteRead;
			int totalRead = 0; 
			byte[] buffer = new byte[8192];
			while((byteRead = response.read(buffer, 0, buffer.length)) >= 0)
			{ 
				fosArchive.write(buffer, 0, byteRead);
				totalRead += byteRead;
				synchronized(this) { downloadCompleteness = (totalRead * 100) / contentLength; }
			}
			
			// Comparing response MD5 digest and the exepected one.
			boolean digestOk = MessageDigest.isEqual(expectedMd5Digest, response.getMessageDigest().digest());
			
			synchronized(this) 
			{ 
				if(digestOk) 
				{
					// Storing the upgrade MD5 to a file for possible subsequent checks
					fosMD5 = new FileOutputStream(upgradeMD5HostFile);
					fosMD5.write(expectedMd5Digest);
					lastError = Error.NO_ERROR;
					currentState = State.UPGRADE_READY_TO_INSTALL;
				}
				else
				{
					lastError = Error.CORRUPTED_DATA;
					currentState = State.TO_BE_DETERMINE;
				}
			}
		}
		catch (IOException e) 
		{
			Logger.error(LC.gi(), this, "While interacting with the upgrade server OR while writing to local files", e);
			synchronized(this) { lastError = Error.NETWORK_ERROR; currentState = State.TO_BE_DETERMINE; }
		}
		catch (Exception e) 
		{
			Logger.error(LC.gi(), this, "(Unhandled) While interacting with the upgrade server", e);
			synchronized(this) { lastError = Error.NETWORK_ERROR; currentState = State.TO_BE_DETERMINE; }
		}
		finally
		{
			try 
			{
				if(response != null) {
					response.close();
				}
				
				if(fosArchive != null) {
					fosArchive.close();
				}
				
				if(fosMD5 != null) {
					fosMD5.close();
				}
				
				if(connection != null) {
					connection.disconnect();
				}
			}
			catch(IOException e)
			{
				Logger.error(LC.gi(), this, "While closing streams", e);
			}
			
			currentStateLock.unlock();
		}
	}
	
	private boolean createFile(File file)
	{
		try
		{
			if(!file.createNewFile()) // Try to create the file. 
			{
				// File creation fails by returning false: the file may already exist. We try to delete it.
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "Trying to delete a possible previous "+file.getAbsolutePath()+" file");
				}
				
				if(file.exists() && !file.delete())
				{
					Logger.debug(LC.gi(), this, "Deletion of "+file.getAbsolutePath()+" file failed.");
				}
			}
		}
		catch(IOException e)
		{
			// File creation fails by firing an exception. Folder might not exist yet. We create it.
			file.getParentFile().mkdirs();
		}
		
		if(!file.exists()) // Is the file has been created ?
		{
			// If not, try to create it again.
			try 
			{
				if(!file.createNewFile())
				{
					// File creation fails for the second time, the storage device might be full or write protected or dead.
					Logger.error(LC.gi(), this, "Cannot create the "+file.getAbsolutePath()+" file.");
					return false;
				}
			} 
			catch (IOException e) {
				Logger.error(LC.gi(), this, "Cannot create the "+file.getAbsolutePath()+" file.", e);
				return false;
			}
		}
		
		return true;
	}
}
