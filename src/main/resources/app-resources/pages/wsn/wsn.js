var Wsn = {
		
	init: function() 
	{
		cwf.api.get_query("api/wsn/pemBaseURLs", 
		{
			success: function(data) 
			{
				Wsn.__pemBaseURLs = data.urls;
				if(Wsn.__pemBaseURLs != null)
				{
					for(var id in Wsn.__pemBaseURLs) { if (Wsn.__pemBaseURLs.hasOwnProperty(id)) {
						Wsn.__loadMashupPemTranslationData(Wsn.__pemBaseURLs[id]);
						Wsn.__loadMashupControlUIHandler(Wsn.__pemBaseURLs[id]);
						Wsn.__loadMashupConfigurationUIHandler(Wsn.__pemBaseURLs[id]);
						Wsn.__loadMashupAddingUIHandler(Wsn.__pemBaseURLs[id]);
					}}
				}
			}
		});
	},
	
	initUI: function()
	{
		cwf.logdebug("WSN InitUI");
		var params = cwf.get_params();
		cwf.logdebug(params);
		if(params != null && params.nbb != undefined) {
			Placetouch.hideBackButton();
		}
		else {
			Placetouch.showBackButton(cwf.tr("home"));
		}
		
		Wsn.__hideMashupConfigurationUIs();
		Wsn.__exitPairingMode();
		
		$("#EnterPairingModeButton").click(function() {
			Wsn.__enterPairingMode();
		});
		
		$("#pemConfigurationButton").click(function() {
			Wsn.__enterConfigurationUI();
		});
		
		$("#CloseConfigurationModeButton").click(function() {
			Wsn.__exitConfigurationUI();
		});
		
		$("#ExitPairingModeButton").click(function() {
			Wsn.__exitPairingMode();
		});
		
		$("#newItemDetails #dropNewItem").click(function() {
			Wsn.__dropNewItem();
		});
		
		$("#newItemDetails #addItem").click(function() {
			Wsn.__addNewItem();
		});
		
		$("#itemDetails #dropItem").click(function() {
			$("#itemDetails #dropItem").attr("disabled", true);
			$("#itemDetails #dropConfirmation").show();
		});
		
		$("#itemDetails #cancel").click(function() {
			$("#itemDetails #dropItem").removeAttr("disabled");
			$("#itemDetails #dropConfirmation").hide();
		});
		
		$("#itemDetails #dropIt").click(function() {
			$("#itemdetails #dropConfirmation").hide();
			Wsn.__dropItem();
		});
		
		$("#itemDetails #itemName").change(function() {
			var val = $("#itemDetails #itemName").val();
			
			Wsn.__updateProperty("CustomName", val, function() {
				var htmlID = $("#itemDetails #htmlID").val();
				$("#items #"+htmlID+" .itemHeader").html(val);
				for(var i in Wsn.__items) {
					if(Wsn.__items[i].htmlID == htmlID) {
						Wsn.__items[i].CustomName = val;
					}
				}
			});
		});
		
		$("#itemDetails #itemLocation").change(function() {
			var val = $("#itemDetails #itemLocation").val();
			
			Wsn.__updateProperty("Location", val, function() {
				var htmlID = $("#itemDetails #htmlID").val();
				$("#items #"+htmlID+" .itemLocalisation").html(val);
				for(var i in Wsn.__items) {
					if(Wsn.__items[i].htmlID == htmlID) {
						Wsn.__items[i].Location = val;
					}
				}
			});
		});
		
		$("#addItemFeedbackFailed  SPAN").click(function() {
			$("#newItemDetails #addItem").removeAttr("disabled");
			$("#newItemDetails #dropNewItem").removeAttr("disabled");
			$("#addItemFeedbackFailed").hide();
		});
		
		$("#newItems").click(function(event) {
			if(event.target.id == "newItems") {
				$("#newItems>DIV").removeClass("selected");
				$("#newItemDetails").hide();
				$("#detailPanel #pairingHelp").show();
			}
		});
		
		$("#items").click(function(event) {
			if(event.target.id == "items") {
				clearInterval(Wsn.__pollingDeviceValue);
				$("#items>DIV").removeClass("selected");
				$("#itemDetails").hide();
				$("#detailPanel #wsnHelp").show();
			}
		});
	},
	
	run: function(data)
	{
		
	},
	
	quit: function()
	{
		clearInterval(Wsn.__pollingNewDevices);
		clearInterval(Wsn.__pollingDeviceValue);
		cwf.api.query("api/wsn/exitPairingMode", { }, {});
	},
	
	__enterPairingMode: function()
	{
		clearInterval(Wsn.__pollingDeviceValue);
		Wsn.__pollingNewDevices = setInterval(function() {
    		
    		cwf.api.get_query("api/wsn/getNewItems", {
        		success: function(data) {
        			if(data.items != undefined) 
        			{
        				for(var i in data.items) {
        					Wsn.__displayNewItem(data.items[i]);
        				}
        			}
        		}
        	}, {} /* no feedback handler for these queries*/);
    	}, 1000);
		
		cwf.api.query("api/wsn/enterPairingMode", { },
		{
			success: function(data) 
			{
				$("#EnterPairingModeButton").hide();
				$("#pemConfigurationButton").hide();
				$("#ExitPairingModeButton").show();
				$("#items").hide();
				$("#newItems").html("<h1>"+cwf.tr("newItemsTitle")+"</h1>");
				$("#newItems").show();
				$("#detailPanel #wsnHelp").hide();
				$("#detailPanel #pairingHelp").show();
				Wsn.__displayMashupAddingUIs();
				$("#itemDetails").hide();
				
				if(Wsn.__pemBaseURLs != null)
				{
					var i = 0;
					for(var id in Wsn.__pemBaseURLs) { if (Wsn.__pemBaseURLs.hasOwnProperty(id)) {
						Wsn.__appendMashupPairingInstructions("#detailPanel  #pairingHelp>DIV#mashup", Wsn.__pemBaseURLs[id], "piUI_pem"+i);
						i++;
					}}
				}
			}
		});
	},
	
	__exitPairingMode: function()
	{
		clearInterval(Wsn.__pollingNewDevices);
    		
		cwf.api.get_query("api/wsn/getItems", {
    		success: function(data) {
    			if(data.items != undefined) 
    			{
    				Wsn.__items = data.items;
    				for(var i in Wsn.__items) {
    					Wsn.__items[i].index = i;
    					Wsn.__displayItem(i);
    				}
    			}
    		}
    	}, {} /* no feedback handler for these queries*/);
		
		cwf.api.query("api/wsn/exitPairingMode", { },
		{
			success: function(data) 
			{
				$("#detailPanel #wsnHelp").show();
				$("#detailPanel #pairingHelp").hide();
				Wsn.__hideMashupAddingUIs();
				$("#detailPanel #pairingHelp>DIV#mashup").html("");
				$("#detailPanel #newItemDetails").hide();
				$("#EnterPairingModeButton").show();
				$("#pemConfigurationButton").show();
				$("#ExitPairingModeButton").hide();
				$("#items").show();
				$("#newItems").hide();
			}
		});
	},
	
	__enterConfigurationUI: function()
	{
		$("#items").hide();
		$("#itemDetails").hide();
		$("#EnterPairingModeButton").hide();
		$("#pemConfigurationButton").hide();
		$("#detailPanel #wsnHelp").hide();
		$("#CloseConfigurationModeButton").show();
		Wsn.__displayMashupConfigurationUIs();
	},
	
	__exitConfigurationUI: function()
	{
		$("#EnterPairingModeButton").show();
		$("#pemConfigurationButton").show();
		$("#detailPanel #wsnHelp").show();
		$("#items").show();
		$("#CloseConfigurationModeButton").hide();
		Wsn.__hideMashupConfigurationUIs();
	},
	
	__appendMashupPairingInstructions: function(sel, baseURL, htmlID)
	{
		$.ajax({	
			type: "GET", 
			url:baseURL + "ui/help/pairing/"+Wsn.__sysappName+"/instructions."+cwf.l10n.get_language()+".html",
			success: function(data) {
				$(sel).append("<div id=\""+htmlID+"\">"+data+"</div>");
				$("#"+htmlID+" DIV").hide();
				$("#"+htmlID+" H2").click(function() {
					$("#"+htmlID+" DIV").slideToggle("fast");
				});
			},
			error: function() { 
				cwf.log("No pairing instruction at "+baseURL);
			}
		});
	},
	
	__displayMashupAddingUIs: function()
	{
		$("#detailPanel #addItemMashupUIs>DIV").html("");
		if(Wsn.__pemBaseURLs != null)
		{
			var i = 0;
			var oneOrMoreUI = false;
			for(var id in Wsn.__pemBaseURLs) { if (Wsn.__pemBaseURLs.hasOwnProperty(id)) {
				var htmlID = "maUI_pem"+i;
				var handler = Placetouch.getPemAddingUIHandler(id);
				if(handler != undefined)
				{
					oneOrMoreUI = true;
					$("#detailPanel #addItemMashupUIs>DIV").append("<div id=\""+htmlID+"\"></div>");
					handler.addAddingUI("#"+htmlID);
					$("#"+htmlID+">DIV").hide();
					$("#"+htmlID+">H2").click(function(event) {
						$("#"+$(event.target).parent().attr("id")+">DIV").slideToggle("fast");
					});
				}
				i++;
			}}
			
			if(oneOrMoreUI) {
				$("#detailPanel #addItemMashupUIs").show();
			}
		}
	},
	
	__hideMashupAddingUIs: function()
	{
		$("#detailPanel #addItemMashupUIs").hide();
	},
	
	__displayMashupConfigurationUIs: function()
	{
		if($("#configurationUIs>DIV").html() == "")
		{
			$("#configurationUIs>DIV").html("");
			if(Wsn.__pemBaseURLs != null)
			{
				var i = 0;
				var oneOrMoreUI = false;
				for(var id in Wsn.__pemBaseURLs) { if (Wsn.__pemBaseURLs.hasOwnProperty(id)) {
					var htmlID = "confUI_pem"+i;
					var handler = Placetouch.getPemConfigurationUIHandler(id);
					if(handler != undefined)
					{
						oneOrMoreUI = true;
						$("#configurationUIs>DIV").append("<div id=\""+htmlID+"\"></div>");
						handler.addConfigurationUI("#"+htmlID);
						$("#"+htmlID+">DIV").hide();
						$("#"+htmlID+">H2").click(function(event) {
							$("#"+$(event.target).parent().attr("id")+">DIV").slideToggle("fast");
						});
					}
					i++;
				}}
				
				if(oneOrMoreUI) {
					$("#configurationUIs").show();
				}
			}
		}
		else
		{
			$("#configurationUIs").show();
		}
	},
	
	__hideMashupConfigurationUIs: function()
	{
		$("#configurationUIs").hide();
	},
	
	__loadMashupAddingUIHandler: function(baseURL)
	{
		cwf.load_js(baseURL+"ui/adding/"+Wsn.__sysappName+"/handler.js");
	},
	
	__loadMashupControlUIHandler: function(baseURL)
	{
		cwf.load_js(baseURL+"ui/control/"+Wsn.__sysappName+"/handler.js");
	},
	
	__loadMashupConfigurationUIHandler: function(baseURL)
	{
		cwf.load_js(baseURL+"ui/configuration/"+Wsn.__sysappName+"/handler.js");
	},
	
	__loadMashupPemTranslationData: function(baseURL)
	{
		cwf.l10n.load_tr(baseURL+"ui/L10n/"+Wsn.__sysappName+"/strings");
	},
	
	__displayNewItem: function(data)
	{
		if($("#newItems #"+data.htmlID).length == 0) 
		{
			var item = "<div class=\"wsnItem\" id=\""+data.htmlID+"\"><div class=\"itemHeader\">"+data.uid+"</div><div class=\"itemDescription\">";
			
			item += "<img class=\"itemTechnology\" src=\""+Wsn.__pemBaseURLs[data.pemUID]+"icon/icon_h30.png\"/>";
											
			if(data.capabilitySelection == undefined || data.capabilitySelection == "NO")
			{
				for(var i in data.capabilities)
				{
					item += "<img class=\"itemCapability\" src=\""+Wsn.__pemBaseURLs[data.pemUID]+"icon/capability/"+Wsn.__sysappName+"/"+data.capabilities[i]+".png\"/>" 
				}
			}
			else 
			{
				item += "<img class=\"itemCapability\" src=\"img/cap_unknown.png\"/>"
			}
			
			item += "</div><div class=\"itemFooter\">";
			item += "<div class=\"added\" style=\"display:none\">"+cwf.tr("added")+"</div>";
			item +="</div></div>"
					
			$("#newItems").append(item);
			
			if(data.added) {
				$("#"+data.htmlID).css("opacity", 0.3);
				$("#"+data.htmlID +" DIV.added ").show();
			}
			else {
				$("#"+data.htmlID).click(function() {
					$("#newItems>DIV").removeClass("selected");
					$("#"+data.htmlID).addClass("selected");
					Wsn.__displayNewItemDetails(data);
				});
			}
		}
	},
	
	__displayItem: function(index)
	{
		var data = Wsn.__items[index];
		if($("#items #"+data.htmlID).length == 0) 
		{
			var item = "<div class=\"wsnItem\" id=\""+data.htmlID+"\"><div class=\"itemHeader\">"+data.CustomName+"</div><div class=\"itemDescription\">";
			
			item += "<img class=\"itemTechnology\" src=\""+Wsn.__pemBaseURLs[data.pemUID]+"icon/icon_h30.png\"/>";
											
								
			for(var i in data.capabilities)
			{
				item += "<img class=\"itemCapability\" src=\""+Wsn.__pemBaseURLs[data.pemUID]+"icon/capability/"+Wsn.__sysappName+"/"+data.capabilities[i]+".png\"/>" 
			}
			
			item += "</div><div class=\"itemFooter\">";
			
			item += "<div class=\"itemLocalisation\">"+data.Location+"</div>";
			item +="</div></div>";
					
			$("#items").append(item);
			$("#"+data.htmlID).click(function() {
				$("#items>DIV").removeClass("selected");
				$("#"+data.htmlID).addClass("selected");
				Wsn.__displayItemDetails(data);
			});
		}
	},
	
	__displayItemDetails: function(data)
	{
		$("#detailPanel #wsnHelp").hide();
		$("#itemDetails #htmlID").val(data.htmlID);
		$("#itemDetails #itemUID").html(data.uid);
		$("#itemDetails INPUT#itemName").val(data.CustomName),
		$("#itemDetails INPUT#itemLocation").val(data.Location)
		$("#itemDetails #capabilities").html("");
		
		$("#itemDetails #itemControl>DIV").html("");
		var controlUIHandler = Placetouch.getPemControlUIHandler(data.pemUID);
		var controlUI = false;
		
		for(var i in data.capabilities)
		{
			var capability = data.capabilities[i];
			var configuration = null;
			if(data.configuration != undefined) {
				configuration = data.configuration; 
			}
			
			$("#itemDetails #capabilities").append(cwf.tr(capability)+"<br>");
			if((controlUIHandler != undefined) && controlUIHandler.doesControlUIExistFor(capability)) {
				controlUI = true;
				controlUIHandler.addControlUI(data.uid, capability, configuration, "#itemDetails #itemControl>DIV");
			}
		}
		
		var properties = false;
		$("#itemDetails #properties TABLE").html("");
		for(var property in data.customProperties) { if(data.customProperties.hasOwnProperty(property)) 
		{
			$("#itemDetails #properties TABLE").append("<tr><td class=\"label\">"+cwf.tr(property)+"</td><td class=\"value\">"+data.customProperties[property]+"</td></tr>");
			properties = true;
		}}
		
		if(properties) {
			$("#itemDetails #properties").show();
		}
		else {
			$("#itemDetails #properties").hide();
		}
		
		if(controlUI) {
			cwf.l10n.tr_element("#itemDetails #itemControl");
			$("#itemDetails #itemControl").show();
		}
		else {
			$("#itemDetails #itemControl").hide();
		}
		
		$("#itemDetails #updateItem").removeAttr("disabled");
		$("#itemDetails #dropItem").removeAttr("disabled");
		$("#itemDetails #noData").hide();
		$("#itemDetails #value").hide();
		$("#itemDetails").show()
		$("#itemDetails #loading").show();
		clearInterval(Wsn.__pollingDeviceValue);
		
		if(data.type != "ACTUATOR")
		{
			$("#itemDetails #itemValue").show();
			Wsn.__pollingDeviceValue = setInterval(function() {
				cwf.api.get_query("api/wsn/getItemValue/"+data.uid, {
		    		success: function(data) {
		    			$("#itemDetails #loading").hide();
		    			if(data.noDataAvailable) {
		    				$("#itemDetails #noData").show();
		    				$("#itemDetails #value").hide();
		    			}
		    			else 
		    			{
		    				var table = "<table>";
		    				for(var field in data) { if (data.hasOwnProperty(field) && (field != "status") && (field != "now")) {
		    					var val = "n/a";
		    					var time = "";
		    					if(data[field].timestamp != undefined) 
		    					{
			    					if(data[field].timestamp != 0) {
			    						val = data[field].uiValue + ((data[field].unit != undefined)?data[field].unit:"");
			    						time = duration_to_HMS_string((data.now - data[field].timestamp)/1000);
			    					}
		    					}
		    					else 
		    					{
		    						if(data[field].unit != undefined) 
			    					{
			    						switch(data[field].unit)
			    						{
			    							case "duration":
			    								if(data[field].value != 0)
			    								{
			    									val = duration_to_HMS_string((data.now - data[field].value)/1000);
			    								}
			    								break;
			    						}
			    					}
		    						else
		    						{
		    							val = data[field].uiValue;
		    						}
		    					}
								table += "<tr><td class=\"label\">"+cwf.tr(field)+"</td><td class=\"value\">"+val+"</td><td class=\"time\">"+time+"</td></tr>";
							}}
		    				table += "</table>";
		    				$("#itemDetails #value").html(table);
		    				$("#itemDetails #noData").hide();
		    				$("#itemDetails #value").show();
		    			}
		    		}
		    	}, {/* no feedback handler */});
			}, 1000);
		}
		else
		{
			$("#itemDetails #itemValue").hide();
		}
	},
	
	__displayNewItemDetails: function(data)
	{
		$("#detailPanel #pairingHelp").hide();
		$("#newItemDetails #itemUID").html(data.uid);
		$("#newItemDetails INPUT#itemName").val("");
		if(data.capabilitySelection == undefined || data.capabilitySelection == "NO")
		{
			$("#newItemDetails #capabilities").html("");
			for(var i in data.capabilities)
			{
				$("#newItemDetails #capabilities").append(cwf.tr(data.capabilities[i])+"<br>"); 
			}
		}
		else {
			var select = "<select id=\"capability\">";
			for(var i in data.capabilities)
			{
				select += "<option value=\""+data.capabilities[i]+"\">"+cwf.tr(data.capabilities[i])+"</option>";
			}
			select += "</select>";
			$("#newItemDetails #capabilities").html(select);
		}
		
		$("#newItemDetails #addItem").removeAttr("disabled");
		$("#newItemDetails #dropNewItem").removeAttr("disabled");
		$("#newItemDetails").show();
	},
	
	__dropNewItem: function()
	{
		$("#newItems>DIV.wsnItem.selected").hide();
		$("#newItems>DIV.wsnItem.selected").removeClass("selected");
		$("#newItemDetails").hide();
		$("#detailPanel #pairingHelp").show();
	},
	
	__addNewItem: function()
	{
		$("#newItemDetails #addItem").attr("disabled", "disabled");
		$("#newItemDetails #dropNewItem").attr("disabled", "disabled");
		var param = {
			itemUID: $("#newItemDetails TD#itemUID").html(),
			customName: $("#newItemDetails INPUT#itemName").val(),
			location: $("#newItemDetails INPUT#itemLocation").val(),
			capabilities: $("#newItemDetails SELECT#capability").val(),
    	};
		
		cwf.api.query("api/wsn/addItem", param, {
    		success: function() {
    			$("#newItems>DIV.wsnItem.selected").css("opacity", 0.3);
    			$("#newItems>DIV.wsnItem.selected DIV.added ").show();
    			$("#newItems>DIV.wsnItem.selected").removeClass("selected");
    			$("#newItemDetails").hide();
    			$("#detailPanel #pairingHelp").show();
    			$("#addItemFeedbackOk").show();
    			$("#addItemFeedbackOk").fadeOut(6000);
    		},
    		
    		failed: function(reason, errorCode) {
    			$("#addItemFeedbackFailed #reason").html(reason);
    			$("#addItemFeedbackFailed").show();
    		}
    	}, { to_hide: [], to_show: ["#newItemDetails #loading"]});
	},
	
	__dropItem: function()
	{
		var itemUID = $("#itemDetails TD#itemUID").html();
		
		for(var i in Wsn.__items) {
			if(Wsn.__items[i].uid == itemUID) {
				Wsn.__items.splice(i, 1);
				break;
			}
		}
		
		cwf.api.query("api/wsn/dropItem/"+itemUID, {}, {
    		success: function() {
				$("#items>DIV.wsnItem.selected").remove();
				$("#itemDetails").hide();
				$("#detailPanel #wsnHelp").show();
    		}
		});
	},
	
	__updateProperty: function(propertyName, value, successHandler)
	{
		var itemUID = $("#itemDetails TD#itemUID").html();
		var param = {
				propertyName: propertyName,
				value: value
	    	};
				
		cwf.api.query("api/wsn/updateProperty/"+itemUID, param, {
    		success: function() {
    			successHandler();
    		}
		});
	},
	
	__pollingNewDevices: null,
	
	__pollingDeviceValue: null,
	
	__pemBaseURLs: null,
	
	__items : null,
	
	__sysappName: "placetouch"
}