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

package fr.immotronic.ubikit.placetouch.impl;	// APPLICATION_NAME must be replaced.

// ----- 
//
// TODO:	On the package line, replace APPLICATION_NAME with the value you entered
//			in build.xml & packaging_info.bnd files
//
//			Update the package-info.java file
//          Update the AppLauncher.java file
//
//			The rest of the file does NOT change
// -----


import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.ubikit.service.AppRegistryService;
import org.ubikit.service.PhysicalEnvironmentModelService;
import org.ubikit.service.HSQLDatabaseService;
import org.ubikit.system.ExtensionManagerService;
import org.ubikit.system.SystemInspectionService;

//import fr.immotronic.backoffice.gatewaymanagerservice.CustomerAccountInformation;
import fr.immotronic.commons.http.HttpClientService;
import fr.immotronic.commons.upnp.device.InternetGatewayDevice;
import fr.immotronic.ubikit.placetouch.PlacetouchService;
import fr.immotronic.ubikit.placetouch.auth.AuthManager;
import fr.immotronic.ubikit.placetouch.license.LicenseInstaller;
import fr.immotronic.ubikit.placetouch.system.SystemEventObserver;

@Component
@Instantiate
@Provides(specifications={PlacetouchService.class})
public class AppComponent implements PlacetouchService, AppServicePublisher
{
	/*
	 * ___EXTERNAL_DEPENDENCIES__________________________________________________________________
	 */
	
	@ServiceController(value=false)
	private boolean controller;
	
	@Requires
	private HttpService httpService = null;
		
	@Requires
	private HSQLDatabaseService hsqlDatabaseService = null;
	
	@Requires
	private ConfigurationAdmin configurationAdminService = null;

	// Because, this test-app need to inspect ubikit system data, this service is needed.
	// Regular application do NOT need it, and is NOT allowed to use it.
	@Requires
	private SystemInspectionService ubikitInspectionService = null;
	
	// Because, this test-app need to inspect ubikit system data, this service is needed.
	// Regular application do NOT need it, and is NOT allowed to use it.
	@Requires
	private ExtensionManagerService extensionManagerService = null;
	
	@Requires
	private HttpClientService httpClient = null;
	
	/*
	 * ___CONFIGURABLE_PROPERTIES__________________________________________________________________
	 */
	
	/*
	 * ___INTERNAL_CLASSES_&_MEMBERS_______________________________________________________________
	 */
	
	private BundleContext bundleContext;
	private AppLauncher appLauncher;
	
	/*
	 * ____________________________________________________________________________________________
	 */
	
	public AppComponent(BundleContext bc)
	{
		bundleContext = bc;
		appLauncher = new AppLauncher(bundleContext, this);
	}
	
	@Validate
    public synchronized void validate()
    {
		appLauncher.setSystemServices(extensionManagerService, ubikitInspectionService, httpClient);
		appLauncher.validate(configurationAdminService, httpService, hsqlDatabaseService);
    }
	
	@Invalidate
    public synchronized void invalidate()
    {
		appLauncher.invalidate();
    }
	
	@Bind
	public void bindAppRegistry(AppRegistryService appRegistry)
	{
		appLauncher.bindAppRegistry(appRegistry);
	}
	
	@Unbind
	public void unbindAppRegistry(AppRegistryService appRegistry)
	{
		appLauncher.unbindAppRegistry();
	}
	
	@Bind(optional=true, aggregate=true)
	public void bindPhysicalEnvironmentModel(PhysicalEnvironmentModelService model)
	{
		appLauncher.bindPhysicalEnvironmentModelService(model);
	}
	
	@Unbind
	public void unbindPhysicalEnvironmentModel(PhysicalEnvironmentModelService model)
	{
		appLauncher.unbindPhysicalEnvironmentModelService(model);
	}
	
	@Bind(optional=true)
	public void bindInternetGatewayDevice(InternetGatewayDevice igd)
	{
		appLauncher.setInternetGatewayDevice(igd);
	}
	
	@Unbind
	public void unbindInternetGatewayDevice(InternetGatewayDevice igd)
	{
		appLauncher.setInternetGatewayDevice(null);
	}
	
	@Override
	public void publishServices()
	{
		controller = true;
	}

	@Override
	public AuthManager getAuthManager() 
	{
		return appLauncher.getAuthManager();
	}

	/*@Override
	public LicenseInstaller getLicenseInstaller() 
	{
		return appLauncher.getLicenseInstaller();
	}*/
	
	@Override
	public SystemEventObserver getSystemEventObserver()
	{
		return appLauncher.getSystemEventObserver();
	}

	/*
	@Override
	public CustomerAccountInformation getCustomerAccountInformation()
	{
		return appLauncher.getCustomerAccountInformation();
	}*/
	
	@Override
	public String getDistantServicesBaseURL()
	{
		return appLauncher.getDistantServicesBaseURL(null);
	}
}
