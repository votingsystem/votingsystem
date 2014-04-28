window.onload=function(){
	$("#votingSystemAppletFrame").attr("src", "");
	$("#validationToolAppletFrame").attr("src", "");
	checkIEVersion()
};


//http://www.mkyong.com/javascript/how-to-detect-ie-version-using-javascript/
function getInternetExplorerVersion() {
// Returns the version of Windows Internet Explorer or a -1
// (indicating the use of another browser).
   var rv = -1; // Return value assumes failure.
   if (navigator.appName == 'Microsoft Internet Explorer')
   {
      var ua = navigator.userAgent;
      var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
      if (re.exec(ua) != null)
         rv = parseFloat( RegExp.$1 );
   }
   return rv;
}

function checkIEVersion() {
   var ver = getInternetExplorerVersion();
   if ( ver> -1 ) {
      if ( ver<= 9.0 ) {
          alert("<g:message code='browserNosuportedMsg'/>")
	  }
   }
}

function isChrome () {
	return (navigator.userAgent.toLowerCase().indexOf("chrome") > - 1);
}

function isAndroid () {
	return (navigator.userAgent.toLowerCase().indexOf("android") > - 1);
}

function isFirefox () {
	return (navigator.userAgent.toLowerCase().indexOf("firefox") > - 1);
}


function isJavaEnabledClient() {
	if(!(deployJava.versionCheck('1.8') || deployJava.versionCheck('1.7'))) {
		console.log("---- checkJavaEnabled -> browser without Java7 or Java8 ");
		$("#browserWithoutJavaDialog").dialog("open")
		return false
	} else return true
}

function getFnName(fn) {
	  var f = typeof fn == 'function';
	  var s = f && ((fn.name && ['', fn.name]) || fn.toString().match(/function ([^\(]+)/));
	  return (!f && 'not a function') || (s && s[1] || 'anonymous');
}

function VotingSystemApplet () {
	this.validationToolLoaded = false
    this.signatureClientToolLoaded = false
	this.messageToSignatureClient = null;
	this.messageToValidationTool = null;
	this.signatureClientCallback = null
}

VotingSystemApplet.prototype.getMessageToSignatureClient = function (appMessage) {
		var result
		if(this.messageToSignatureClient != null) {
			console.log("getMessageToSignatureClient - delivering message to applet");
			result = this.messageToSignatureClient
			this.messageToSignatureClient = null
		}
		return result
	}

VotingSystemApplet.prototype.getMessageToValidationTool = function (appMessage) {
		var result
		if(messageToValidationTool != null) {
			console.log("getMessageToValidationTool - delivering message to applet: " + appMessage);
			result = messageToValidationTool
			messageToValidationTool = null
		}
		return result
	}


VotingSystemApplet.prototype.setMessageToValidationTool = function (message) {
		console.log("utils.js - setMessageToValidationTool - message:" + message);
		messageToValidationTool = message;
		if(!validationToolLoaded) {
			if(isJavaEnabledClient()) {
				console.log("Loading validationTool")
				window.getMessageToValidationTool = this.getMessageToValidationTool
				$("#validationToolAppletFrame").attr("src", '${createLink(controller:'applet', action:'validationTool')}');
				$("#loadingVotingSystemAppletDialog").dialog("open");

			}
    	} else {
    		console.log("setMessageToValidationTool - validationToolLoaded already loaded");
    		$("#workingWithAppletDialog").dialog("open");
	    }
	}

VotingSystemApplet.prototype.setMessageToSignatureClient = function (messageJSON, callerCallback) {
		var callerCallbackName = getFnName(callerCallback)
        messageJSON.callerCallback = callerCallbackName
		this.signatureClientCallback = callerCallback

		this.messageToSignatureClient = JSON.stringify(messageJSON)
		console.log(" - callerCallback: " + callerCallbackName + " - setMessageToSignatureClient: " + this.messageToSignatureClient);

	   	//var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SEND_SMIME_VOTE)
	   	//this.messageToSignatureClient = JSON.stringify(webAppMessage)


		//alert("'" + encodeURIComponent(messageToSignatureClient) + "'")
		if(isAndroid()) {
			console.log("=============== isAndroid browser")
			//to avoid URI too large
			//if(messageToSignatureClient.eventVS != null) messageToSignatureClient.eventVS.content = null;

			var redirectURL = "${createLink(controller:'app', action:'androidClient')}?msg=" + encodeURIComponent(messageToSignatureClient) +
				"&refererURL=" + window.location +
				"&serverURL=" + "${grailsApplication.config.grails.serverURL}"

			alert(redirectURL)
			window.location.href = redirectURL.replace("\n","")
			return
		}

		if(!this.signatureClientToolLoaded) {
			if(isJavaEnabledClient()) {
				console.log("Loading signature client");
				$("#votingSystemAppletFrame").attr("src", '${createLink(controller:'applet', action:'client')}');
				$("#loadingVotingSystemAppletDialog").dialog("open");
			}
    	} else {
    		console.log("signature client already loaded");
    		$("#workingWithAppletDialog").dialog("open");
	    }
	}

VotingSystemApplet.prototype.setMessageFromSignatureClient = function (appMessage) {
		var appMessageJSON = toJSON(appMessage)
		if(appMessageJSON != null) {
			if(ResponseVS.SC_PROCESSING == appMessageJSON.statusCode){
				this.signatureClientToolLoaded = true;
				$("#loadingVotingSystemAppletDialog").dialog("close");
				$("#workingWithAppletDialog").dialog("open");
			} else {
		        this.signatureClientCallback(appMessage)
		    }
		}
	}

var votingSystemClient = new VotingSystemApplet()
