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

import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.BundleContext;

import fr.immotronic.commons.http.tools.HttpRequester;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class WebAPI 
{
	private static URL apiBaseUrl = null;
	private static String extension = null;
	
	public static void init(BundleContext bc)
	{
		try 
		{
			String url = bc.getProperty("fr.immotronic.placetouch.cloudApiUrl");
			if(url == null || !url.endsWith("/"))
			{
				url = url + "/";
			}
			
			apiBaseUrl = new URL(url);
		} 
		catch (MalformedURLException e) 
		{
			throw new IllegalArgumentException("In the config.properties file, 'fr.immotronic.placetouch.cloudApiUrl' MUST be defined with a valid URL");
		}
		
		extension = bc.getProperty("fr.immotronic.placetouch.cloudApiExtension");
		if(extension == null) {
			extension = "";
		}
	}
	
	
	
	public static String getApiBaseURL()
	{
		if(apiBaseUrl != null)
		{
			return apiBaseUrl.toString();
		}
		
		throw new RuntimeException("WebAPI has NOT been initialized. "
			+ "Call WebAPI.init(BundleContext) static function in AppLauncher.");
	}
	
	
	
	public static class GatewayManager
	{
		public enum v1_0
		{
			registerGateway(new HttpRequester(
				HttpRequester.Method.POST,
				"$1gateway-manager/gateway-api/register-gateway",
				"application/vnd.immotronic.evolugreen.account-info-v1+json",
				"application/vnd.immotronic.evolugreen.gateway-info-v1+json"
				)),
			
			acitvateApplication(new HttpRequester(
				HttpRequester.Method.POST,
				"$1gateway-manager/gateway-api/activate-app/$2",
				null,
				"application/vnd.immotronic.evolugreen.gateway-id-v1+json"
				));
			
			
			
			
			private final HttpRequester requester;
			
			private v1_0(HttpRequester requester)
			{
				this.requester = requester;
			}
			
			public HttpRequester getRequester()
			{
				return requester;
			}
		}
	}
	
	
	
	
	public static class DDNS
	{
		private static final String defaultCharset=";charset=utf-8";
		
		public enum v1_0
		{
			dns("application/vnd.immotronic.dns.registration-v1", null, "ddns/dns"),
			visibilitycheck(null, "application/vnd.immotronic.dns.visibility-v1", "ddns/visibilitycheck");
			
			private final String requestContentType;
			private final String responseContentType;
			private final String relativeUrl;
			
			v1_0(String requestContentType, String responseContentType, String relativeUrl)
			{
				this.requestContentType = requestContentType;
				this.responseContentType = responseContentType;
				this.relativeUrl = relativeUrl;
			}
			
			public String getRequestContentType()
			{
				return requestContentType+defaultCharset;
			}
			
			public String getResponseContentType()
			{
				return responseContentType+defaultCharset;
			}
			
			public URL getUrl()
			{
				assert extension != null : "WebAPI has NOT been initialized. Call WebAPI.init(BundleContext) static function in AppLauncher.";
				
				try 
				{
					return new URL(apiBaseUrl, relativeUrl + extension);
				} 
				catch (MalformedURLException e) 
				{
					throw new IllegalArgumentException("API URL for "+relativeUrl+" function is not valid. Maybe check the 'fr.immotronic.placetouch.cloudApiExtension' property in config.properties file.");
				}
			}
		}
		
		public enum v1_1
		{
			register("application/vnd.immotronic.ddns.registration-v1.1", null, "ddns/register"),
			isReachable(null, "application/vnd.immotronic.ddns.reachability-v1", "ddns/is-reachable");
			
			private final String requestContentType;
			private final String responseContentType;
			private final String relativeUrl;
			
			v1_1(String requestContentType, String responseContentType, String relativeUrl)
			{
				this.requestContentType = requestContentType;
				this.responseContentType = responseContentType;
				this.relativeUrl = relativeUrl;
			}
			
			public String getRequestContentType()
			{
				return requestContentType+defaultCharset;
			}
			
			public String getResponseContentType()
			{
				return responseContentType+defaultCharset;
			}
			
			public URL getUrl()
			{
				assert extension != null : "WebAPI has NOT been initialized. Call WebAPI.init(BundleContext) static function in AppLauncher.";
				
				try 
				{
					return new URL(apiBaseUrl, relativeUrl + extension);
				} 
				catch (MalformedURLException e) 
				{
					throw new IllegalArgumentException("API URL for "+relativeUrl+" function is not valid. Maybe check the 'fr.immotronic.placetouch.cloudApiExtension' property in config.properties file.");
				}
			}
		}
		
		public enum v1_2
		{
			register("application/vnd.immotronic.ddns.registration-v1.2", null, "ddns/register"),
			isReachable(null, "application/vnd.immotronic.ddns.reachability-v1.1", "ddns/is-reachable"),
			tunnel("application/vnd.immotronic.ddns.tunnel-info-v1", null, "ddns/tunnel");
			
			private final String requestContentType;
			private final String responseContentType;
			private final String relativeUrl;
			
			v1_2(String requestContentType, String responseContentType, String relativeUrl)
			{
				this.requestContentType = requestContentType;
				this.responseContentType = responseContentType;
				this.relativeUrl = relativeUrl;
			}
			
			public String getRequestContentType()
			{
				return requestContentType+defaultCharset;
			}
			
			public String getResponseContentType()
			{
				return responseContentType+defaultCharset;
			}
			
			public URL getUrl()
			{
				assert extension != null : "WebAPI has NOT been initialized. Call WebAPI.init(BundleContext) static function in AppLauncher.";
				
				try 
				{
					return new URL(apiBaseUrl, relativeUrl + extension);
				} 
				catch (MalformedURLException e) 
				{
					throw new IllegalArgumentException("API URL for "+relativeUrl+" function is not valid. Maybe check the 'fr.immotronic.placetouch.cloudApiExtension' property in config.properties file.");
				}
			}
		}
	}
	
	public static class UpgradeServer
	{
		private static final String defaultCharset=";charset=utf-8";
		
		public enum v1_0
		{
			download(null, "application/zip", "upgrade/download"),
			upgrade_availability(null, "text/plain", "upgrade/upgrade-availability");
			
			private final String requestContentType;
			private final String responseContentType;
			private final String relativeUrl;
			
			v1_0(String requestContentType, String responseContentType, String relativeUrl)
			{
				this.requestContentType = requestContentType;
				this.responseContentType = responseContentType;
				this.relativeUrl = relativeUrl;
			}
			
			public String getRequestContentType()
			{
				return requestContentType+defaultCharset;
			}
			
			public String getResponseContentType()
			{
				return responseContentType+defaultCharset;
			}
			
			public URL getUrl()
			{
				assert extension != null : "WebAPI has NOT been initialized. Call WebAPI.init(BundleContext) static function in AppLauncher.";
				try 
				{
					return new URL(apiBaseUrl, relativeUrl + extension);
				} 
				catch (MalformedURLException e) 
				{
					throw new IllegalArgumentException("API URL for "+relativeUrl+" function is not valid. Maybe check the 'fr.immotronic.placetouch.cloudApiExtension' property in config.properties file.");
				}
			}
		}
	}
}
