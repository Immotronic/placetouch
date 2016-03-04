var AppstoreVendor = {
		init: function()
		{
			
		},
		
		initUI: function(vendorUID) 
		{
			$("#trail #home").click();
			
			Appstore.updateTrail([
			    {
			    	id: "home",
			    	text: "home",
			    	click: function() {
			    		Appstore.run("to_home");
			    	}
			    },
			    {
			    	id: vendorUID
			    }
			]);
			
			cwf.api.crossdomain_get_query(Appstore.getAppstoreURL()+"vendors/"+vendorUID+"/vendor.php", {
				success: function(data)
				{
					$("#"+vendorUID+">SPAN").html(data.name);
					if(data.logoURL != "") {
						$("#vendorDetails #logo").attr("src", Appstore.getAppstoreURL()+data.logoURL);
					}
					
					$("#vendorDetails #description").append("<h1>"+data.name+"</h1>");
					$("#vendorApps>H1>SPAN").html(data.name);
					$("#vendorDetails #description").append(data.description);
					$("#vendorDetails #description").append("<p><a href=\""+data.URL+"\" target=\"_blank\">"+data.URL+"</a></p>");
					Appstore.displayApps("#vendorApps", data.apps);
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
			
		}
}