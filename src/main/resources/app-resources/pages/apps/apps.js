var Apps = {
		
	init: function() {
		
	},
	
	initUI: function() {
		
		//Placetouch.update_hash("#to_home", transition_stack);
		Placetouch.showTopBarHelpMessage(cwf.tr("apps_qh"));
		Placetouch.showBackButton(cwf.tr("home"));
		
		// Get the public app list
		cwf.api.get_query("api/apps/public", {
			success: function(data) {
				var apps = data.apps;
				for(var i in apps) 
				{
					Apps.__displayAppIcon(apps[i]);
				}
			}
		});
	},
	
	/*run: function(transition_stack) {
			
	},*/
	
	quit: function() {
		Placetouch.hideTopBarHelpMessage();
	},
	
	__displayAppIcon: function(app)
	{
		if(app.status != "PAUSED") {
			$("#publicapps").append("<div class=\"appicon\"><a href=\"/"+app.uid+"/\"><img src=\"api/apps/icon/"+app.uid+".png\"/><br/>"+app.name+"</a></div>");
		}
		else {
			$("#publicapps").append("<div class=\"appicon paused\"><img src=\"api/apps/icon/"+app.uid+".png\"/>"+app.name+"</div>");
		}
	}
}