var AppstoreHome = {
		init: function()
		{
			
		},
		
		initUI: function() 
		{	
			Appstore.updateTrail([{
				text: "home"
			}]);
			
			if(Appstore.__lookNfeel != null)
			{
				if(Appstore.__lookNfeel.title != undefined)
				{
					$("#bannerTitle").html(Appstore.__lookNfeel.title);
				}
				
				if(Appstore.__lookNfeel.slideshow != undefined && Appstore.__lookNfeel.slideshow.nbOfImages > 0)
				{
					Appstore.displaySlideshow("#slideshow", Appstore.__lookNfeel.slideshow.nbOfImages, Appstore.__lookNfeel.slideshow.imageCanonicalName, Appstore.__lookNfeel.slideshow.imageExtension);
					if(Appstore.__lookNfeel.slideshow.nbOfImages > 1) {
						AppstoreHome.__slideshow_timer = Appstore.slideshow();
					}
				}
			}
			
			cwf.api.crossdomain_get_query(Appstore.getAppstoreURL()+"categories.php", {
				success: function(data)
				{
					if(data.categories.length != 0) {
						Appstore.displayCategories("#categories", data.categories);
					}
				},
				
				error: function(errorCode)
				{
					Appstore.displayNetworkErrorMessage(errorCode);
				}
			});
			
			cwf.api.crossdomain_get_query(Appstore.getAppstoreURL()+"top-charts.php", {
				success: function(data)
				{
					Appstore.displayApps("#topApps", data.apps);
				},
				
				error: function(errorCode)
				{
					Appstore.displayNetworkErrorMessage(errorCode);
				}
			})
		}, 
		
		run: function(transition)
		{

		},
		
		quit: function()
		{
			clearInterval(AppstoreHome.__slideshow_timer);
		},
		
		__slideshow_timer: null,
}