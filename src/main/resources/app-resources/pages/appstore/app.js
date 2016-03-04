var AppstoreApp = {
		init: function()
		{
			
		},
		
		initUI: function(data) 
		{
			var vendorUID = data[0];
			var appUID = data[1];
			cwf.api.crossdomain_get_query(Appstore.getAppstoreURL()+"vendors/"+vendorUID+"/"+appUID+".php", {
				success: function(data)
				{
					$("#appIcon").attr("src", Appstore.getAppstoreURL()+data.iconURL);
					
					Appstore.updateTrail([
	      			    {
	      			    	id: "home",
	      			    	text: "home",
	      			    	click: function() {
	      			    		Appstore.run("to_home");
	      			    	}
	      			    },
	      			    {
	      			    	id: vendorUID,
	      			    	text: data.vendorName,
	      			    	click: function() {
	      			    		Appstore.run("to_vendor="+vendorUID);
	      			    	}
	      			    },
	      			    {
	      			    	text: data.appName
	      			    }
	      			]);
					
					$("#appName").html(data.appName);
					
					$(".vendor").html(data.vendorName);
					$(".vendor").click(function() {
						Appstore.run("to_vendor="+vendorUID);
					});
					
					$(".category").html(data.categoryName);
					$("#description").html(data.description);
					$("#updated").html(data.lastUpdate);
					$("#version").html(data.version);
					$("#languages").html(data.languages);
					$("#placetouchVersion").html(data.placetouchVersion);
					
					AppstoreApp.__displayRequirements("#sensors>TABLE", data.sensors);
					AppstoreApp.__displayRequirements("#actuators>TABLE", data.actuators);
					AppstoreApp.__displayScreenShots("#screenshots>DIV", data.screenshotURLs);
					
					var appURL = Appstore.getAppstoreURL()+data.appURL;
					var appID = data.appID;
					
					cwf.api.get_query("api/apps/checkAppInstallation/"+vendorUID+"/"+appUID, {
						success: function(data)
						{
							if(data.appStatus == "installed") {
								$("#installButton").html("<span class=\"button big green deactivated\">"+cwf.tr("installed")+"</span>");
							}
							else {
								$("#installButton").html("<span class=\"button big white\">"+cwf.tr("installit")+"</span>");
								$("#installButton>SPAN").click(function() {
									cwf.log("appURL="+appURL);
									cwf.api.query("api/apps/install", { appUrl : appURL, vendorUID : vendorUID, appID : appID }, {
										success: function() 
										{
											$("#installButton").html("<span class=\"button big green deactivated\">"+cwf.tr("installed")+"</span>");
											cwf.log("et ho");
										},
										
										failed: function()
										{
											Appstore.displayNetworkErrorMessage(600);
										},
										
										error: function(errorCode, error, helpmsg) {
											Appstore.displayNetworkErrorMessage(errorCode);
										}
									},
									{
										to_hide: [ "DIV#installButton" ],
										to_show: [ "DIV#installLoader" ]
									});
								});
							} 
						}
					});
				},
				
				error: function(errorCode)
				{
					Appstore.displayNetworkErrorMessage(errorCode);
				}
			});
			
			
		}, 
		
		run: function(transition)
		{

		},
		
		quit: function()
		{
			
		},
		
		__displayRequirements: function(containerSel, data)
		{
			$(containerSel).html("<tr><td class=\"titleRow left\">"+cwf.tr("type")+"</td><td class=\"titleRow right\">"+cwf.tr("quantity")+"</td></tr>");
			for(var i in data) {
				var item = data[i];
				$(containerSel).append("<tr><td>"+item.type+"</td><td class=\"quantity\">"+item.quantity+"</td></tr>");
			}
		},
		
		__displayScreenShots: function(containerSel, urls)
		{
			$(containerSel).html("");
			for(var i in urls) {
				$(containerSel).append("<img src=\""+Appstore.getAppstoreURL()+urls[i]+"\">");
			}
		}
}