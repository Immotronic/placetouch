var Settings = {
		init: function()
		{
			
		},
		
		initUI: function() 
		{
			Placetouch.showBackButton(cwf.tr("home"));
			Settings.__refresh_configuration_info();
			
			cwf.ui.error.set_contextual_template("<tr cwf=\"errmsg\" id=\"%message_id\"><td class=\"c_errmsg\" colspan=\"2\">%message</td></tr>");
			
			$("#changePasswordDialog").dialog({
				resizable: false, autoOpen: false, width: "450px", modal: true,
				buttons: [ 
					{ 	
						text: cwf.tr("change"),
						click: function() 
						{
							var error = false;
								
							if($("#username").val() == "")
							{
								$("#errorMessageDialog #errorMessage").html(cwf.tr("noUsername"));
								error = true;
							}
							else if($("#newPassword").val() != $("#newPasswordConf").val())
							{
								$("#errorMessageDialog #errorMessage").html(cwf.tr("newPasswordDoNotMatch"));
								error = true;
							}
							else if($("#newPassword").val().length < 8) {
								$("#errorMessageDialog #errorMessage").html(cwf.tr("newPasswordTooSmall"));
								error = true;
							}
							
							if(error) {
								$("#errorMessageDialog").dialog("open");
							}
							else {
								Settings.__updateUsernameAndPassword($("#username").val(), $("#newPassword").val(), $("#oldPassword").val());
								$(this).dialog( "close" );
							}
							
							$("#newPassword").val("");
							$("#newPasswordConf").val("");
							$("#oldPassword").val("");
						}
					},
						{ 	
							text: cwf.tr("cancel"),
							click: function() {
							$(this).dialog( "close" );
						}
					}]
			});
			
			$( "#errorMessageDialog" ).dialog({ 
				modal: true, autoOpen: false, dialogClass: "errorMessageDialog",
				buttons: [
				          { 	text: cwf.tr("close"),
		                  	click: function() {
		                  		$(this).dialog( "close" );
		                  	}
		                  }
		              ]
			});
			
			$("BUTTON#changeUsernameAndPassword").click(function() {
				$("#changePasswordDialog").dialog("open");
			});
			
			$("#configureWifiDialog").dialog({
				resizable: false, autoOpen: false, width: "450px", modal: true,
				buttons: [ 
					{ 	
						text: cwf.tr("connect"),
						click: function() 
						{
							var error = false;
								
							if($("#newSSID").val() == "")
							{
								$("#errorMessageDialog #errorMessage").html(cwf.tr("noSSID"));
								error = true;
							}
							else if($("#newPassphrase").val() == "")
							{
								$("#errorMessageDialog #errorMessage").html(cwf.tr("noPassPhrase"));
								error = true;
							}
							
							if(error) {
								$("#errorMessageDialog").dialog("open");
							}
							else {
								Settings.__configureWifi($("#newWifiSecurity").val(), $("#newSSID").val(), $("#newPassphrase").val(), $("#hiddenSSID").is(":checked"));
								$(this).dialog( "close" );
							}
						}
					},
						{ 	
							text: cwf.tr("cancel"),
							click: function() {
							$(this).dialog( "close" );
						}
					}]
			});
			
			$("BUTTON#configureWifi").click(function() {
				$("#configureWifiDialog").dialog("open");
			});
						
			$("#forgetWifiConfigurationDialog").dialog({
				resizable: false, autoOpen: false, width: "450px", modal: true,
				buttons: [ 
					{ 	
						text: cwf.tr("forget"),
						click: function() 
						{
							cwf.api.raw_query(
								"api/system/wifi",
								"application/vnd.immotronic.placetouch.wificonfig-v1;charset=utf-8",
								{ remove: true },
								{
									success: function() {
										location.reload(true);
									}
								});
						}
					},
						{ 	
							text: cwf.tr("cancel"),
							click: function() {
							$(this).dialog( "close" );
						}
					}]
			});
			
			$("BUTTON#forgetWifiConfiguration").click(function() {
				$("#forgetWifiConfigurationDialog").dialog("open");
			});
			
			$("#setSystemTimeDialog").dialog({
				resizable: false, autoOpen: false, width: "300px", modal: true,
				buttons: [ 
					{ 	
						id: "setSystemTimeOkButton",
						text: cwf.tr("change"),
						click: function() 
						{
							var date = $("#date3").val() + "-";
							var lang = cwf.l10n.get_full_language();
							if (lang == "fr-FR") {
								date += $("#date2").val() + "-" + $("#date1").val();
							}
							else {
								date += $("#date1").val() + "-" + $("#date2").val();
							}
							date += " " + $("#time1").val() + ":" + $("#time2").val() + ":" + Date.today().setTimeToNow().toString("ss");
							$(this).dialog( "close" );
							cwf.api.query("api/system/time", { date : date });
						}
					},
						{ 	
							text: cwf.tr("cancel"),
							click: function() {
							$(this).dialog( "close" );
						}
					}]
			});
			
			$(".setTime").change(function() {
				var lang = cwf.l10n.get_full_language();
				var itemID = $(this).attr("id");
				if (itemID == "date1")
				{
					var date1 = parseInt($("#date1").val());
					if (lang == "fr-FR")
					{
						if (isNaN(date1) || date1 < 1 || date1 > 31) 
							$("#date1").css("background-color", "#FF7070");
						else
							$("#date1").removeAttr("style");
					}
					else
					{
						if (isNaN(date1) || date1 < 1 || date1 > 12)
							$("#date1").css("background-color", "#FF7070");
						else 
							$("#date1").removeAttr("style");
					}
					
				}
				else if (itemID == "date2")
				{
					var date2 = parseInt($("#date2").val());
					if (lang == "fr-FR")
					{
						if (isNaN(date2) || date2 < 1 || date2 > 12)
							$("#date2").css("background-color", "#FF7070");
						else
							$("#date2").removeAttr("style");
					}
					else
					{
						if (isNaN(date2) || date2 < 1 || date2 > 31)
							$("#date2").css("background-color", "#FF7070");
						else
							$("#date2").removeAttr("style");
					}
					
				}
				else if (itemID == "date3")
				{
					var year = parseInt($("#date3").val());
					if (isNaN(year) || year < 1970 || year > 2037)
						$("#date3").css("background-color", "#FF7070");
					else
						$("#date3").removeAttr("style");
				}
				else if (itemID == "time1")
				{
					var hh = parseInt($("#time1").val());
					if (isNaN(hh) || hh < 0 || hh > 23)
						$("#time1").css("background-color", "#FF7070");
					else
						$("#time1").removeAttr("style");
				}
				else if (itemID == "time2")
				{
					var mm  = parseInt($("#time2").val());
					if (isNaN(mm) || mm < 0 || mm > 59) 
						$("#time2").css("background-color", "#FF7070");
					else
						$("#time2").removeAttr("style");
				}
				
				$('#setSystemTimeOkButton').removeAttr("disabled").removeClass("ui-state-disabled");
				$(".setTime").each(function() {
					var attr = $(this).attr("style");
					if (typeof attr !== "undefined" && attr !== false) {
						$('#setSystemTimeOkButton').attr("disabled", true).addClass("ui-state-disabled");
						return;
					}
				});
			});
			
			$("BUTTON#setSystemTime").click(function() {
				var d = Date.today().setTimeToNow();
				var lang = cwf.l10n.get_full_language();
				if (lang == "fr-FR") {
					$("#date1").val(d.toString("dd")).removeAttr("style");
					$("#date2").val(d.toString("MM")).removeAttr("style");
				}
				else {
					$("#date1").val(d.toString("MM")).removeAttr("style");
					$("#date2").val(d.toString("dd")).removeAttr("style");
				}
				$("#date3").val(d.toString("yyyy")).removeAttr("style");
				$("#time1").val(d.toString("HH")).removeAttr("style");
				$("#time2").val(d.toString("mm")).removeAttr("style");
				$('#setSystemTimeOkButton').removeAttr("disabled").removeClass("ui-state-disabled");
				$("#setSystemTimeDialog").dialog("open");
			});
			
			$("#settings #language SELECT").val(cwf.l10n.get_full_language());
			$("#settings #language SELECT").change(function() {
				var lg = $("#settings #language SELECT").val();
				$.jStorage.set("placetouch.language", lg);
				cwf.l10n.set_language(lg);
				window.location.reload();
			});
			
			/*$("#settings #externalAccess").change(function() {
				var externalAccess = $("#settings #externalAccess").is(":checked");
				cwf.api.query("api/system/configuration", { isDistantAccessAllowed : externalAccess });
			});*/
			
			$("#settings #NATManagement").change(function() {
				var natManagement = $("#settings #NATManagement").is(":checked");
				cwf.api.query("api/system/configuration", { network_nat_automaticManagement : natManagement });
			});
			
			$("#settings #automaticUpdates").change(function() {
				var automaticUpdates = $("#settings #automaticUpdates").is(":checked");
				cwf.api.query("api/system/configuration", { isAutomaticUpdateEnabled : automaticUpdates });
			});
			
			$("#settings #enableHeartbeat").change(function() {
				var enableHeartbeat = $("#settings #enableHeartbeat").is(":checked");
				cwf.api.query("api/system/configuration", { isHeartBeatFeatureEnabled : enableHeartbeat });
			});
			
			$("#settings #automaticBackup").change(function() {
				var automaticBackup = $("#settings #automaticBackup").is(":checked");
				cwf.api.query("api/system/configuration", { isAutomaticBackupEnabled : automaticBackup });
			});
			
			$("#settings #externalWebServerPort").change(function() {
				$("#settings #externalWebServerPort").addClass("unsaved");
				cwf.ui.error.hide_contextual("externalWebServerPortRow");
				var externalWebServerPort = $("#settings #externalWebServerPort").val();
				if(cwf.ui.error.check.is_int(externalWebServerPort)) {
					cwf.api.query("api/system/configuration", { network_da_externalWebServerPort : externalWebServerPort }, {
						success: function()
						{
							$("#settings #externalWebServerPort").removeClass("unsaved");
						}
					});
				}
				else
				{
					cwf.ui.error.show_contextual("externalWebServerPortRow", "not_an_integer", "after");
				}
			});
			
			$("#settings #externalSSHPort").keypress(function() {
				$("#settings #externalSSHPort").addClass("unsaved");
			});
			
			$("#settings #externalSSHPort").change(function() {
				$("#settings #externalSSHPort").addClass("unsaved");
				cwf.ui.error.hide_contextual("externalSSHPortRow");
				var externalSSHPort = $("#settings #externalSSHPort").val();
				if(cwf.ui.error.check.is_int(externalSSHPort)) {
					cwf.api.query("api/system/configuration", { network_da_externalSSHPort : externalSSHPort }, {
						success: function()
						{
							$("#settings #externalSSHPort").removeClass("unsaved");
						}
					});
				}
				else
				{
					cwf.ui.error.show_contextual("externalSSHPortRow", "not_an_integer", "after");
				}
			});
			
			$("#settings #externalSSHPort").keypress(function() {
				$("#settings #externalSSHPort").addClass("unsaved");
			});
			
			$("#settings #contactEmail").change(function() {
				cwf.ui.error.hide_contextual("contactEmailRow");
				var contactEmail = $("#settings #contactEmail").val();
				if(cwf.ui.error.check.is_email(contactEmail)) {
					cwf.api.query("api/system/configuration", { contactEmail : contactEmail });
				}
				else {
					cwf.ui.error.show_contextual("contactEmailRow", "not_an_email", "after");
				}
				
			});
			
			$("#settings #contactMobilePhone").change(function() {
				var contactMobilePhone = $("#settings #contactMobilePhone").val();
				cwf.api.query("api/system/configuration", { contactMobilePhone : contactMobilePhone });
			});
			
			$("#settings #licenseUID").change(function() {
				var licenseUID = $("#settings #licenseUID").val();
				cwf.api.query("api/system/license", { licenseUID : licenseUID }, {
					success: function(data)
					{
						$("#settings #licenseUIDErrorRow").slideUp();
						$("#settings #licenseType").html(data.licenseType);
						Settings.__refreshCustomerAccountUI(data);
						Appstore.setAppstoreURL(data.appstoreURL);
					},
					
					failed: function(reason, code)
					{
						Appstore.setAppstoreURL(undefined);
						$("#settings #licenseUIDErrorRow>TD").html(reason);
						$("#settings #licenseUIDErrorRow").slideDown("slow");
						Settings.__refreshCustomerAccountUI(null);
					}
				});
			});
			
			$("BUTTON#displayDiagnosticsUI").click(function() {
				document.location = "#to_diagnostics/";
			});
			
			$("BUTTON#systemShutdown").click(function() {
				cwf.api.raw_query("api/system/general-shutdown", "", "", {
					success: function(data)
					{
						if(data.lastError != undefined && data.lastError != "NO_ERROR") 
						{
							
						}
						else
						{
							$("#workspace>#content").html("<h1>"+cwf.tr("unpluggedTheBoxWhenLEDsAreOff")+"</h1>");
						}
					},
					error: function(errorCode, error, message)
					{
						//$("#upgradeRebootPanel .message").addClass("error");
						cwf.log("POST general-shutdown "+errorCode+" "+message);
					}
				});
			});
			
			$("BUTTON#systemReboot").click(function() {
				cwf.api.raw_query("api/system/general-reboot", "", "", {
					success: function(data)
					{
						if(data.lastError != undefined && data.lastError != "NO_ERROR") 
						{
							
						}
						else
						{
							$("#workspace>#content").html("<h1>"+cwf.tr("systemIsRebooting")+"</h1>");
						}
					},
					error: function(errorCode, error, message)
					{
						//$("#upgradeRebootPanel .message").addClass("error");
						cwf.log("POST general-reboot "+errorCode+" "+message);
					}
				});
			});
		}, 
		
		run: function(transition)
		{

		},
		
		quit: function()
		{

		},
		
		__refresh_configuration_info: function()
		{
			cwf.api.get_query("api/system/properties", {
	    		success: function(data) {
	    			
	    			$("#settings #name").html(data.hostname);
	    			$("#settings #ipAddresses").html(((data.hostaddresses != undefined)?data.hostaddresses.replace("\n", "<br/>"):""));
	    			//$("#settings #internetAccess").html(cwf.tr(((data.internet == "CONNECTED")?cwf.tr("yes"):cwf.tr("no"))));
	    			$("#settings #internetAccess").html(cwf.tr(data.internet));
	    			$("#settings #ethernetAddresses").html(((data.ethernetAddresses != undefined)?data.ethernetAddresses.replace("_", "<br/>"):""));
	    			//$("#settings #natTranslation").html(cwf.tr(((data.nat == "ESTABLISHED")?cwf.tr("yes"):cwf.tr("no"))));
	    			$("#settings #natTranslation").html(cwf.tr(data.nat));
	    			$("#settings #router").html(data.routerReferences);
	    			$("#settings #externalAddress").html(data.externalIP);
	    			//$("#settings #domainValidity").html(cwf.tr(((data.domainValidity == "DOMAIN_LINKED")?cwf.tr("yes"):cwf.tr("no"))));
	    			$("#settings #domainValidity").html(cwf.tr(data.domainValidity));
	    			$("#settings #uptime").html(duration_to_string(data.uptime / 1000));
	    			$("#settings #version").html(data.placetouchVersion);
	    			$("#settings #buildDate").html(data.buildDate);
	    			$("#settings #licenseUID").val(data.licenseUID);
	    			$("#settings #licenseType").html(data.licenseType);
	    			
	    		}
			});
			
			cwf.api.get_query("api/system/configuration", {
	    		success: function(data) {
	    			
	    			$("#settings #externalAccess").attr("checked", data.isDistantAccessAllowed == "true");
	    			$("#settings #NATManagement").attr("checked", data.network_nat_automaticManagement == true);
	    			$("#settings #externalWebServerPort").val(data.network_da_externalWebServerPort);
	    			$("#settings #externalSSHPort").val(data.network_da_externalSSHPort);
	    			$("#settings #automaticUpdates").attr("checked", data.isAutomaticUpdateEnabled == "true");
	    			$("#settings #enableHeartbeat").attr("checked", data.isHeartBeatFeatureEnabled == "true");
	    			$("#settings #automaticBackup").attr("checked", data.isAutomaticBackupEnabled == "true");
	    			$("#settings #contactEmail").val(data.contactEmail);
	    			$("#settings #contactMobilePhone").val(data.contactMobilePhone);
	    			$("#changePasswordDialog #username").val(data.username);
	    			
	    			Settings.__refreshCustomerAccountUI(data);
	    		}
			});
			
			cwf.api.get_query("api/system/wifi", {
	    		success: function(data) {
	    			if (data.allowed)
	    			{
	    				$(".embedded").show();
	    				if (typeof data.ssid != "undefined") {
	    					$("#settings #SSID").html(data.ssid);
	    				}
	    				else {
	    					$("#settings #SSIDRaw").hide();
	    				}
	    				
	    				$("#settings #wifiIsUp").html(cwf.tr(((data.isUp)?cwf.tr("yes"):cwf.tr("no"))));
	    			}
	    			else
	    			{
	    				$(".embedded").hide();
	    			}
	    		}
			});
		},
		
		__updateUsernameAndPassword: function(newUsername, newPassword, oldPassword)
		{
			cwf.logdebug("new_username="+newUsername+", new_password="+newPassword+", old_password="+oldPassword);
			
			cwf.api.raw_query(
					"api/system/credentials",
					"application/vnd.immotronic.general.credential-update-v1;charset=utf-8",
					{ new_username: newUsername, new_password: newPassword, old_password: oldPassword },
					{
						success: function() {
							location.reload(true);
						}
					});
		},
		
		__configureWifi: function(wifiSecurity, SSID, passphrase, hiddenSSID)
		{
			cwf.logdebug("wifiSecurity="+wifiSecurity+", SSID="+SSID+", passphrase="+passphrase+", hiddenSSID="+hiddenSSID);
			
			cwf.api.raw_query(
					"api/system/wifi",
					"application/vnd.immotronic.placetouch.wificonfig-v1;charset=utf-8",
					{ security: wifiSecurity, SSID: SSID, passphrase: passphrase, hidden: hiddenSSID },
					{
						success: function() {
							location.reload(true);
						},
						
						error: function(errorCode, error, message)
						{
							location.reload(true);
							$("#errorMessageDialog #errorMessage").html(cwf.tr("cantConnect"));
							$("#errorMessageDialog").dialog("open");
						}
					});
		},
		
		__refreshCustomerAccountUI: function(data)
		{
			if(data != null && data.customerName != null && data.customerName 
					!= undefined && data.customerName != "")
			{
				$("#settings #customerName").html(data.customerName);
				$("#settings #customerSiteName").html(data.customerSiteName);
				$("#settings #customerGatewayId").html(data.customerGatewayId);
			}
			else
			{
				$("#settings #customerName").html("");
				$("#settings #customerSiteName").html("");
				$("#settings #customerGatewayId").html("");
			}
		}
}
