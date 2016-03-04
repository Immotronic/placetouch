var Diagnostics = {
		
	init: function()
	{
		
	},
	
	initUI: function() 
	{
		Placetouch.showBackButton(cwf.tr("home"));
		$("#diagnostic-tabs").tabs({ event: "click" });
		Diagnostics.__refresh_info();
		
		$("#journal").click(function() {
			Diagnostics.__refresh_journal_logs();
		});
	}, 
	
	run: function(transition)
	{
		Diagnostics.__running = true;
	},
	
	quit: function()
	{
		Diagnostics.__running = false;
	},
	
	__refresh_info: function()
	{
		Diagnostics.__refresh_enocean_info();
	},
	
	__refresh_enocean_info: function()
	{
		cwf.api.get_query("api/debug/diagnostics/", {
    		success: function(data) {
    			$("#enoceanStats PRE").html(data.info);
    		}
    	});
		
		cwf.api.get_query("api/debug/diagnosticLogs/", {
    		success: function(data) {
    			var nbRows = $("#enoceanLogs > DIV").children().length;
				for(var k in data.logs) {
					var log = data.logs[k];
					if(nbRows == 30) {
						$("#enoceanLogs > DIV > DIV:last-child").remove();
					}
					
					var cssClass = "";
					if(log.match(/^DISCARD */)) {
						cssClass = " class=\"discard\"";
					}
					else if(log.match(/^EMIT */)) {
						cssClass = " class=\"emit\"";
					}
					else if(log.match(/^ACK .*OK/)) {
						cssClass = " class=\"ack\"";
					}
					else if(log.match(/^ACK /)) {
						cssClass = " class=\"nack\"";
					}
					
					if(nbRows == 0) {
						$("#enoceanLogs > DIV").append("<div"+cssClass+">"+data.logs[k]+"</div>");	
					}
					else {
						$("<div"+cssClass+">"+log+"</div>").insertBefore("#enoceanLogs > DIV > DIV:first-child");
					}
    					
    			}
    			
    			if(Diagnostics.__running) {
    				Diagnostics.__refresh_info();
    			}
    		}
    	});
	},
	
	__refresh_journal_logs: function()
	{
		cwf.api.get_query("api/debug/systemEventsLogs/", {
    		success: function(data) {
    			if (data.logs.length != 0)
    			{
    				$("#systemEvents > DIV").empty();
	    			for(var l in data.logs) {				
	    				var log = data.logs[l];
	    				var date = new Date(log.date);
	    				var dateString;
	    				switch(cwf.l10n.get_language()) {
	    	    			case "en":
	    	    				dateString = date.toString("dd/MM/yyyy")+" "+date.toString("hh:mm:ss");
	    	    				break;
	    	    			case "fr":
	    	    				dateString = date.toString("dd/MM/yyyy")+" "+date.toString("HH:mm:ss");
	    	    				break;
	    				}
	    				var severity = "severity-low"; // default value
	    				switch(parseInt(log.severity))
	    				{
	    					case 2: severity = "severity-normal"; break;
	    					case 3: severity = "severity-high"; break;
	    				}
	    				$("#systemEvents > DIV").append($("<div>").html(dateString+" &nbsp;&nbsp;&nbsp; <span class=\""+severity+"\" >"+log.message+"</span>"));
	    			}
    			}
    			else
    			{
    				$("#systemEvents > DIV > SPAN").show();
    			}
    		}
    	});
	},
	
	__running : false
}