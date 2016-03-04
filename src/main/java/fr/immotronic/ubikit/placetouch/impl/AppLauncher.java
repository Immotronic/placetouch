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

package fr.immotronic.ubikit.placetouch.impl; // APPLICATION_NAME must be replaced.

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.AbstractApplication;
import org.ubikit.Configurable;
import org.ubikit.ConfigurationView;
import org.ubikit.service.PhysicalEnvironmentModelService;
import org.ubikit.system.ExtensionManagerService;
import org.ubikit.system.SystemApp;
import org.ubikit.system.SystemInspectionService;
import org.ubikit.tools.BundleResourceUtil;

import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDRegistration;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.RegisterListener;

//import fr.immotronic.backoffice.gatewaymanagerservice.CustomerAccountInformation;
//import fr.immotronic.backoffice.gatewaymanagerservice.GatewayAPIConnector;
import fr.immotronic.commons.http.HttpClientService;
import fr.immotronic.commons.upnp.device.InternetGatewayDevice;
//import fr.immotronic.license.impl.LicenseManagerImpl;
import fr.immotronic.ubikit.pems.enocean.PEMEnocean;
import fr.immotronic.ubikit.placetouch.auth.AuthManager;
import fr.immotronic.ubikit.placetouch.auth.impl.AuthController;
import fr.immotronic.ubikit.placetouch.auth.impl.AuthManagerImpl;
import fr.immotronic.ubikit.placetouch.networkmanager.InternetGatewayDeviceProvider;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManager;
import fr.immotronic.ubikit.placetouch.networkmanager.NetworkManagerImpl;
import fr.immotronic.ubikit.placetouch.cloud.UpgradeManager;
import fr.immotronic.ubikit.placetouch.cloud.WebAPI;
import fr.immotronic.ubikit.placetouch.config.AppListManager;
import fr.immotronic.ubikit.placetouch.gatewaymanager.GatewayManager;
import fr.immotronic.ubikit.placetouch.gatewaymanager.GatewayManagerImpl;
import fr.immotronic.ubikit.placetouch.hci.APIServletApps;
import fr.immotronic.ubikit.placetouch.hci.APIServletDiagnostics;
import fr.immotronic.ubikit.placetouch.hci.APIServletSystemManagement;
import fr.immotronic.ubikit.placetouch.hci.APIServletWsn;
import fr.immotronic.ubikit.placetouch.hci.SecurityHandler;
import fr.immotronic.ubikit.placetouch.impl.ConfigurationManager.Property;
import fr.immotronic.ubikit.placetouch.license.LicenseInstaller;
import fr.immotronic.ubikit.placetouch.system.SystemEventObserver;
import fr.immotronic.ubikit.placetouch.system.impl.SystemEventObserverImpl;
import fr.immotronic.ubikit.placetouch.wsn.WsnManager;
import fr.immotronic.ubikit.placetouch.wsn.WsnManagerImpl;



// import org.ubikit.Logger;

