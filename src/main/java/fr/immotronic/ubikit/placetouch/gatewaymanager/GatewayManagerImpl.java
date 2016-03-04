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

package fr.immotronic.ubikit.placetouch.gatewaymanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.ubikit.Logger;

import fr.immotronic.ubikit.placetouch.impl.LC;



public final class GatewayManagerImpl implements GatewayManager
{
	/**
	 * ACTION_REBOOT and ACTION_SHUTDOWN aim to control the behavior of the 'launcher' script. Thus,
	 * their value MUST be identical to matching variable in 'launcher' script.
	 */
	private static final String ACTION_REBOOT = "REBOOT";
	private static final String ACTION_SHUTDOWN = "SHUTDOWN";

	private final String shutdownFlagFilePath;
	private final BundleContext bundleContext;






	public GatewayManagerImpl(BundleContext bundleContext)
	{
		String shutdownFlagFilePath =
			bundleContext.getProperty("fr.immotronic.placetouch.shutdownFlagFile");

		if (shutdownFlagFilePath == null)
		{
			Logger.error(LC.gi(), this, "The 'fr.immotronic.placetouch.shutdownFlagFile' "
				+ "property MUST be set in the config.properties file.");
		}

		this.bundleContext = bundleContext;
		this.shutdownFlagFilePath = shutdownFlagFilePath;
	}






	@Override
	public synchronized void shutdown()
	{
		shutdownJVM(ACTION_SHUTDOWN);
	}






	@Override
	public synchronized void reboot()
	{
		shutdownJVM(ACTION_REBOOT);
	}






	private void shutdownJVM(String systemAction)
	{
		try
		{
			Logger.info(LC.gi(), this, "Shutdowning the JVM, then asking for system "
				+ systemAction);

			FileOutputStream fosFlagFile = null;
			try
			{
				File shutdownFlagFile = new File(shutdownFlagFilePath);
				if (createFile(shutdownFlagFile))
				{
					fosFlagFile = new FileOutputStream(shutdownFlagFile);
					fosFlagFile.write(systemAction.getBytes());
				}
			}
			catch (IOException e)
			{
				Logger.error(LC.gi(), this, "While writing the shutdown flag file", e);
			}
			finally
			{
				try
				{
					if (fosFlagFile != null)
					{
						fosFlagFile.close();
					}
				}
				catch (IOException e)
				{
					Logger.error(LC.gi(), this, "While closing streams");
				}
			}

			bundleContext.getBundle(0).stop();
		}
		catch (Exception e)
		{
			System.exit(0);
		}
	}






	private boolean createFile(File file)
	{
		try
		{
			if (!file.createNewFile()) // Try to create the file.
			{
				// File creation fails by returning false: the file may already exist. We try to
				// delete it.
				if (LC.debug)
				{
					Logger.debug(LC.gi(), this, "Trying to delete a possible previous "
						+ file.getAbsolutePath()
						+ " file");
				}

				if (file.exists()
					&& !file.delete())
				{
					Logger.debug(LC.gi(), this, "Deletion of "
						+ file.getAbsolutePath()
						+ " file failed.");
				}
			}
		}
		catch (IOException e)
		{
			// File creation fails by firing an exception. Folder might not exist yet. We create it.
			file.getParentFile().mkdirs();
		}

		if (!file.exists()) // Is the file has been created ?
		{
			// If not, try to create it again.
			try
			{
				if (!file.createNewFile())
				{
					// File creation fails for the second time, the storage device might be full or
					// write protected or dead.
					Logger.error(LC.gi(), this, "Cannot create the "
						+ file.getAbsolutePath()
						+ " file.");
					return false;
				}
			}
			catch (IOException e)
			{
				Logger.error(LC.gi(), this, "Cannot create the "
					+ file.getAbsolutePath()
					+ " file.", e);
				return false;
			}
		}

		return true;
	}
}
