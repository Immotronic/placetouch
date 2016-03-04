"use strict";
var cwfui = {
		
	init: function(path_to_cwfui_l10n_folder)
	{
		cwf.l10n.load_tr(path_to_cwfui_l10n_folder+"cwfui");
		cwfui.vtabs.init();
		cwfui.form.validation.init();
	},
		
	vtabs : 
	{
		init: function()
		{
			$("DIV.vtabs").each(function() 
			{
				if($(this).children("[id]").length == 0) {
					$(this).children(".initial-help").addClass("showed");
				}
				else
				{
					var dt = $(this).attr("default-tab");
					if(dt != undefined) {
						$(this).find("LI[tab="+dt+"]").addClass("selected");
					}
					else {
						var item = $("DIV.vtabs DIV.tabs LI:first-child");
						item.addClass("selected");
						dt = item.attr("tab");
					}
					
					$(dt).addClass("showed");
				}
			});
			
			$("DIV.vtabs DIV.models LI").click(cwfui.vtabs.__tabClick);
		},
	
		// String renameHandler(tabId, newTabName);
		addTab: function(vtabsId, tabId, tabName, renameHint, renameHandler)
		{
			var vtabs = $("DIV.vtabs#"+vtabsId);
			vtabs.append("<div class=\"tab\" id=\""+tabId+"\"></div>");
			vtabs.find("DIV.tabs UL").append("<li tab=\""+tabId+"\"><span>"+tabName+"</span><input type=\"text\" value=\""+tabName+"\" size=\"25\" title=\""+renameHint+"\"></input></li>");
			vtabs.find("DIV.tabs LI").click(cwfui.vtabs.__tabClick);
			vtabs.find("DIV.tabs LI INPUT").change(function() { cwfui.vtabs.__onTabNameChange($(this), renameHandler) });
			vtabs.find("DIV.tabs LI[tab="+tabId+"]").click();
		},
		
		removeTab: function(tabId)
		{
			var tab = $("DIV.vtabs DIV.tabs LI[tab="+tabId+"]");
			var tabBar = tab.parent();
			tab.remove();
			$("DIV#"+tabId).remove();
			if(tabBar.children().length == 0) {
				tabBar.parent().parent().children(".initial-help").addClass("showed");
			}
			else {
				tabBar.children().first().click();
			}
		},
		
		__tabClick: function()
		{
			$(this).siblings().removeClass("selected");
			$(this).addClass("selected");
			$(this).parent().parent().siblings().removeClass("showed");
			$("#"+$(this).attr("tab")).addClass("showed");
		},
		
		__onTabNameChange: function(inputElement, renameHandler)
		{
			var v = inputElement.val().trim();
			// Next if is a hook for user code to get the user entry and to check its validity. 
			//   String is returned : same string as "v" == ok, 
			//      a different string means that the user entry has been rejected, the returned string is the one to replace the user one
			if(renameHandler != null) {
				v = renameHandler(inputElement.parent().attr("tab"), v);
			}
			
			inputElement.siblings().html(v);
			inputElement.val(v);
		}
	},
	
	form : 
	{	
		validation : 
		{		
			init : function() 
			{
				$("INPUT[validation~=auto]").change(function() {
					cwfui.form.validation.check($(this));
				});
			},
			
			validate: function(form_sel)
			{
				
			},
			
			check: function(input)
			{
				var constraint = input.attr("validation");

				if(constraint === undefined) {
					return true;
				}
				else 
				{
					constraint = constraint.replace("auto", "").trim();
					if(constraint === "") {
						return true;
					}
				}
				
				var value = input.val();
				var res = false;
				switch(constraint)
				{
					case "int":
						if(isNaN(parseInt(value))) {
							cwfui.form.validation.__showError(input, "must_be_an_int");
						}
						else {
							cwfui.form.validation.__hideError(input);
							input.val(parseInt(value));
							res = true;
						}
						break;
						
					case "decimal":
						value = value.replace(",", ".");
						if(isNaN(parseFloat(value))) {
							cwfui.form.validation.__showError(input, "must_be_a_decimal");
						}
						else {
							cwfui.form.validation.__hideError(input);
							input.val(parseFloat(value));
							res = true;
						}
						break;
				}
				
				return res;
			},
			
			__showError: function(element, message)
			{
				$("<div class=\"cwf validation error "+element.attr("name")+"CWFVALIDATIONERRORMESSAGE\">"+cwf.tr(message)+"</div>").appendTo(element.parent());
			},
			
			__hideError: function(element)
			{
				$("."+element.attr("name")+"CWFVALIDATIONERRORMESSAGE").remove();
			},
		}
	},
};