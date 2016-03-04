"use strict";
/*global $, document, window, escape, unescape */

//////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////
////	PUBLIC GENERAL PURPOSE FUNCTIONS
//////////////////////////////////////////////////////////////////////////////////////////

function is_array(input)
{
	return typeof(input)==='object'&&(input instanceof Array);
}

//----------------------------------------------------------------------------------------

function is_object(input)
{
	return typeof(input)==='object'&&(input instanceof Object)&&!(input instanceof Array);
}

//----------------------------------------------------------------------------------------

function is_function(input)
{
	return typeof(input)==='function';
}

//----------------------------------------------------------------------------------------

function object_count(object) 
{
	if(is_array(object)) {
		return object.length;
	}
	else if(is_object(object)) {
	
		var res = [];
		for(var _id in object) { if (object.hasOwnProperty(_id)) {
			res.push(_id);
		}}
		
		return res.length;
	}
	
	return 1;
}

//----------------------------------------------------------------------------------------

function isEmpty(object) 
{
	if(is_array(object)) {
		return object.length == 0;
	}
	
    for(var prop in object) {
        if(object.hasOwnProperty(prop))
            return false;
    }

    return true;
}

//----------------------------------------------------------------------------------------

function clone(obj) 
{
    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) {
    	return obj;
    }

    // Handle Date
    if (obj instanceof Date) {
        var copy = new Date();
        copy.setTime(obj.getTime());
        return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
        var copy = [], len = obj.length;
        for (var i = 0; i < len; ++i) {
            copy[i] = clone(obj[i]);
        }
        return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
        var copy = {};
        for (var attr in obj) {
            if (obj.hasOwnProperty(attr)) copy[attr] = clone(obj[attr]);
        }
        return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
}

//----------------------------------------------------------------------------------------

function json_to_object(json_string)
{
	return eval("("+json_string+")");
}

//----------------------------------------------------------------------------------------

function object_to_json(object, indent)
{
	var enable_indent = indent !== null;
	
	if(indent === undefined) {
		indent = "";
	}
	
	var text;
	if(is_array(object)) {
		text = "[";
	}
	else if(is_object(object)) {
		text = "{";
	}
	else {
		return object;
	}
	
	if(enable_indent) {
		text += "\n";
	}
	
	var nb_items = object_count(object);
	var count = 1;
	for(var _id in object) { if (object.hasOwnProperty(_id))
	{
		if(enable_indent) {
			text += indent + "    ";
		}
		
		if(is_object(object)) {
			text += "\"" + _id + "\":";
		}
		
		var new_indent = null;
		if(enable_indent) {
			new_indent = indent + "    ";
		}
		
		if(is_array(object[_id]) || is_object(object[_id])) {			
			text += object_to_json(object[_id], new_indent);
		}
		else if(typeof object[_id] == "string") {
			var safetext = object[_id].replace(/\\/g, "\\\\");
			safetext = safetext.replace(/\"/g, "\\\"");
			text += "\"" + safetext + "\"";
		}
		else { 
			text += "\"" + object[_id] + "\"";
		}
		
		if(count < nb_items) {
			text += ",";
		}
		
		if(enable_indent) {
			text +=  "\n";
		}
		
		count++;
	}}
	
	if(is_array(object)) {
		
		if(enable_indent) {
			text += indent + "]";
		}
		else {
			text += "]";
		}
	}
	else if(is_object(object)) {
		
			
		if(enable_indent) {
			text += indent + "}";
		}
		else {
			text += "}";
		}
	}
	
	return text;
}

function get_object_keys(object)
{
	var res = [];
	for(var k in object) { if (object.hasOwnProperty(k)) {
		res.push(k);
	}};

	return res;
}


//////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////
////	CWF CLASS
//////////////////////////////////////////////////////////////////////////////////////////


