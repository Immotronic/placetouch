// # Placetouch UI
// # Author: Lionel Balme, Immotronic

cwf.load_js("pages/home/home.js");
cwf.load_js("pages/apps/apps.js");
cwf.load_js("pages/appsconfiguration/appsconfiguration.js");
cwf.load_js("pages/wsn/wsn.js");
cwf.load_js("pages/diagnostics/diagnostics.js");
cwf.load_js("pages/settings/settings.js");
cwf.load_js("pages/appstore/appstore.js");

//
// General purpose functions
// -------------------------

function duration_to_string(nb_of_seconds)
{
	var days = Math.floor(nb_of_seconds / 86400);
	nb_of_seconds -= days * 86400;
	var hours = Math.floor(nb_of_seconds / 3600);
	nb_of_seconds -= hours * 3600;
	var minutes = Math.floor(nb_of_seconds / 60);
	nb_of_seconds -= minutes * 60;
	var seconds = Math.floor(nb_of_seconds);
	
	var res = "";
	if(days == 1) res = days + " day ";
	if(days > 1) res = days + " days ";
	if(res != "") return res;
	
	if(hours >= 1) res = hours + " hours ";
	if(minutes >= 1) {
		res += minutes + " min ";
		if(hours == 0) res += seconds;
	}
	if(res != "") return res;
	
	return seconds + " seconds";
}

function duration_to_HMS_string(nb_of_seconds)
{
	var hours = Math.floor(nb_of_seconds / 3600);
	nb_of_seconds -= hours * 3600;
	var minutes = Math.floor(nb_of_seconds / 60);
	nb_of_seconds -= minutes * 60;
	var seconds = Math.floor(nb_of_seconds);
	
	var res = "";
	if(hours == 0) { res = ""; }
	else { res = hours + "h"; }
	
	if(minutes == 0 && hours != 0) { res += "00m"; }
	else if(minutes < 10 && hours != 0) { res += "0"+minutes + "m"; }
	else { res += minutes + "m"; }
	
	if(seconds == 0 && (hours != 0 || minutes != 0)) { res += "00s"; }
	else if(seconds < 10 && (hours != 0 || minutes != 0)) { res += "0"+seconds+"s"; }
	else { res += seconds+"s"; }
	
	return res;
}

function displayNetworkErrorMessage(errorCode, optionalHelpMessage, alternateTitle)
{
	var help_msg = "Unknow error. ";
	
	switch(errorCode)
	{
		case "err_internal_error":
		case "err_invalid_query":
			help_msg = cwf.tr(errorCode);
			if(optionalHelpMessage == undefined) {
				optionalHelpMessage = cwf.tr("soundLikeABug");
			}
			break;
		
		case 500:
			help_msg = cwf.tr("error500");
			if(optionalHelpMessage == undefined) {
				optionalHelpMessage = cwf.tr("soundLikeABug");
			}
			break;	
			
		case 404:
			help_msg = cwf.tr("error404");
			if(optionalHelpMessage == undefined) {
				optionalHelpMessage = cwf.tr("soundLikeABug");
			}
			break;
			
		case 0:
			help_msg = cwf.tr("networkError");
			if(optionalHelpMessage == undefined) {
				optionalHelpMessage = cwf.tr("checkNetworkConnection");
			}
			break;
	}
	
	var opt = "";
	if(optionalHelpMessage != undefined) 
	{
		opt ="<div class=\"opthelpmsg\">"+optionalHelpMessage+"</div>";
	}
	
	var title = alternateTitle;
	if(title == undefined) {
		title = cwf.tr("anErrorOccured");
	}
	
	$("#workspace>#content").html("<div class=\"networkError\"><h1>"+
									title+"</h1><div><span class=\"label\">"+
									cwf.tr("errorCode")+"</span>"+errorCode+
									"</div><div class=\"helpmsg\">"+help_msg+"</div>"+opt+"</div>");
}

//
// Placetouch specific functions
// -----------------------------