final class AppLauncher extends AbstractApplication implements SystemApp, RegisterListener,
	InternetGatewayDeviceProvider, Configurable
{
	private static final String APP_METADATA_FILENAME = "/app-metadata.json";

	// -------List of required physical environment models----------
	//
	// TODO: Here, customize this list with UIDs of required
	// physical environment models or leaves it empty
	// if no physical environment model is required for
	// your application.
	//
	// Ex: physicalEnvironmentModelUIDs = { "fr.immotronic.ubikit.pems.enocean",
	// "fr.immotronic.ubikit.pems.knx" };
	//
	// -----
	private static final String[] physicalEnvironmentModelUIDs = { "ALL_ROOT_PEMS",
			"fr.immotronic.ubikit.pems.enocean", "fr.immotronic.ubikit.pems.modbus" };

	// -------SPECIFIC APP PRIVATE MEMBERS--------------------------

	private final AppServicePublisher appServicePublisher;
	private final DiagnosticLogger diagnosticLogger;
	private final LEDsController ledsController;
	private final LEDsManagerImpl ledsManager;
	private final String placetouchVersionNumber;
	private final String placetouchBuildDate;
	//private final LicenseManagerImpl licenseManager;
	// private final HttpRedirectorServer httpRedirectorServer;
	private final GatewayManager gatewayManager;

	private DNSSDRegistration registration = null; // object that maintains our service
													// advertisement

	private InternetGatewayDevice internetGatewayDevice = null;

	private org.ubikit.ConfigurationManager configurationManager = null;
	private ConfigurationView configurationView;
	private SystemInspectionService ubikitInspectionService = null;
	private ExtensionManagerService extensionManagerService = null;
	private AppListManager appListManager = null;
	private NetworkManager networkManager = null;
	//private LicenseExecutor licenseExecutor = null;
	private AuthController authController = null;
	private WsnManager wsnManager = null;
	//private UpgradeManager upgradeManager = null;
	private SystemEventObserverImpl systemEventObserver = null;
	private HttpClientService httpClient = null;
	//private GatewayAPIConnector gatewayManagerAPIConnector = null;

	private APIServletSystemManagement systemPropertiesServlet = null;
	private APIServletApps appsServlet = null;
	private APIServletDiagnostics diagnosticsServlet = null;
	private APIServletWsn wsnServlet = null;

	final Logger logger = LoggerFactory.getLogger(AppLauncher.class);






	// -------END OF SPECIFIC APP PRIVATE MEMBERS-------------------

	AppLauncher(BundleContext bundleContext, AppServicePublisher appServicePublisher)
	{
		super(5, bundleContext, physicalEnvironmentModelUIDs); // 5 threads: 2 pour LEDs, 1 pour
																// Placetouch observer, 1 pour
																// Gateway Port Mapper et
																// InternetConnectionChecker, 1 pour
																// l'arret automatique des apps si
																// pas de licence
		WebAPI.init(bundleContext);

		gatewayManager = new GatewayManagerImpl(bundleContext);
		diagnosticLogger = new DiagnosticLogger();

		LEDsControllerImpl ledc = null;
		LEDsManagerImpl ledm = null;
		try
		{
			ledc = new LEDsControllerImpl(bundleContext);
			ledm = new LEDsManagerImpl(ledc);
		}
		catch (Exception e)
		{
			logger.info("No valid configuration for status LED management.");
		}

		ledsController = ledc;
		ledsManager = ledm;

		this.appServicePublisher = appServicePublisher;

		String version = null;
		String build = null;
		// Read the build info file and extract system version number, build unique id and build
		// date.
		try
		{
			JSONObject appMetadata = BundleResourceUtil.getResourceJSONContent(bundleContext
				.getBundle()
				.getEntry(APP_METADATA_FILENAME));
			version = appMetadata.getString("version");
			build = appMetadata.getString("build");
		}
		catch (Exception e)
		{
			logger
				.error(
					"Cannot extract the placetouch version number. THIS EXCEPTION SHOULD NEVER HAPPEN. Check code and JAR file",
					e);
		}
		finally
		{
			placetouchVersionNumber = version;
			placetouchBuildDate = build;
		}

		/*LicenseManagerImpl lm = null;
		try
		{
			lm = new LicenseManagerImpl(bundleContext, System.getProperty("user.dir")
				+ "/" + bundleContext.getProperty("fr.immotronic.placetouch.downloadFolder"));
		}
		catch (IOException e)
		{
			logger.info("##### LICENSE MANAGER CANNOT BE CREATED #######");
			try
			{
				bundleContext.getBundle(0).stop();
			}
			catch (BundleException be)
			{
				System.exit(0);
			}
		}
		finally
		{
			licenseManager = lm;
			logger.debug("LicenseManager instance ID={}.", licenseManager);
		}*/

		/*
		 * boolean forwardingPort80enabled = Boolean.parseBoolean(
		 * bundleContext.getProperty("fr.immotronic.placetouch.port80forwarding"));
		 */

		// HttpRedirectorServer httpRedirectorServer = null;
		/*
		 * if(forwardingPort80enabled && webUIhttpPort != 80) { httpRedirectorServer = new
		 * HttpRedirectorServer(80, webUIhttpPort); }
		 */
		// this.httpRedirectorServer = httpRedirectorServer;
	}






	@Override
	public synchronized void startApplication() throws Exception
	{
		if (ubikitInspectionService == null)
		{
			throw new RuntimeException("'ubikitInspectionService' is null, THAT SHOULD NEVER "
				+ "HAPPEN. BUG FIX REQUIRED.");
		}

		if (ledsController != null)
		{
			ledsController.setExecutorService(getExecutorService());
		}

		final PhysicalEnvironmentModelService enoceanPEM = getPhysicalEnvironmentModel("fr.immotronic.ubikit.pems.enocean");

		enoceanPEM.setObserver(diagnosticLogger);

		appListManager = new AppListManager(ubikitInspectionService);

		wsnManager = new WsnManagerImpl(
			getPhysicalEnvironmentModelsEventGate(),
			getPhysicalEnvironmentModelManager());

		wsnServlet = new APIServletWsn(wsnManager);
		this.registerServlet("wsn", wsnServlet);

		if (ledsManager != null)
		{
			ledsManager.placetouchDidStarted();
			PEMsHardwareLinkObserver.create(
				getPhysicalEnvironmentModelsEventGate(),
				getPhysicalEnvironmentModelManager(),
				ledsManager);
		}

		configurationManager = new fr.immotronic.ubikit.placetouch.impl.ConfigurationManager(this);

		configurationView = configurationManager.createView(this, getClass().getSimpleName());
		
		try
		{
			authController = new AuthManagerImpl(
				configurationManager,
				getDatabaseConnection()/*,
				licenseManager.getHardwareIdentifier()*/);
		}
		catch (SQLException e2)
		{
			throw new RuntimeException("System event observer cannot get a DB connection.", e2);
		}

		setHttpSecurityHandler(new SecurityHandler(authController));

		networkManager = new NetworkManagerImpl(
			this,
			AppLauncher.this,
			configurationManager,
			authController,
			getBundleContext(),
			gatewayManager);

		if (ledsManager != null)
		{
			networkManager.addNetworkManagerObserver(ledsManager);
		}

		/*licenseExecutor = new LicenseExecutor(
			extensionManagerService,
			appListManager,
			licenseManager,
			configurationManager,
			getExecutorService(),
			networkManager,
			(PEMEnocean) enoceanPEM);

		logger.debug("Check license after LicenseExecutor creation.");
		licenseExecutor.check(false);*/

		/*upgradeManager = new UpgradeManager(
			getBundleContext(),
			configurationManager,
			placetouchVersionNumber,
			getExecutorService(),
			networkManager,
			licenseManager);*/

		try
		{
			/*
			 * TODO: Transform SystemEventObserverImpl to be dynamically reconfigurable. For the
			 * moment, if 'batteryObserver' change, SystemEventObserverImpl will not adapt.
			 * 
			 * However, this SystemEventObserer feature need a complete rework: we need a solution
			 * to let any applications or PEMs log messages for final users.
			 */
			systemEventObserver = new SystemEventObserverImpl(
				getDatabaseConnection(),
				networkManager,
				getPhysicalEnvironmentModelsEventGate(),
				configurationView.getBoolean(Property.batteryObserver));
		}
		catch (SQLException e1)
		{
			throw new RuntimeException("System event observer cannot get a DB connection.", e1);
		}

		systemPropertiesServlet = new APIServletSystemManagement(
			ubikitInspectionService,
			networkManager,
			configurationManager,
			placetouchVersionNumber,
			placetouchBuildDate,
			authController,
			/*licenseManager,
			licenseExecutor,
			upgradeManager,
			gatewayManagerAPIConnector,*/
			isEmbbeded());

		registerServlet("system", systemPropertiesServlet);

		networkManager.start();

		appsServlet = new APIServletApps(
			ubikitInspectionService,
			extensionManagerService,
			appListManager,
			/*gatewayManagerAPIConnector,*/
			configurationManager);

		registerServlet("apps", appsServlet);

		diagnosticsServlet = new APIServletDiagnostics(
			enoceanPEM,
			diagnosticLogger,
			systemEventObserver);

		registerServlet("debug", diagnosticsServlet);

		logger.debug("Trying to register a service with DNS-SD...");

		try
		{
			registration = DNSSD.register(
				0,
				0,
				ubikitInspectionService.getSystemProperties().getSystemHostname(),
				"_http._tcp",
				"",
				"",
				getHttpServicePort(),
				null,
				AppLauncher.this);
			if (registration != null)
			{
				logger.info(
					"DNS-SD service '_http._tcp' registered for {}.",
					ubikitInspectionService.getSystemProperties().getSystemHostname());
			}
		}
		catch (DNSSDException e)
		{
			if (isEmbbeded())
			{
				logger.error("Cannot register the TestService service.", e);
			}
			else
			{
				logger.info("Cannot register the TestService service.");
			}
			registration = null;
		}

		logger.info("Placetouch UI has started.");

		appServicePublisher.publishServices();

		/*
		 * if (httpRedirectorServer != null) { httpRedirectorServer.start(); }
		 */
	}






	@Override
	public synchronized void stopApplication()
	{
		if (configurationManager != null)
		{
			configurationManager.stop();
			configurationManager = null;
		}
		if (registration != null)
		{
			registration.stop();
			registration = null;
		}

		/*
		 * if (httpRedirectorServer != null) { httpRedirectorServer.stop(); }
		 */

		networkManager.stop();
		networkManager = null;

		if (ledsController != null)
		{
			ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.OFF);
			ledsController.set(LEDsController.LED.LED_2, LEDsController.Color.OFF);
		}

		if (systemEventObserver != null)
		{
			systemEventObserver.stop();
			systemEventObserver = null;
		}

		appListManager = null;
		systemPropertiesServlet = null;
		appsServlet = null;
		diagnosticsServlet = null;
		wsnServlet = null;
		wsnManager = null;
	//	licenseExecutor = null;
		authController = null;
	//	upgradeManager = null;

		PEMsHardwareLinkObserver.terminate();

		logger.info("Placetouch UI has stopped.");
	}






	@Override
	public void serviceRegistered(	DNSSDRegistration registration,
									int flags,
									String serviceName,
									String regType,
									String domain)
	{
		logger.debug("Service registered: {}, regType={}, domain={}", serviceName, regType, domain);
		logger.debug("Service registration object: ", registration.toString());
	}






	@Override
	public void operationFailed(DNSSDService service, int errorCode)
	{
		logger.error("DNS-SD Service registration failed: service={}, errorCode=", service
			.toString(), String.valueOf(errorCode));
	}






	// Not for regular application. Only because this one test ubikit SystemInspectionService.
	void setSystemServices(	ExtensionManagerService extensionManagerService,
							SystemInspectionService ubikitInspectionService,
							HttpClientService httpClient)
	{
		this.ubikitInspectionService = ubikitInspectionService;
		this.extensionManagerService = extensionManagerService;
		this.httpClient = httpClient;
	}






	synchronized void setInternetGatewayDevice(InternetGatewayDevice igd)
	{
		internetGatewayDevice = igd;
	}






	@Override
	public synchronized InternetGatewayDevice getInternetGatewayDevice()
	{
		return internetGatewayDevice;
	}






	AuthManager getAuthManager()
	{
		assert authController != null : "authController MUST be instanciated before a client can ask for a reference onto it.";

		return authController;
	}