var cwf =
{
	//////////////////////////////////////////////////////////////////////////////////////
	////	general purpose FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	
	set_debug:	function()
	{
		cwf.__debug__ = true;
		/*if(!$("#cwfconsole").length) 
		{
			$("BODY").append("<div id=\"cwfconsole\"><h4>Console</h4></div>");
		}*/
		cwf.log("userAgent: "+navigator.userAgent.toLowerCase());
	},
	
	
	////	cookies & url parameters 	//////////////////////////////////////////////////
	//------------------------------------------------------------------------------------
	get_cookie_value:	function(cookie_name)
	{
		if (document.cookie.length>0)
		{
			var start = document.cookie.indexOf(cookie_name + "=");
			if (start !== -1)
			{
				start = start + cookie_name.length + 1;
				var end = document.cookie.indexOf(";", start);
				if (end === -1) {
					end = document.cookie.length;
				}
				return unescape(document.cookie.substring(start, end));
			}
		}

		return "";
	},
	
	
	set_cookie:	function(name, value, expire) // expire in sec.
	{
		var expire_date = new Date();
		expire_date.setTime(expire_date.getTime() + expire * 1000);
		
		var content = name + "=" 
						+ escape(value) 
						+ ((expire===null) ? "" : ";expires="+expire_date.toUTCString()+"; path=/");
		document.cookie = content;
	},
	
	
	get_params: function() 
	{
		var del_index = window.location.href.indexOf('?');
		if(del_index < 0) {
			return null;
		}
		
		var del_index_end = window.location.href.indexOf('#');
		var hashes;
		
		if(del_index_end < 0) {
			hashes = window.location.href.slice(del_index + 1).split('&');
		}
		else {
			hashes = window.location.href.slice(del_index + 1, del_index_end).split('&');
		}
		
		var vars = {};
		var hash;
		var i;
		
		for(i = 0; i < hashes.length; i++)
		{
			hash = hashes[i].split('=');
			vars[hash[0]] = ((hash[1]!=undefined)?hash[1]:true);
		}
		
		return vars;
	},
	

	////	dynamic css & javascript & file content loading	//////////////////////////////////////////
	//------------------------------------------------------------------------------------
	load_css: function(css_url)
	{
		css_url += "?r="+Math.random();
		$("HEAD").append('<link rel="stylesheet" type="text/css" href="'+css_url+'">');
	},
	
	load_js: function(js_url)
	{
		$("HEAD").append('<script type="text/javascript" src="'+js_url+'"></script>');
	},
	
	load_file_content: function(file_url, handler, data_for_handler)
	{
		$.ajax({
			type: "GET",
			url: file_url,
			cache: false,
			success: function(data) 
			{
				if(handler != undefined) {
					handler(data, data_for_handler);
				}
			},
			error: function (h, m, e)
			{
				var http_error = "";
				if(h.status != 200) {
					http_error = "Error "+h.status;
				}
				cwf.logerr("load_file_content: "+http_error+" while loading "+file_url+": "+e+", "+m);
			}
		});
	},
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	////	l10n FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	
	tr:	function(text_id, params)
	{
		var res = cwf.l10n.__text__[text_id];
		if(res != undefined && is_array(params)) {
			for(var i in params) {
				res = res.replace("$$", params[i]);
			}
			
			return res;
		}
		return (res === undefined)?text_id:res;
	},
	
	l10n:
	{
		load_tr: function(tr_url_without_extension, handler)
		{
			$.ajax({	
				dataType: "json",
				url: tr_url_without_extension + "." + cwf.l10n.__lg__ + ".json",
				cache: false,
				success: function(data) 
				{
					cwf.l10n.__load_translations(data);
					if(handler != undefined) {
						handler();
					}
				},
				error: function (h, m, e)
				{
					var http_error = "";
					if(h.status != 200) {
						http_error = "Error "+h.status;
					}
					cwf.log("load_tr: "+http_error+" while loading "+tr_url_without_extension+"."+cwf.l10n.__lg__+".json: "+e+", "+m);
				}
			});
		},
		
		//--------------------------------------------------------------------------------
		
		load_date_lib: function(cwf_path)
		{
			cwf.load_js(cwf_path+"datejs/date-"+cwf.l10n.__full_lg__+".js");
		},
		
		//--------------------------------------------------------------------------------
		
		set_language: function(language)
		{
			var lg = language.split("-", 1);
			cwf.l10n.__lg__ = lg[0];
			cwf.l10n.__full_lg__ = language;
		},
		
		//--------------------------------------------------------------------------------
		
		get_language: function()
		{
			return cwf.l10n.__lg__;
		},
		
		//--------------------------------------------------------------------------------
		
		get_full_language: function()
		{
			return cwf.l10n.__full_lg__;
		},
		
		//--------------------------------------------------------------------------------
	
		tr_html: function (text_id)
		{
			var text = cwf.tr(text_id);
			text = text.replace(/%([A-z0-9_-]*)%/g, "<span class=\"tr-text-placeholder\" id=\"$1\"></span>");
			
			$("[title=\""+text_id+"\"]").attr("title", text);
			$("[tr=\""+text_id+"\"]").html(text);
			$("IMG[tr=\""+text_id+"\"]").attr("src", text);
			$("INPUT[tr=\""+text_id+"\"]").attr("value", text);
		},
		
		//--------------------------------------------------------------------------------
		
		tr_doc: function()
		{
			for(var t in cwf.l10n.__text__) { if (cwf.l10n.__text__.hasOwnProperty(t))
			{
				cwf.l10n.tr_html(t);
			}}
		},
		
		//--------------------------------------------------------------------------------
		
		tr_element: function(selector)
		{
			for(var t in cwf.l10n.__text__) { if (cwf.l10n.__text__.hasOwnProperty(t))
			{
				cwf.l10n.__tr_html(selector, t);
			}}
		},
		
		//--------------------------------------------------------------------------------
		// app: private members	//////////////////////////////////////////////////////////
		
		__load_translations: function(data)
		{
			for(var _id in data) { if (data.hasOwnProperty(_id))
			{
				if(cwf.l10n.__text__[_id] != null) {
					//cwf.logwarn("[L10n] overring translation id "+_id+" ('"+cwf.l10n.__text__[_id]+"' -> '"+data[_id]+"')");
				}
				cwf.l10n.__text__[_id] = data[_id];
			}}
		},
		
		//--------------------------------------------------------------------------------
		
		__tr_html: function (sel, text_id)
		{
			if(sel != "") {
				sel += " ";
			}
			
			var text = cwf.tr(text_id);
			$(sel+"[tr=\""+text_id+"\"]").html(text);
			$(sel+"IMG[tr=\""+text_id+"\"]").attr("src", text);
			$(sel+"INPUT[tr=\""+text_id+"\"]").attr("value", text);
		},
		
		//--------------------------------------------------------------------------------
		
		__text__: [],
	
		__full_lg__: "en-US",
		
		__lg__: "en"
	},
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	////	api FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	
	api: 
	{
		/**
		 * failure handler: function(reason, errorCode) {}
		 */
		set_default_failure_handler: function(default_failure_handler)
		{
			cwf.api.__default_failure_handler = default_failure_handler;
		},
		
		/**
		 * error handler: function(errorCode, error, message) {}
		 */
		set_default_error_handler: function(default_error_handler)
		{
			cwf.api.__default_error_handler = default_error_handler;
		},
		
		/**
		 * ui_feedback : object with 2 members where to_hide contains an array of 
		 *	element selectors to hide while querying, and to_show contains an array of
		 *	element selectors to show while querying.
		 *		ex: {
		 *				to_hide: [ "#install_button", "#cancel"],
		 *				to_show: [ "#loading_wheel"]
		 *			}
		 */
		set_default_ui_feedback: function(ui_feedback) 
		{
			cwf.api.__default_ui_feedback = ui_feedback;
		},
		
		/** 
		 *  response_handler : {
		 *  	success: function(data) {},
		 *  	failed: function(reason, errorCode) {},
		 *  	error: function(errorCode, error, message) {}
		 *  }
		 *  	Si pas de failed ou error, les default handler sont appelés, s'ils existent.
		 *  
		 *  ui_feedback :an optional object with 2 members where to_hide contains an array of 
		 *	element selectors to hide while querying, and to_show contains an array of
		 *	element selectors to show while querying.
		 *		ex: {
		 *				to_hide: [ "#install_button", "#cancel"],
		 *				to_show: [ "#loading_wheel"]
		 *			}
		 *
		 *	When query finish (successfully or not), to_show elements are hidden.
		 */
		query: function(url, data_to_post, response_handler, ui_feedback)
		{
			// Getting user credentials & and adding them to data_to_post
			data_to_post["__credentials"] = cwf.auth.get_credentials();
			cwf.api.__query("POST", url, data_to_post, response_handler, ui_feedback, false, null);
		},
		
		crossdomain_query: function(url, data_to_post, response_handler, ui_feedback, contentType)
		{
			cwf.api.__query("POST", url, data_to_post, response_handler, ui_feedback, true, contentType);
		},
        
        json_query: function(url, data_to_post, response_handler, ui_feedback)
        {
            data = { data: object_to_json(data_to_post, null) };
			cwf.api.query(url, data, response_handler, ui_feedback, false, null);
        },
        
        get_query: function(url, response_handler, ui_feedback)
        {
        	cwf.api.__query("GET", url, null, response_handler, ui_feedback, false, null);
        },
        
        crossdomain_get_query: function(url, response_handler, ui_feedback)
        {
        	cwf.api.__query("GET", url, null, response_handler, ui_feedback, true, null);
        },
        
        raw_query: function(url, contentType, data_to_post, response_handler, ui_feedback)
        {
        	cwf.api.__query("POST", url, data_to_post, response_handler, ui_feedback, false, contentType);
        },
		
		__query: function(type, url, data_to_post, response_handler, ui_feedback, cross_domain, contentType)
		{
			var feedback = ui_feedback;
			if(feedback == null) {
				feedback = cwf.api.__default_ui_feedback;
			}
			
			if(feedback)
			{
				for(var k in feedback.to_hide) {
					$(feedback.to_hide[k]).hide();
				}
				
				for(var k in feedback.to_show) {
					$(feedback.to_show[k]).show();
				}
			}
			
			var ts = (new Date()).getTime(); //Used to prevent JS/CSS caching
			if(url.indexOf("?") == -1) {
				url += "?ts="+ts;
			}
			else {
				url += "&ts="+ts;
			}
			
			var processData = true;
			var _contentType = "application/x-www-form-urlencoded; charset=UTF-8"
			var dataType = "json";
			if(contentType != null) {
				processData = false;
				_contentType = contentType;
				if(is_object(data_to_post) || is_array(data_to_post)) {
					data_to_post = JSON.stringify(data_to_post);
				}
				dataType = "text";
			}
			
			// Perform JSON ajax query
			$.ajax({	
				type: type,
				dataType: dataType, 
				url:url,
				crossDomain: cross_domain,
				data: data_to_post,
				contentType: _contentType,
				processData: processData,
				success:	function(data)
				{
					if(feedback)
					{
						for(var k in feedback.to_show) {
							$(feedback.to_show[k]).hide();
						}
						
						for(var k in feedback.to_hide) {
							$(feedback.to_hide[k]).show();
						}
					}
					
					var json = data;
					
					// ----- These lines could be useful for debugging purposes ------
					/*try {
						json = $.parseJSON(data);
					}
					catch(e) {
						cwf.logdebug("cwf.api.query("+url+"): JSON exception occured. Data were:\n");
						cwf.logdebug(data);
						return;
					}*/
					// ---------------------------------------------------------------
					
					if(json == null) {
						cwf.logdebug("cwf.api.query("+url+"): JSON is null. Data were:\n");
						cwf.logdebug(data);
						return;
					}
					
					if(contentType == null)
					{
						if(json.status == "ok")
						{
							if(response_handler != null && response_handler.success !== undefined)  {
								response_handler.success(json);
							}
						}
						else
						{
							if(response_handler != null && response_handler.failed !== undefined) {
								response_handler.failed(json.reason, json.code);
							}
							else if(cwf.api.__default_failure_handler != null) {
								cwf.api.__default_failure_handler(json.reason, json.code);
							}
						}
					}
					else
					{
						if(response_handler != null && response_handler.success !== undefined)  {
							response_handler.success(json);
						}
					}
				},
				error:	function(h, err, help_msg)
				{
					if(feedback)
					{
						for(var k in feedback.to_show) {
							$(feedback.to_show[k]).hide();
						}
						
						for(var k in feedback.to_hide) {
							$(feedback.to_hide[k]).show();
						}
					}
					
					if(response_handler != null && response_handler.error !== undefined) {
						response_handler.error(h.status, err, help_msg);
					}
					else if(cwf.api.__default_error_handler != null) {
						cwf.api.__default_error_handler(h.status, err, help_msg);
					}
				}
			});
		},
		
		__default_failure_handler : function(reason, code) {
			cwf.logerr("[cwf.api] query FAILURE: reason="+reason+", code="+code);
		},
		__default_error_handler : function(status, err, help_msg) {
			cwf.logerr("[cwf.api] query ERROR: status="+status+", err="+err+", help="+help_msg);
		},
		__default_ui_feedback : null
	},
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	////	auth FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	
	auth:
	{
		/*
			handler = { success: function(), //=> called if sign in succeed
						failed: function(),  //=> called if login/password are wrong
						error: function(http_error_code, other_error_type, error)}	 //=> called on network error (HTTP 404, HTTP 500, etc...) 
		*/
		sign_in: function(username, password, handler, ui_feedback)
		{
			cwf.auth.__username = username;
			cwf.auth.__password = password;
			
			cwf.api.query(cwf.auth.__url, {}, {
				success:	function(data)
				{
					if(data.status == "ok")
					{
						cwf.auth.maintain();
						
						cwf.auth.__data = data;
						cwf.auth.__store_data();
						
						if(handler.success !== undefined) handler.success();
					}
					else
					{
						if(handler.failed !== undefined) handler.failed(data.reason);
					}
				},
				error:	function(h, err, help_msg)
				{
					if(handler.error !== undefined) handler.error(h.status, err, help_msg);
				}
			}, ui_feedback);
		},
		
		//--------------------------------------------------------------------------------
		
		get_credentials: function()
		{
			if(cwf.auth.__username == "" && cwf.auth.__password == "") {
				cwf.auth.__username = $.jStorage.get("cwf.auth.username");
				cwf.auth.__password = $.jStorage.get("cwf.auth.password");
			}
			return { username: cwf.auth.__username, password: cwf.auth.__password };
		},
		
		//--------------------------------------------------------------------------------
		
		set_new_password: function(old_password, new_password)
		{
			if(cwf.auth.__username == "" && cwf.auth.__password == "") {
				cwf.auth.__username = $.jStorage.get("cwf.auth.username");
				cwf.auth.__password = $.jStorage.get("cwf.auth.password");
				cwf.auth.__data = $.jStorage.get("cwf.auth.data");
			}
			
			if(old_password == cwf.auth.__password) {
				cwf.auth.__password = new_password;
				cwf.auth.__store_data();
			}
		},
		
		//--------------------------------------------------------------------------------
		
		get_data: function()
		{
			if($.isEmptyObject(cwf.auth.__data)) {
				cwf.auth.__data = $.jStorage.get("cwf.auth.data");
			}
			return clone(cwf.auth.__data);
		},

		//--------------------------------------------------------------------------------		
		
		sign_out: function()
		{
			cwf.set_cookie(cwf.auth.__cookie_login, "no");
			cwf.auth.__flush_data();
		},
		
		//--------------------------------------------------------------------------------
		
		check_logging: function ()
		{
			return cwf.get_cookie_value(cwf.auth.__cookie_login) == "yes";
		},
		
		//--------------------------------------------------------------------------------
		
		maintain: function()
		{
			cwf.set_cookie(cwf.auth.__cookie_login, "yes", cwf.auth.__login_duration);
		},
	
		//--------------------------------------------------------------------------------
		
		set_login_duration: function(duration_in_seconds)
		{
			cwf.auth.__login_duration = duration_in_seconds;
		},
		
		//--------------------------------------------------------------------------------
		
		set_sign_in_url: function(url)
		{
			cwf.auth.__url = url;
		},
		
		//--------------------------------------------------------------------------------
		// auth: private members	//////////////////////////////////////////////////////
		
		__store_data: function()
		{
			$.jStorage.set("cwf.auth.data", cwf.auth.__data);
			$.jStorage.set("cwf.auth.username", cwf.auth.__username);
			$.jStorage.set("cwf.auth.password", cwf.auth.__password);
		},
		
		//--------------------------------------------------------------------------------
		
		__flush_data: function()
		{
			$.jStorage.deleteKey("cwf.auth.data");
			$.jStorage.deleteKey("cwf.auth.username");
			$.jStorage.deleteKey("cwf.auth.password");
		},
		
		//--------------------------------------------------------------------------------
		
		__data: {},
		__username: "",
		__password: "",
		__cookie_login: "cwfsi",
		__url: "",
		__login_duration: 300		// 300 sec == 5 min
	},
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	////	app FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	app:	
	{
		read_params:	function()
		{
			var params = cwf.get_params();
			cwf.app.__page__ = ((params.p === null)?"main":params.p);
			if(params.lg !== null) {
				cwf.app.__lg__ = params.lg;
			}
		},
		
		//--------------------------------------------------------------------------------
		
		get_page:	function()
		{
			return cwf.app.__page__;
		},
		
		//--------------------------------------------------------------------------------
		
		set_language:	function(lg)
		{
			cwf.app.__lg__ = lg;
		},
		
		//--------------------------------------------------------------------------------
		
		set_expire_delay:	function(delay)
		{
			cwf.app.__expire_delay__ = delay;
		},
		
		//--------------------------------------------------------------------------------
		// app: private members	//////////////////////////////////////////////////////////
		
		__page__:				"",
		
		__lg__:					"en",	// default language is english
		
		__expire_delay__:		3600	// 1 minute
	},
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	////	ui FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	
	ui:
	{
		create_select: function(name, options, onchange)
		{
			var res = "<select name=\""+name+"\" onchange=\""+onchange+"\">";	
			for(var k in options)
			{
				res += "<option value=\""+options[k].key+"\">"+options[k].text+"</option>";
			}
			
			res += "</select>";
			return res;
		},
		
		create_table: function(css_class, rows, header_row)
		{
			var res = "<table class=\""+css_class+"\">";
				
			if(header_row != undefined)
			{
				res += "<tr class=\"header\">";
				for(var k in header_row) {
					res += "<td>"+header_row[k]+"</td>";
				}
				res += "</tr>";
			}
			
			var even = true;
			for(var row in rows)
			{
				even = !even;
				
				if(even) {
					res += "<tr class=\"even\">";
				}
				else {
					res += "<tr class=\"odd\">";
				}
				
				for(var col in rows[row])
				{
					res += "<td>"+rows[row][col]+"</td>";
				}
				res += "</tr>";
			}
			
			res += "</table>";
			
			return res;
		},
		
		create_link: function(text, href, onclick)
		{
			if(href != undefined && href != null && href != "") {
				href = " href=\""+href+"\"";
			}
			else {
				href = "";
			}
			
			if(onclick != undefined && onclick != null && onclick != "") {
				onclick = " onclick=\""+onclick+"\"";
			} else {
				onclick = "";
			}

			return "<a"+href+onclick+">"+text+"</a>";
		},
        
        create_button: function(name, text, onclick)
        {
            if(onclick != undefined && onclick != null && onclick != "") {
				onclick = " onclick=\""+onclick+"\"";
			} else {
				onclick = "";
			}
            
            return "<input type=\"button\" name=\""+name+"\" value=\""+text+"\""+onclick+"></input>";
        },
		
		//////////////////////////////////////////////////////////////////////////////////
		error:	//// error handling //////////////////////////////////////////////////////
		{		
			check: 
			{
				is_email: function(value)
				{
					var pattern=/^([a-zA-Z0-9_.-])+@([a-zA-Z0-9_.-])+\.([a-zA-Z])+([a-zA-Z])+/;
					return pattern.test(value);
				},
				
				is_int: function(value)
				{
					var pattern=/^([0-9]+)$/;
					return pattern.test(value);
				}
			},
			
			set_contextual_template: function(template)
			{
				cwf.ui.error.__contextual_template = template;
			},
			
			//--------------------------------------------------------------------------------
			
			show_contextual: function(contextual_element_id, message, in_or_after)
			{
				var msg_id = contextual_element_id+"_err";
				
				var elem = $("#"+contextual_element_id);
				var box = cwf.ui.error.get_contextual(message, msg_id);
	
				if(in_or_after === undefined || in_or_after === "after") {
					elem.after(box);
				}
				else {
					elem.html(box);
				}
			},
			
			//--------------------------------------------------------------------------------
			
			hide_contextual: function(contextual_element_id)
			{
				var msg_id = "#"+contextual_element_id+"_err";
				$(msg_id).remove();
			},
			
			//--------------------------------------------------------------------------------
			
			get_contextual: function(message, message_id)
			{
				return cwf.ui.error.__contextual_template.replace("%message_id", message_id).replace("%message", cwf.tr(message));
			},
			
			//--------------------------------------------------------------------------------
			
			hide_all_contextual: function()
			{
				$("[cwf=errmsg]").remove();
			},
			
			//--------------------------------------------------------------------------------
			
			__contextual_template: "<div cwf=\"errmsg\" id=\"%message_id\" class=\"c_errmsg\">%message</div>"
		},
		
		//////////////////////////////////////////////////////////////////////////////////
		loading:	//// loading wheels management ///////////////////////////////////////
		{
			/*set_ui: function(loading_ui_html)
			{
				if(loading_ui_html !== undefined) {
					cwf.ui.loading.__loading_ui__ = loading_ui_html;
				}
			},*/
			
			//--------------------------------------------------------------------------------
			
			show: function()
			{
				//$(cwf.ui.loading.__loading_hosting_element__).html(cwf.ui.loading.__loading_ui__);
				$(cwf.ui.loading.__loading_hosting_element__).show();
			},
			
			//--------------------------------------------------------------------------------
			
			hide: function()
			{
				//$(cwf.ui.loading.__loading_hosting_element__).html("");
				$(cwf.ui.loading.__loading_hosting_element__).hide();
			},
			
			//--------------------------------------------------------------------------------
			
			//__loading_ui__: "<img src=\"resources/img/loading.gif\" alt=\"loading...\"/>",
	
			__loading_hosting_element__: "#loading"
		},

		//////////////////////////////////////////////////////////////////////////////////		
		menu: 	//// menu management /////////////////////////////////////////////////////
		{
			add_item: function(menu_id, item_id, action, prepend)
			{
				if(action === undefined) {
					action = "";
				}
				else {
					action = " onclick=\"" + action + "\"";
				}
				
				var item_html = "<li id=\"" + item_id + "\" tr=\"" + item_id + "\" " + action + "/>";
				if(prepend === undefined || !prepend) {
					$("#"+menu_id).prepend(item_html);
				}
				else {
					$("#"+menu_id).append(item_html);
				}
				
				cwf.l10n.tr_html(item_id);
			},
			
			//----------------------------------------------------------------------------
			
			select_item: function(menu_id, item_id)
			{
				$("#"+menu_id+" LI").removeClass("selected");
				$("#"+menu_id+" LI#" + item_id).addClass("selected");
			}
		},
		
		//////////////////////////////////////////////////////////////////////////////////
		ctrl:
		{
			/* parameters = { 
					form_sel: selector of the enclosing element of all ordered fields
					submit: ref of function to call on enterkey press if action == "submit"
				}
			*/
			enterkey_management: function(event, parameters)
			{
				if(event.keyCode == 13)
				{
					if(parameters.submit !== undefined) {
						parameters.submit();
					}
					else
					{
						var sel = parameters.form_sel+" #"+event.target.id;
						var order = parseInt($(sel).attr("order"));
						
						if(isNaN(order)) {
							cwf.log("order attribute of "+sel+" element is not a number");
							return; 
						}
						
						var next = $(parameters.form_sel+" [order="+(order+1)+"]");
						if(next.length == 0) {
							next = $(parameters.form_sel+" [order=1]");
							if(next.length == 0) {
								cwf.log("Cannot find an form element with order attribute set to 1");
								return; 
							}
						}
						
						next.focus();
					}
				}
			}
		}
	},
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	////	form FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	
	form:
	{
		update: function(form_descr)
		{
			for(var f in form_descr) { if (form_descr.hasOwnProperty(f))
			{
				if(cwf.form.__is_field(form_descr[f]) && $(form_descr[f].sel).length)
				{
					form_descr[f].val = $(form_descr[f].sel).val();
				}
			}}
			
			return form_descr;
		},
		
		//--------------------------------------------------------------------------------
		
		partial_update: function(form_descr, fields_to_update)
		{
			for(var field_name in fields_to_update) { if (fields_to_update.hasOwnProperty(field_name))
			{
				if(cwf.form.__is_field(form_descr[field_name]) && $(form_descr[field_name].sel).length)
				{
					form_descr[field_name].val = $(form_descr[field_name].sel).val();
				}
			}}
			
			return form_descr;
		},
		
		//--------------------------------------------------------------------------------
		
		update_html: function(form_descr)
		{
			for(var f in form_descr) { if (form_descr.hasOwnProperty(f))
			{
				if(cwf.form.__is_field(form_descr[f]) && $(form_descr[f].sel).length)
				{
					var element = $(form_descr[f].sel);
					var value = form_descr[f].val;
					if((element.attr("type") === "radio" || element.attr("type") === "checkbox" || element.attr("multiple") === "multiple") && !is_array(value))
					{
						value = [ value ];
					}
					if(element.attr("type") || element.get(0).tagName === "select")
					{
						element.val(value);
					}
					else
					{
						element.html(value);
					}
				}
			}}
		},
		
		//--------------------------------------------------------------------------------
		
		partial_update_html: function(form_descr, fields_to_update)
		{
			for(var field_name in fields_to_update) { if (fields_to_update.hasOwnProperty(field_name))
			{
				if(cwf.form.__is_field(form_descr[field_name]) && $(form_descr[field_name].sel).length)
				{
					$(form_descr[field_name].sel).val(form_descr[field_name].val);
				}
			}}
		},
		
		//--------------------------------------------------------------------------------
		
		__is_field: function(obj)
		{
			return is_object(obj) && typeof(obj.sel) === "string";
		}
	},
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	////	STATE MACHINE FUNCTION SUBSET
	//////////////////////////////////////////////////////////////////////////////////////
	
	sm:
	{
		add_state_machine:	function(id, state_machine)
		{
			cwf.sm.__sm__[id] = state_machine;
		},
		
		//--------------------------------------------------------------------------------
		
		get_dialog_ctrl: function(transition, data)
		{
			var json = "";
			if(data != undefined && data != null)
			{
				json = ", "+data;
			}
			
			return "cwf.sm.dialog_ctrl('main', '"+transition+"'"+json+");";
		},
		
		//--------------------------------------------------------------------------------
		
		get_current_state: function(sm_name)
		{
			return cwf.sm.__sm__[sm_name].current_state;
		},
		
		//--------------------------------------------------------------------------------
		
		reinit_state_machine: function(sm_name)
		{
			cwf.sm.__sm__[sm_name].current_state = "";
		},
		
		//--------------------------------------------------------------------------------
		
		set_default_page_loading_error_handler: function(default_page_loading_error_handler) 
		{
			cwf.sm.__default_page_loading_error_handler = default_page_loading_error_handler;
		},
		
		//--------------------------------------------------------------------------------
	
		dialog_ctrl : function(sm_name, transition, data)
		{
			var page_name = null;
			var postprocess = null;
			var pur_state = false;
			var autotrans;
			
			//cwf.logdebug("cwf.sm.dialog_ctrl(): sm="+sm_name+", cs="+cwf.sm.__sm__[sm_name].current_state+", tr="+transition);
			
			if(transition == "") {
				transition = undefined;
			}
			
			if(cwf.sm.__sm__[sm_name] != undefined)
			{
				// Getting the state machine current state.
				var current_state = cwf.sm.__sm__[sm_name].current_state;
				if(current_state == undefined || current_state === "")	// If no current state is defined
				{
					// loading initial state as current state
					current_state = cwf.sm.__sm__[sm_name].init;
					cwf.sm.__sm__[sm_name].current_state = current_state;
				}
	
				// Checking if "transition" exists in the current state
				if(transition != undefined && cwf.sm.__sm__[sm_name][current_state][transition] != undefined)
				{
					// execute the state leaving code, passing transition as an argument
					if(cwf.sm.__sm__[sm_name][current_state].__leaving != undefined)
					{
						if(!cwf.sm.__sm__[sm_name][current_state].__leaving(transition, data))
						{
							return; // if leaving code invalidate this transition, then 
									// the state does not change.
						}
					}
					
					// transition exists, getting the new page name
					page_name = cwf.sm.__sm__[sm_name][current_state][transition];
					
					// and update the current state.
					cwf.sm.__sm__[sm_name].current_state = page_name;
				}
				// Else, if transition does NOT exists in current state, and transition is not "undefined"
				else if(transition != undefined)
				{
					// transition does not exists in that state. The current state does
					//	not change. Execute state code, if any.
					if(cwf.sm.__sm__[sm_name][current_state].__in != undefined)
					{
						autotrans = cwf.sm.__sm__[sm_name][current_state].__in(transition, data);
						if(autotrans)
						{
							cwf.sm.dialog_ctrl(sm_name, autotrans, data);
							return;
						}
						return;
					}
					// State does NOT have any "loop-in" code
					else {
						cwf.logwarn("cwf.sm.dialog_ctrl(): '"+transition+"' transition does NOT exist in state "+current_state
							+" and this state does NOT contain a __in() function that could give a chance to interpret it");
					}
					return;
				}
				// Else, transition is undefined. This is only make sense if current state is initial state.
				else if(current_state == cwf.sm.__sm__[sm_name].init) {
					page_name = current_state;
				}
				else {
					cwf.logerr("cwf.sm.dialog_ctrl(): An undefined transition was sent to a non-initial state.");
					return;
				}
				
				// If the state exist, 
				if(cwf.sm.__sm__[sm_name][page_name] != undefined)
				{
					// execute state initialization code, if any
					if(cwf.sm.__sm__[sm_name][page_name].__pre != undefined)
					{
						autotrans = cwf.sm.__sm__[sm_name][page_name].__pre(data);
						if(autotrans)
						{
							cwf.sm.dialog_ctrl(sm_name, autotrans, data);
							return;
						}
					}
					
					// Check if HTML code need to be loaded or if the state is a pure logical state
					if(cwf.sm.__sm__[sm_name][page_name].__nohtml != undefined)
					{
						pur_state = true;
					}
					
					// Prepare code execution after HTML loading, if any, and if state is not pure
					if(cwf.sm.__sm__[sm_name][page_name].__post != undefined)
					{
						postprocess = function(loading_ok, errorCodeIfAny) 
									{ 
										autotrans = cwf.sm.__sm__[sm_name][page_name].__post(data, loading_ok, errorCodeIfAny);
										if(autotrans)
										{
											cwf.sm.dialog_ctrl(sm_name, autotrans, data);
											return;
										}
									};
					}
				}
			}
			else
			{
				// no state machine named "sm_name" exists.
				cwf.logerr("no state machine named \""+sm_name+"\" exists.");
			}
	
			// If state is not a pure logical state, loading the associated HTML code.
			if(!pur_state)
			{
				var target_sel = cwf.sm.__sm__[sm_name][cwf.sm.__sm__[sm_name].current_state].element_sel;
				if(target_sel == undefined) {
					target_sel = cwf.sm.__sm__[sm_name].element_sel;
				}
				
				var pages_folder = page_name;
				if(cwf.sm.__sm__[sm_name].pages_folder != undefined) {
					pages_folder = cwf.sm.__sm__[sm_name].pages_folder;
				}
				
				//cwf.logdebug("target_sel= \""+target_sel+"\" (current_state="+cwf.sm.__sm__[sm_name].current_state+") ");
				
				if(cwf.l10n.__lg__ === "")
				{
					cwf.sm.__load_page(target_sel, pages_folder, page_name, postprocess);
				}
				else
				{
					cwf.sm.__load_L10n_page(target_sel, pages_folder, page_name, cwf.l10n.__lg__, postprocess);
				}
			}
		},
		
		//--------------------------------------------------------------------------------
		// sm: private members	//////////////////////////////////////////////////////////
		
		__load_L10n_page:	function(content_sel, pages_folder, page_name, lang, postprocess)
		{
			$.ajax({	
							dataType: "json",
							url: cwf.sm.__pages_url__ + pages_folder + "/L10n/" + page_name + "." + lang + ".json",
							cache: false,
							success: function(data) 
							{
								cwf.l10n.__load_translations(data);
								cwf.sm.__load_page(content_sel, pages_folder, page_name, postprocess);
							},
							error: function (h, err_type, err)
							{
								var http_error = "";
								if(h.status != 200) {
									http_error = "Error "+h.status;
								}
								cwf.logerr("__load_L10n_page: "+http_error+" while loading " + cwf.sm.__pages_url__ + pages_folder + "/L10n/" + page_name + "." + lang + ".json ("+err_type+", "+err+")");
								cwf.sm.__load_page(content_sel, pages_folder, page_name, postprocess);
							}
						});
		},
		
		//--------------------------------------------------------------------------------
		
		// For debug purpose only
		/*__load_page:	function(content_id, page_name, postprocess)
		{
			cwf.ui.loading.show();
			var lp = function() { cwf.sm.__load_page_(content_id, page_name, postprocess); };
			setTimeout(lp, 3000);
		},*/
		
		//--------------------------------------------------------------------------------
		
		__load_page: function(content_sel, pages_folder, page_name, postprocess)
		{
			cwf.ui.loading.show();
			if(content_sel == undefined) {
				cwf.logerr("cwf.sm # content selector for inserting a state page is undefined");
			}
			
			$.ajax({	url: cwf.sm.__pages_url__ + pages_folder + "/" + page_name + ".inc.html", 
						cache: false,
						success: function(data) 
						{
							$(content_sel).html(data);
							cwf.ui.loading.hide();
	
							var title = $("cwf_title").html();
							if(title != null && title != "") {
								$("title").html(cwf.tr(title));
							}
							
							cwf.l10n.tr_doc();
							
							window.scrollTo(0, 0);
							
							if(postprocess !== null) {
								postprocess(true);	
							}
						},
						error:	function(h, err_type, err)
						{
							cwf.ui.loading.hide();
							if(cwf.__debug__) {
								cwf.logerr(	"cwf.sm # "+h.status+" "+err+" ERROR while loading "+ pages_folder + "/" + page_name);
							}
							
							if(cwf.sm.__default_page_loading_error_handler != null) {
								cwf.sm.__default_page_loading_error_handler(h.status);
							}
							
							if(postprocess !== null) {
								postprocess(false, h.status);
							}
						}
					});
		},
		
		//--------------------------------------------------------------------------------
		
		__pages_url__ : "pages/",
	
		__sm__: [],
		
		__default_page_loading_error_handler : null
	},
	
	
	
	date: 
	{
		timestampToDatejsObject: function(timestamp) 
		{
			var d = new Date(parseInt(timestamp));
			return Date.parse(d.getFullYear()+"-"+(d.getMonth()+1)+"-"+d.getDate()+" "+d.getHours()+":"+d.getMinutes()+":"+d.getSeconds());
		}
	},
	
	log: function(obj)
	{
		if(cwf.__debug__)
		{
			console.log(cwf.__formatObjectToLog(obj));
		}
	},
	
	logerr: function(message)
	{
		console.error(cwf.__formatObjectToLog(message));
	},
	
	logwarn: function(message)
	{
		console.warn(cwf.__formatObjectToLog(message));
	},
	
	logdebug: function(message)
	{
		console.info(cwf.__formatObjectToLog(message));
	},
	
	__formatObjectToLog: function(obj)
	{
		if(typeof obj == "string") {
			return obj;
		}
		else {
			return object_to_json(obj); 
		}
	},

	//------------------------------------------------------------------------------------
	
	log_storage: function()
	{
		cwf.log("<strong>&lt;localStorage: "+$.jStorage.storageSize()+" bytes&gt;</strong>");
		var idx = $.jStorage.index();
		var i;
		for(i=0; i < idx.length; i++) {
			cwf.log(idx[i]+"="+object_to_json($.jStorage.get(idx[i])));
		}
		cwf.log("<strong>&lt;end of localStorage&gt;</strong>");
	},
	
	//------------------------------------------------------------------------------------
	
	__debug__: false
};