var Placetouch = {
	
	init: function()
	{
		Placetouch.refreshWatch();
		
		cwf.api.set_default_ui_feedback({
			//to_hide: [ ],
			to_show: [ "#header IMG#defaultLoader"]
		});
		
		cwf.api.set_default_failure_handler(function(reason, code) {
			displayNetworkErrorMessage(code, reason);
		});
		
		cwf.api.set_default_error_handler(function(code, error, help_msg) {
			displayNetworkErrorMessage(code);
		});
		
		cwf.sm.set_default_page_loading_error_handler(function(code) {
			displayNetworkErrorMessage(code);
		});
		
		$("#backbutton").click(function() {
			Placetouch.run("back");
		});
		
		Placetouch.initUpgradeDialog();		
		Home.init();
		Apps.init();
		AppsConfiguration.init();
		Wsn.init();
		Diagnostics.init();
		Settings.init();
		Appstore.init();
		
		cwf.sm.add_state_machine("placetouch", Placetouch.root_fsm);
		
		setInterval(Placetouch.refreshWatch, 500);
		setInterval(Placetouch.__checkHash, 300);
		
		cwf.api.get_query("api/system/properties", {
    		success: function(data) 
    		{
    			$("TITLE").html(data.hostname);
    			
    			$("#placetouchVersion").html(data.placetouchVersion);
    			
    			Placetouch.__update_transition_stack_from_hash(window.location.hash);
    			Placetouch.run();
    		}
    	});
		
		cwf.api.get_query("api/system/upgrade", {
			success: function(data) 
    		{
				if(data.state != "NOTHING_TO_DO" && data.state != "TO_BE_DETERMINE") 
				{
					if(data.state == "UPGRADE_AVAILABLE")
					{
						Placetouch.__availableUpdateVersion = data.version;
					}
					
					$("#upgradeDialog").dialog("open");
					Placetouch.displayUpgradeDialogPanel(data.state);
				}
    		}
		});
	},
	
	run: function(transition)
	{
		if(transition == null) {
			// The transition is empty. Try to get one from the transition stack
			transition = Placetouch.get_transition_from_stack(0); // Get transition at position == 0 (this FSM is root level)
			
			if(transition == null) {
				//  No transition could be gotten from the stack. The FSM is run from its initial state.
				cwf.sm.reinit_state_machine("placetouch");
				cwf.sm.dialog_ctrl("placetouch");
			}
			else {
				// A transition has been retrieved from stack. Execute it.
				cwf.sm.dialog_ctrl("placetouch", transition);
			}
			
		}
		else {
			Placetouch.update_transition_stack(0, transition); // Update stack from position == 0 (this FSM is root level)
			cwf.sm.dialog_ctrl("placetouch", transition);
		}
	},
	
	initUpgradeDialog: function()
	{
		$("#upgradeDialog").dialog({
            resizable: false, autoOpen: false, width: "500px", modal: true, title: cwf.tr("newUpgradeAvailableTitle"),
            buttons: []
		});
		
		$("#downloadUpgradeButton").click(function() {
			Placetouch.displayUpgradeDialogPanel("DOWNLOADING");
			cwf.api.raw_query("api/system/upgrade-download", "application/vnd.immotronic.general.upgrade-download-v1;charset=utf-8", Placetouch.__availableUpdateVersion, {
				success: function(data)
				{
					if(data.lastError != undefined && data.lastError != "NO_ERROR") 
					{
						$("#upgradeDownloadingPanel .message").addClass("error");
						$("#upgradeDownloadingPanel .message").html(cwf.tr(data.lastError));
					}
				},
				error: function(errorCode, error, message)
				{
					$("#upgradeDownloadingPanel .message").addClass("error");
					$("#upgradeDownloadingPanel .message").html("POST upgrade-download "+errorCode+" "+message);
				}
			});
		});
		
		$("#installUpgradeButton").click(function() {
			$("#upgradeInstallationPanel .message").addClass("center-align");
			$("#upgradeInstallationPanel .message").html(cwf.tr("installing")+"<br><img src=\"img/loading_default.gif\" width=\"32\" height=\"32\"/>");
			cwf.api.raw_query("api/system/upgrade-install", "", "", {
				success: function(data)
				{
					if(data.lastError != undefined && data.lastError != "NO_ERROR") 
					{
						$("#upgradeInstallationPanel .message").addClass("error");
						$("#upgradeInstallationPanel .message").html(cwf.tr(data.lastError));
					}
					else
					{
						Placetouch.displayUpgradeDialogPanel("READY_TO_REBOOT");
					}
				},
				error: function(errorCode, error, message)
				{
					$("#upgradeInstallationPanel .message").addClass("error");
					$("#upgradeInstallationPanel .message").html("POST upgrade-install "+errorCode+" "+message);
				}
			});
		});
		
		$("#systemRebootButton").click(function() {
			$("#upgradeRebootPanel .message").addClass("center-align");
			$("#upgradeRebootPanel .message").html(cwf.tr("rebooting")+"<br><img src=\"img/loading_default.gif\" width=\"32\" height=\"32\"/>");
			cwf.api.raw_query("api/system/upgrade-reboot", "", "", {
				success: function(data)
				{
					if(data.lastError != undefined && data.lastError != "NO_ERROR") 
					{
						$("#upgradeRebootPanel .message").addClass("error");
						$("#upgradeRebootPanel .message").html(cwf.tr(data.lastError));
					}
					else
					{
						$("#upgradeRebootPanel .buttons").html("");
						setTimeout(function() {
							window.location.reload(true);
						}, 60000);
					}
				},
				error: function(errorCode, error, message)
				{
					$("#upgradeRebootPanel .message").addClass("error");
					$("#upgradeRebootPanel .message").html("POST upgrade-reboot "+errorCode+" "+message);
				}
			});
		});
		
		$(".closeUpgradeDialogButton").click(function() {
			$("#upgradeDialog").dialog( "close" );
		});
		
		$("#upgradeDownloadProgressbar").progressbar({
		      value: false,
		      change: function() {
		    	  $("#upgradeDownloadProgressbar .progress-label").text($("#upgradeDownloadProgressbar").progressbar("value") + " %" );
		      },
		      complete: function() {
		    	  $("#upgradeDownloadProgressbar .progress-label").text("100 %");
		      }
		});
	},
	
	displayUpgradeDialogPanel: function(state)
	{
		$("#upgradeDialog .panel").hide();
		$("#upgradeDialog .message").removeClass("error");
		$("#upgradeProgressPath SPAN").removeClass("selected");
		switch(state)
		{
			case "UPGRADE_AVAILABLE":
				$("#upgradeProgressPath .newUpgrade").addClass("selected");
				$("#upgradeAvailablePanel").show();
				break;
			case "DOWNLOADING":
				Placetouch.setDownloadProgressObserver();
				$("#upgradeProgressPath .download").addClass("selected");
				$("#upgradeDownloadingPanel").show();
				break;
			case "UPGRADE_READY_TO_INSTALL":
				$("#upgradeProgressPath .installation").addClass("selected");
				$("#upgradeInstallationPanel").show();
				break;
			case "READY_TO_REBOOT":
				$("#upgradeProgressPath .reboot").addClass("selected");
				$("#upgradeRebootPanel").show();
				break;
		}
	},
	
	setDownloadProgressObserver: function()
	{
		Placetouch.__upgradeProgressbarTimer = setInterval(function() {
			cwf.api.get_query("api/system/upgrade", {
				success: function(data) 
	    		{
					if(data.state == "DOWNLOADING" && data.progress != 0) 
					{
						$("#upgradeDownloadProgressbar").progressbar("value", data.progress);
					}
					else if(data.state == "UPGRADE_READY_TO_INSTALL") 
					{
						clearInterval(Placetouch.__upgradeProgressbarTimer);
						Placetouch.displayUpgradeDialogPanel(data.state);
					}
					else if(data.lastError != undefined && data.lastError != "NO_ERROR")
					{
						$("#upgradeDownloadingPanel .message").addClass("error");
						$("#upgradeDownloadingPanel .message").html(cwf.tr(data.lastError));
						clearInterval(Placetouch.__upgradeProgressbarTimer);
					}
	    		},
				error: function(errorCode, error, message)
				{
					$("#upgradeDownloadingPanel .message").addClass("error");
					$("#upgradeDownloadingPanel .message").html("GET upgrade "+errorCode+" "+message);
				}
			});
		}, 500);
	},
    
    refreshWatch: function() 
    {
    	//var h = new Date();
    	//var d = Date.today().addHours(h.getHours()).addMinutes(h.getMinutes());
    	var d = Date.today().setTimeToNow();
    	switch(cwf.l10n.get_language()) {
    		case "en":
    			$("#datetime").html(d.toString("dddd, MMMM ddS, yyyy")+" &nbsp;&nbsp;&nbsp; "+d.toString("hh:mm tt"));
    			break;
    		case "fr":
    			$("#datetime").html(d.toString("dddd dd MMMM yyyy")+" &nbsp;&nbsp;&nbsp; "+d.toString("HH:mm"));
    			break;
    	}
	},
	
	showTopBarHelpMessage: function(message) 
	{
		if(message == null || message == "") {
			$("#helpTopBar").hide();
		}
		else {
			$("#helpTopBar > SPAN").html(message);
			$("#helpTopBar").show();
		}
	},
	
	hideTopBarHelpMessage: function() 
	{
		$("#helpTopBar").hide();
	},
	
	showBackButton: function(text)
	{
		if(text == null || text == "") {
			text = cwf.tr("back");
		}
		
		$("#backbutton > SPAN").html(text);
		$("#backbutton").css({ display: "block" });
	},
	
	hideBackButton: function()
	{
		$("#backbutton").css({ display: "none" });
	},
	
	update_transition_stack: function(position, transition)
	{
		// Remove the current transition and eventual nested FSM transitions 
		Placetouch.__transition_stack.splice(position, Placetouch.__transition_stack.length);
		
		// Pushing the new one into the stack
		Placetouch.__transition_stack.push(transition);
		
		// Updating the location hash to insert a virtual page in the browser history
		var hash = "";
		for(var i in Placetouch.__transition_stack) 
		{
			hash += Placetouch.__transition_stack[i] + "/";  
		}
		
		if(hash != "") {
			Placetouch.__location_hash  = "#"+hash;
			window.location.hash = hash;
		}
	},
	
	get_transition_from_stack: function(position)
	{
		return Placetouch.__transition_stack[position];
	},
	
	registerPemControlUIHandler: function(pemUID, handler)
	{
		Placetouch.__pemControlUIHandlers[pemUID] = handler
	},
	
	getPemControlUIHandler: function(pemUID)
	{
		return Placetouch.__pemControlUIHandlers[pemUID];
	},
	
	registerPemConfigurationUIHandler: function(pemUID, handler)
	{
		Placetouch.__pemConfigurationUIHandlers[pemUID] = handler
	},
	
	getPemConfigurationUIHandler: function(pemUID)
	{
		return Placetouch.__pemConfigurationUIHandlers[pemUID];
	},
	
	registerPemAddingUIHandler: function(pemUID, handler)
	{
		Placetouch.__pemAddingUIHandlers[pemUID] = handler
	},
	
	getPemAddingUIHandler: function(pemUID)
	{
		return Placetouch.__pemAddingUIHandlers[pemUID];
	},
	
	root_fsm: {
		element_sel: "#workspace #content",
		init:	"home",
		home:
		{	
			__pre:	function(data) // here, data are a transition_stack
			{
				//cwf.ui.menu.select_item("menu", "home");
			},
			
			__post: function(data, loading_ok) // here, data are a transition_stack
			{ 
				if(loading_ok) {
					Home.initUI();
				}
			},
			
			__leaving: function(transition)
			{
				//Home.quit();
				return true;
			},
			
			to_apps: "apps",
			to_appsconfiguration: "appsconfiguration",
			to_wsn: "wsn",
			to_settings: "settings",
			to_appstore: "appstore",
			to_diagnostics: "diagnostics",
			back: "home"
		},
		
		apps:
		{
			__pre:	function(data) // here, data are a transition_stack
			{ },
			
			__post: function(data, loading_ok) // here, data are a transition_stack
			{
				if(loading_ok) {
					Apps.initUI();
				}
			},
			
			__leaving: function(transition)
			{
				Apps.quit();
				return true;
			},
			
			back: "home"
		},
		
		appsconfiguration:
		{
			__pre:	function(data) // here, data are a transition_stack
			{ },
			
			__post: function(data, loading_ok) // here, data are a transition_stack
			{
				if(loading_ok) {
					AppsConfiguration.initUI();
				}
			},
			
			__leaving: function(transition)
			{
				AppsConfiguration.quit();
				return true;
			},
			
			back: "home"
		},
		
		wsn:
		{
			__pre:	function(data) // here, data are a transition_stack
			{ },
			
			__post: function(data, loading_ok) // here, data are a transition_stack
			{
				if(loading_ok) {
					Wsn.initUI();
					Wsn.run(data);
				}
			},
			
			__leaving: function(transition)
			{
				Wsn.quit();
				return true;
			},
			
			back: "home"
		},
		
		diagnostics:
		{
			__pre:	function(data) // here, data are a transition_stack
			{ },
			
			__post: function(data, loading_ok) // here, data are a transition_stack
			{
				if(loading_ok) {
					Diagnostics.initUI();
					Diagnostics.run(data);
				}
			},
			
			__leaving: function(transition)
			{
				Diagnostics.quit();
				return true;
			},
			
			back: "home",
			to_settings: "settings"
		},
		
		settings:
		{
			__pre:	function(data) // here, data are a transition_stack
			{ },
			
			__post: function(data, loading_ok) // here, data are a transition_stack
			{
				if(loading_ok) {
					Settings.initUI();
					Settings.run(data);
				}
			},
			
			__leaving: function(transition)
			{
				Settings.quit();
				return true;
			},
			
			to_diagnostics: "diagnostics",
			back: "home"
		},
		
		appstore:
		{	
			__pre:	function(data) // here, data are a transition_stack
			{  },
			
			__post: function(data, loading_ok) // here, data are a transition_stack
			{ 
				if(loading_ok) {
					Appstore.initUI();
					Appstore.run(data);
				}
			},
			
			__leaving: function(transition)
			{
				Appstore.quit();
				return true;
			},
			
			to_appstore: "appstore",
			back: "home"
		}
	},
	
	__checkHash: function() 
	{
		if(Placetouch.__location_hash != null && window.location.hash != Placetouch.__location_hash) 
		{		
			Placetouch.__update_transition_stack_from_hash(window.location.hash);
			Placetouch.run();
		}
	},
	
	__update_transition_stack_from_hash: function(hash)
	{
		// Update the hash cache
		Placetouch.__location_hash = hash;
		
		// Extract a transition stack from this new hash
		Placetouch.__transition_stack = Placetouch.__location_hash.substr(1).split("/");
		
		// Removing the last empty element. (This element exist because a valid hash ends with a "/" char)
		Placetouch.__transition_stack.pop();
	},
	
	__location_hash: null, 
	
	__transition_stack: [],
	
	__pemControlUIHandlers: {},
	__pemConfigurationUIHandlers: {},
	__pemAddingUIHandlers: {},
	__upgradeProgressbarTimer: null,
	__availableUpdateVersion: null
}


// Start the main dialog controller
$(document).ready(function() {
	//cwf.set_debug();
	
	var lg = $.jStorage.get("placetouch.language");
	if(lg === undefined || lg === null || lg === "") {
		cwf.l10n.set_language("fr-FR");
		if(navigator.language.substr(0,2) != "fr") {
			cwf.l10n.set_language("en-US");
		}
	}
	else {
		cwf.l10n.set_language(lg);
	}
	
	cwf.l10n.load_date_lib("lib/");
	cwf.l10n.load_tr("main", function() {
		Placetouch.init();
	});
})
