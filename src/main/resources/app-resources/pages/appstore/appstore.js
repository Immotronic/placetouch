cwf.load_js("pages/appstore/home.js");
cwf.load_js("pages/appstore/category.js");
cwf.load_js("pages/appstore/app.js");
cwf.load_js("pages/appstore/vendor.js");

var Appstore = {
		init: function()
		{
			AppstoreHome.init();
			AppstoreCategory.init();
			AppstoreApp.init();
			AppstoreVendor.init();
			
			cwf.api.get_query("api/system/appstoreURL", {
				success: function(data) 
				{
					Appstore.__appstoreURL = data.appstoreURL;
					cwf.api.crossdomain_get_query(Appstore.getAppstoreURL()+"look-n-feel.php", {
						success: function(data)
						{
							Appstore.__lookNfeel = data;
						}
					});
				},
				
				error: function() {
					Appstore.__lookNfeel = null;
				}
			});			
			
			cwf.sm.add_state_machine("appstore", Appstore.fsm);
			
		},
		
		initUI: function()
		{
			if(Appstore.__lookNfeel != null && Appstore.__lookNfeel.style != undefined) {
				cwf.load_css(Appstore.getAppstoreURL()+Appstore.__lookNfeel.style);
			}
			
			Placetouch.showBackButton(cwf.tr("home"));
			
			$("#appstoreHeader IMG").click(function() {	
				Appstore.run("to_home");
			});
		},
		
		run: function(transition)
		{
			if(transition == null) {
				// The transition is empty. Try to get one from the transition stack
				transition = Placetouch.get_transition_from_stack(1); // Get transition at position == 1 (this FSM is nested is a state of the main FSM)
				
				if(transition == null) {
					//  No transition could be gotten from the stack. The FSM is run from its initial state.
					cwf.sm.reinit_state_machine("appstore");
					cwf.sm.dialog_ctrl("appstore");
				}
				else {
					// A transition has been retrieved from stack. Execute it.
					var params = transition.split("=");
					var trans = params.splice(0, 1);
					cwf.sm.dialog_ctrl("appstore", trans[0], params);
				}
				
			}
			else {
				Placetouch.update_transition_stack(1, transition); // Update stack from position == 1 (this FSM is nested is a state of the main FSM)
				var params = transition.split("=");
				var trans = params.splice(0, 1);
				cwf.sm.dialog_ctrl("appstore", trans[0], params);
			}
		},
		
		quit: function()
		{
			AppstoreHome.quit();
			AppstoreCategory.quit();
			AppstoreApp.quit();
			AppstoreVendor.quit();
		},
		
		slideshow: function() {

			var current = 0;
			var nb_of_pict = $('#slideshow .slide').length;

			$('#slideshow .slide').each(function(i) {
				var pos = $(this).prevAll('.slide').length;
				$(this).addClass("__img_"+pos);
				if(pos != current) $(this).css("opacity", "0");
			});
			
			function next() {
				$("#slideshow .__img_"+current).fadeTo("slow", 0);
				current = (current + 1) % nb_of_pict;
				$("#slideshow .__img_"+current).stop().fadeTo("slow", 1);
			}
			
			return setInterval(next, 5000);
		},
		
		getAppstoreURL: function()
		{
			return Appstore.__appstoreURL;
		},
		
		setAppstoreURL: function(appstoreURL)
		{
			Appstore.__appstoreURL = appstoreURL;
		},
		
		displayApps: function(containerSel, apps)
		{
			for(var i in apps)
			{
				var app = apps[i];
				var widget = Appstore.__app_widget.replace("%appUID", app.vendorUID+"="+app.appUID).replace("%iconURL", Appstore.__appstoreURL+app.iconURL).replace("%appName", app.appName).replace("%categoryName", app.categoryName);
				$(containerSel).append(widget);
			}
			
			$(".app").click(function(event) {
				Appstore.run("to_app="+event.target.parentNode.id);
			});
		},
		
		displayCategories: function(containerSel, categories)
		{
			$(containerSel+" UL").html("");
			for(var i in categories)
			{
				$(containerSel+" UL").append("<li class=\"cat\" id=\""+categories[i].UID+"\">"+categories[i].name+"</li>");
			}
			
			$(".cat").click(function(event) {
				Appstore.run("to_category="+event.target.id);
			});
			
			$(containerSel).show();
		},
		
		displaySlideshow: function(containerSel, nbOfImages, imageCanonicalName, imageExtension)
		{
			$(containerSel).html("");
			for(var i = 1; i <= nbOfImages; i++)
			{
				$(containerSel).append("<img class=\"slide\" src=\""+Appstore.getAppstoreURL()+imageCanonicalName+i+imageExtension+"\"/>");
			}
			
			$(containerSel).show();
		},
		
		updateTrail: function(trail)
		{
			$("#trail").html("");
			for(var i in trail)
			{
				var item = trail[i];
				var id = (item.id != undefined)?(" id=\""+item.id+"\""):"";
				var text = (item.text != undefined)?item.text:"";
				$("#trail").append("<li"+id+"><span>"+cwf.tr(text)+"</span></li>");
				if(item.click != undefined) {
					$("LI#"+item.id+">SPAN").addClass("link");
					$("LI#"+item.id+">SPAN").click(item.click);
				}
			}	
		},
		
		displayNetworkErrorMessage: function(errorCode)
		{
			var helpmsg = "AppStore servers could not be found.";
			var opthelpmsg = "Check your Internet connection and refresh this page.";
			
			if(errorCode == 600)
			{
				helpmsg = "The application installation failed.";
				opthelpmsg = "This application seems NOT compliant with your PlaceTouch firmware version. ";
			}
			
			$("#appstoreWorkspace").html("<div class=\"networkError\"><h1>"+
											cwf.tr("appstoreNetworkError")+"</h1><div><span class=\"label\">"+
											cwf.tr("errorCode")+"</span>"+errorCode+
											"</div><div class=\"helpmsg\">"+helpmsg+"</div><div class=\"opthelpmsg\">"+opthelpmsg+"</div></div>");
		},
		
		fsm: {
			init: "home",
			pages_folder: "appstore",
			element_sel: "#workspace #content #appstoreWorkspace",
			
			home: {
			
				__pre: function(data) // here, data are a transition_stack
				{
					$("#appstoreWorkspace").addClass("home-page");
				},
				
				__post: function(data, loading_ok) // here, data are a transition_stack
				{
					if(loading_ok) {
						AppstoreHome.initUI();
					}
				},
				
				__leaving: function(transition)
				{
					$("#appstoreWorkspace").removeClass("home-page");
					AppstoreHome.quit();
					return true;
				},
				
				to_home: "home",
				to_category: "category",
				to_app: "app",
				to_vendor: "vendor"
			},
			
			category: {
				
				__pre: function(data) // here, data are a transition_stack
				{ },
				
				__post: function(data, loading_ok) // here, data[0] is a category UID
				{
					if(loading_ok) {
						AppstoreCategory.initUI(data[0]);
					}
				},
				
				__leaving: function(transition)
				{
					AppstoreCategory.quit();
					return true;
				},
				
				to_home: "home",
				to_category: "category",
				to_app: "app"
			},
			
			app: {

				__pre: function(data) // here, data are a transition_stack
				{ 
					$("#appstoreWorkspace").addClass("app-page");
				},
				
				__post: function(data, loading_ok) // here, data[1] is an app UID and data[0] is a vendor UID
				{
					if(loading_ok) {
						AppstoreApp.initUI(data);
					}
				},
				
				__leaving: function(transition)
				{
					$("#appstoreWorkspace").removeClass("app-page");
					AppstoreApp.quit();
					return true;
				},
				
				to_home: "home",
				to_category: "category",
				to_vendor: "vendor",
				to_app: "app"
			},
			
			vendor: {
				
				__pre: function(data) // here, data are a transition_stack
				{ 
					$("#appstoreWorkspace").addClass("vendor-page");
				},
				
				__post: function(data, loading_ok) // here, data[0] is a vendorUID
				{
					if(loading_ok) {
						AppstoreVendor.initUI(data[0]);
					}
				},
				
				__leaving: function(transition)
				{
					$("#appstoreWorkspace").removeClass("vendor-page");
					AppstoreVendor.quit();
					return true;
				},
				
				to_home: "home",
				to_app: "app"
			}
		},
		
		__appstoreURL : null,
		__lookNfeel: null,
		__app_widget : "<div class=\"app\" id=\"%appUID\">\
			<img src=\"%iconURL\"/><br/>\
			<span class=\"appname\">%appName</span><br/>\
			<span class=\"cat\">%categoryName</span>\
		</div>"
}