$.fn.centerInClient = function(options) {
    /// <summary>Centers the selected items in the browser window. Takes into account scroll position.
    /// Ideally the selected set should only match a single element.
    /// </summary>    
    /// <param name="fn" type="Function">Optional function called when centering is complete. Passed DOM element as parameter</param>    
    /// <param name="forceAbsolute" type="Boolean">if true forces the element to be removed from the document flow 
    ///  and attached to the body element to ensure proper absolute positioning. 
    /// Be aware that this may cause ID hierachy for CSS styles to be affected.
    /// </param>
    /// <returns type="jQuery" />
    var opt = { forceAbsolute: false,
                container: window,    // selector of element to center in
                completeHandler: null
              };
    $.extend(opt, options);
   
    return this.each(function(i) {
        var el = $(this);
        var jWin = $(opt.container);
        var isWin = opt.container == window;

        // force to the top of document to ENSURE that 
        // document absolute positioning is available
        if (opt.forceAbsolute) {
            if (isWin)
                el.remove().appendTo("body");
            else
                el.remove().appendTo(jWin.get(0));
        }

        // have to make absolute
        el.css("position", "absolute");

        // height is off a bit so fudge it
        var heightFudge = isWin ? 2.0 : 1.8;

        var x = (isWin ? jWin.width() : jWin.outerWidth()) / 2 - el.outerWidth() / 2;
        var y = (isWin ? jWin.height() : jWin.outerHeight()) / heightFudge - el.outerHeight() / 2;

        el.css("left", x + jWin.scrollLeft());
        el.css("top", y + jWin.scrollTop());

        // if specified make callback and pass element
        if (opt.completeHandler)
            opt.completeHandler(this);
    });
};

if(typeof console === "undefined") {
    console = {
        log: function() { },
        debug: function() { },
        info: function() { },
        warn: function() { },
        error: function() { },
    };
}