/*
	LicenseInstaller getLicenseInstaller()
	{
		assert licenseExecutor != null : "licenseExecutor MUST be instanciated and running before a client can ask for a reference onto an LicenseInstaller object.";

		return licenseExecutor;
	}
*/





	SystemEventObserver getSystemEventObserver()
	{
		assert systemEventObserver != null : "systemEventObserver MUST be instanciated and running before a client can ask for a reference onto an LicenseInstaller object.";

		return systemEventObserver;
	}





/*
	CustomerAccountInformation getCustomerAccountInformation()
	{
		if (configurationManager != null)
		{
			return new CustomerAccountInformation(configurationView
				.getInteger(Property.customerGatewayId), configurationView
				.getInteger(Property.customerSiteId), configurationView
				.getString(Property.customerSiteName), configurationView
				.getInteger(Property.customerId), configurationView
				.getString(Property.customerName), configurationView
				.getString(Property.customerGatewayLogin), configurationView
				.getString(Property.customerGatewayPassword));
		}

		return null;
	}

*/




	public String getDistantServicesBaseURL(ConfigurationView view)
	{
		if(view == null)
		{
			view = configurationView;
		}
		
		String distantServicesBaseURL = view.getString(Property.cloud_baseURL);
		if (!distantServicesBaseURL.endsWith("/"))
		{
			distantServicesBaseURL += "/";
		}

		return distantServicesBaseURL;
	}






	@Override
	public void
		configurationUpdate(Enum<?>[] updatedProperties, ConfigurationView view) throws ConfigurationException
	{
		boolean distantServicesBaseURLChanged = view.hasChanged(Property.cloud_baseURL);

		String distantServicesBaseURL = getDistantServicesBaseURL(view);

		if (distantServicesBaseURLChanged)
		{
			logger.info("Immotronic Cloud base URL is now {}.", distantServicesBaseURL);
		}

		if (distantServicesBaseURLChanged
			|| view.hasChanged(Property.cloud_gatewayManagerPath))
		{
			String gatewayManagerURLString = distantServicesBaseURL
				+ view.getString(Property.cloud_gatewayManagerPath);
			URL gatewayManagerURL = null;

			try
			{
				gatewayManagerURL = new URL(gatewayManagerURLString);
			}
			catch (MalformedURLException e)
			{
				throw new ConfigurationException(
					Property.cloud_baseURL.toString()
						+ " | " + Property.cloud_gatewayManagerPath.toString(),
					"Concatenation of these two property values is not a valid URL representation");
			}

			logger.info("Immotronic Cloud gateway manager URL is now {}.", gatewayManagerURLString);

			/*
			if (gatewayManagerAPIConnector == null)
			{
				gatewayManagerAPIConnector = new GatewayAPIConnector(gatewayManagerURL);
				gatewayManagerAPIConnector.setHttpClientService(httpClient);
			}
			else
			{
				gatewayManagerAPIConnector.setServiceEntryPointURL(gatewayManagerURL);
			}*/
		}

		/*
		if (view.hasChanged(Property.customerGatewayLogin)
			|| view.hasChanged(Property.customerGatewayPassword))
		{
			gatewayManagerAPIConnector.setCredentials(
				view.getString(Property.customerGatewayLogin),
				view.getString(Property.customerGatewayPassword));
		}*/

	}
}
