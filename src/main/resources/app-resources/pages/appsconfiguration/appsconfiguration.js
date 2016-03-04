
var AppsConfiguration = {
		
	init: function() {
		
	},
	
	initUI: function() {
		
		//Placetouch.update_hash("#to_home", transition_stack);
		Placetouch.showTopBarHelpMessage(cwf.tr("appsconfiguration_qh"));
		Placetouch.showBackButton(cwf.tr("home"));
		
		$("#closeButton").click(function() {
			AppsConfiguration.__closeAppMenu();
		});
		
		// Get the public app list
		AppsConfiguration.__displayAppIcons();
		
	},
	
	/*run: function(transition_stack) {
			
	},*/
	
	quit: function() {
		Placetouch.hideTopBarHelpMessage();
	},
	
	__displayAppIcons: function()
	{
		cwf.api.get_query("api/apps/all", {
			success: function(data) {
				var apps = data.apps;
				$("#appsconfiguration").html("");
				for(var i in apps) 
				{
					AppsConfiguration.__displayAppIcon(apps[i]);
				}
			}
		});
	},
	
	__displayAppIcon: function(app)
	{
		var appCSS_ID = app.uid.replace("/", "-");
		$("#appsconfiguration").append("<div class=\"appicon\" id=\""+appCSS_ID+"\"><img src=\"api/apps/icon/"+app.uid+".png\"/><br/>"+app.name+"</div>");
		$("#appsconfiguration #"+appCSS_ID).click(function() {
			AppsConfiguration.__displayAppMenu(app.uid);
		});
	},
	
	__displayAppMenu: function(appUID)
	{
		$("#buttons .button").removeClass("deactivated");
		$("#buttonBar .confirmationbox").hide();
		$("#bundleOperationError").hide();
		
		$("#appBigIcon").attr("src", "api/apps/icon/big/"+appUID);
		cwf.api.get_query("api/apps/appProperties/"+appUID, {
			success: function(data)
			{
				$("#appName").html(data.name);
				$("#version").html(data.version);
				$("#vendor").html(data.vendorName);
				
				switch(data.appStatus)
				{
					case "PAUSED":
						$("#appStatus").removeClass("running");
						$("#appStatus").addClass("paused");
						$("#appStatus").html(cwf.tr("paused"));
						$(".label[tr=uptime]").html(cwf.tr("downtime"));
						if(data.downtime == -1) {
							$("#appUptime").html("unknown");
						}
						else {
							$("#appUptime").html(duration_to_string(data.downtime / 1000));
						}
						
						$("#buttons #start").off("click");
						$("#buttons #start").on("click", function() {
							$("#buttons #uninstall").addClass("deactivated");
							AppsConfiguration.__displayConfirmationButtons(appUID, "start", {
								success: function(data)
								{
									$("#buttons #uninstall").removeClass("deactivated");
									AppsConfiguration.__displayAppMenu(appUID);
								},
								canceled: function()
								{
									$("#buttons #uninstall").removeClass("deactivated");
								},
								failure: function(appUID, reason, errorCode)
								{
									$("#bundleOperationError").html(cwf.tr("cannotStart"));
									$("#bundleOperationError").show();
									AppsConfiguration.__closeAppMenu();
									AppsConfiguration.__displayAppIcons();
								}
							});
						});
						
						$("#buttons #uninstall").off("click");
						$("#buttons #uninstall").on("click", function() {
							$("#buttons #start").addClass("deactivated");
							AppsConfiguration.__displayConfirmationButtons(appUID, "uninstall", {
								success: function(data)
								{
									AppsConfiguration.__closeAppMenu();
									AppsConfiguration.__displayAppIcons();
								},
								canceled: function()
								{
									$("#buttons #start").removeClass("deactivated");
								},
								failure: function(appUID, reason, errorCode)
								{
									$("#bundleOperationError").html(cwf.tr("cannotUninstall"));
									$("#bundleOperationError").show();
									AppsConfiguration.__closeAppMenu();
									AppsConfiguration.__displayAppIcons();
								}
							});
						});
						
						$("#buttons #stop").hide();
						$("#buttons #configure").hide();
						$("#buttons #start").show();
						$("#buttons #uninstall").show();
						break;
						
					case "RUNNING":
						$("#appStatus").removeClass("paused");
						$("#appStatus").addClass("running");
						$("#appStatus").html(cwf.tr("running"));
						$(".label[tr=uptime]").html(cwf.tr("uptime"));
						$("#appUptime").html(duration_to_string(data.uptime / 1000));
						
						$("#buttons #stop").off("click");
						$("#buttons #stop").on("click", function() {
							AppsConfiguration.__displayConfirmationButtons(appUID, "stop", {
								success: function(data)
								{
									AppsConfiguration.__displayAppMenu(appUID);
								},
								failure: function(appUID, reason, errorCode)
								{
									$("#bundleOperationError").html(cwf.tr("cannotStop"));
									$("#bundleOperationError").show();
									AppsConfiguration.__closeAppMenu();
									AppsConfiguration.__displayAppIcons();
								}
							});
						});
						
						$("#buttons #configure").off("click");
						$("#buttons #configure").on("click", function() {
							document.location = "/"+appUID+"/configuration/index.html";
						});
						
						$("#buttons #stop").show();
						$("#buttons #configure").show();
						$("#buttons #start").hide();
						$("#buttons #uninstall").hide();
						break;
				}
				
				$("#appInfoPanelBackground").show();
				$("#appInfoPanel").show();
			}
		});
	},
	
	__displayConfirmationButtons: function(appUID, action, actionResponseHandler) 
	{
		if(!$("#buttons #"+action).hasClass("deactivated"))
		{
			$("#buttons #"+action).addClass("deactivated");
			$("#buttonBar .confirmationbox>#action").val(cwf.tr(action));
			$("#buttonBar .confirmationbox>#action").off("click");
			$("#buttonBar .confirmationbox>#action").on("click", function() {
				$("#buttonBar .confirmationbox").hide();
				$("#buttons #"+action).removeClass("deactivated");
				cwf.api.query("api/apps/"+action, { appUID : appUID} , {
					success: function(data)
					{
						if(actionResponseHandler != null && actionResponseHandler.success != null) {
							actionResponseHandler.success(appUID);
						}
					},
					
					failed: function(reason, errorCode)
					{
						cwf.log("Bundle operation failed: '"+reason+"' ("+errorCode+")");
						if(actionResponseHandler != null && actionResponseHandler.failure != null) {
							actionResponseHandler.failure(appUID, reason, errorCode);
						}
					},
					
					error: function(errorCode, error, message)
					{
						cwf.log("Bundle operation crashed: '"+message+"' ("+errorCode+")");
						if(actionResponseHandler != null && actionResponseHandler.error != null) {
							actionResponseHandler.error(appUID, errorCode, message);
						}
					}
				},
				AppsConfiguration.__ui_feedback);
			});
			
			$("#buttonBar .confirmationbox>#cancel").off("click");
			$("#buttonBar .confirmationbox>#cancel").on("click", function() {
				if(actionResponseHandler != null && actionResponseHandler.canceled != null) {
					actionResponseHandler.canceled(appUID);
				}
				$("#buttons #"+action).removeClass("deactivated");
				$("#buttonBar .confirmationbox").hide();
			});
			
			$("#buttonBar .confirmationbox").show();
		}
	},
	
	__closeAppMenu: function()
	{
		$("#appInfoPanel").hide();
		$("#appInfoPanelBackground").hide();
	},
	
	__ui_feedback : {
		to_hide : [ "#appInfoPanel #buttons" ],
		to_show : [ "#appInfoPanel IMG#loader" ]
	}
}