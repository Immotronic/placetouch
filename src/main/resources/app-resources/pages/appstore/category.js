var AppstoreCategory = {
		init: function()
		{
			
		},
		
		initUI: function(categoryUID) 
		{
			AppstoreCategory.__slideshow_timer = Appstore.slideshow();
			
			Appstore.updateTrail([
			      			    {
			      			    	id: "home",
			      			    	text: "home",
			      			    	click: function() {
			      			    		Appstore.run("to_home");
			      			    	}
			      			    },
			      			    {
			      			    	id: categoryUID
			      			    }
			      			]);
			
			cwf.api.crossdomain_get_query(Appstore.getAppstoreURL()+"categories.php", {
				success: function(data)
				{
					Appstore.displayCategories("#categories", data.categories);
				},
				
				error: function(errorCode)
				{
					Appstore.displayNetworkErrorMessage(errorCode);
				}
			});
			
			cwf.api.crossdomain_get_query(Appstore.getAppstoreURL()+"categories/"+categoryUID+".php", {
				success: function(data)
				{
					$("#"+categoryUID+">SPAN").html(data.name);
					$("#catApps H1").html(data.name);
					Appstore.displayApps("#catApps", data.apps);
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
			clearInterval(AppstoreCategory.__slideshow_timer);
		},
		
		__slideshow_timer: null,
}