
var Home = {
		
	init: function() 
	{
		
	},
	
	initUI: function()
	{
		Placetouch.hideBackButton();
		
		$("#appsButton").click(function() {
			Placetouch.run("to_apps");
		});
		
		$("#appConfigurationButton").click(function() {
			Placetouch.run("to_appsconfiguration");
		});
		
		$("#systemSettings").click(function() {
			Placetouch.run("to_settings");
		});
		
		if(Appstore.getAppstoreURL() == undefined || Appstore.getAppstoreURL() == "")
		{
			$("#appStore").addClass("deactivated");
		}
		else 
		{
			$("#appStore").removeClass("deactivated");
			$("#appStore").click(function() {
				Placetouch.run("to_appstore");
			});
		}
		
		
		$("#wsnButton").click(function() {
			Placetouch.run("to_wsn");
		});
	}
}