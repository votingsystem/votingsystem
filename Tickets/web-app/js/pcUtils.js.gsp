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
      if ( ver<= 10.0 ) {
          alert("<g:message code='browserNosuportedMsg'/>")
	  }
   }
}

function openWindow(targetURL) {
    var width = 1000
    var height = 800
    var left = (screen.width/2) - (width/2);
    var top = (screen.height/2) - (height/2);
    var title = ''

    var newWindow =  window.open(targetURL, title, 'toolbar=no, scrollbars=yes, resizable=yes, '  +
            'width='+ width +
            ', height='+ height  +', top='+ top +', left='+ left + '');
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

var VotingSystemApplet = function () {
	
	this.messageToSignatureClient = null;
	this.messageToValidationTool = null;
	
	
	this.getMessageToSignatureClient = function (appMessage) {
		var result
		if(messageToSignatureClient != null) {
			console.log("getMessageToSignatureClient - delivering message to applet");
			result = messageToSignatureClient
			messageToSignatureClient = null
		}
		return result
	}

	this.getMessageToValidationTool = function (appMessage) {
		var result
		if(messageToValidationTool != null) {
			console.log("getMessageToValidationTool - delivering message to applet: " + appMessage);
			result = messageToValidationTool
			messageToValidationTool = null
		}
		return result
	}
	
	this.setMessageToValidationTool = function (message) {
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
	
	this.setMessageToSignatureClient = function (messageJSON, callerCallback) {
		var callerCallbackName = getFnName(callerCallback) 
		messageJSON.callerCallback = callerCallbackName
		var message = JSON.stringify(messageJSON)
		console.log(" - callerCallback: " + callerCallbackName + " - setMessageToSignatureClient: " + message);
		messageToSignatureClient = message;



	   	//var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SEND_SMIME_VOTE)
	   	//message = JSON.stringify(webAppMessage)


		//alert("'" + encodeURIComponent(message) + "'")
		if(isAndroid()) {
			console.log("=============== isAndroid browser")
			//to avoid URI too large
			//if(message.eventVS != null) message.eventVS.content = null;

			var redirectURL = "${createLink(controller:'app', action:'androidClient')}?msg=" + encodeURIComponent(message) + 
				"&refererURL=" + window.location + 
				"&serverURL=" + "${grailsApplication.config.grails.serverURL}"

			alert(redirectURL)
			window.location.href = redirectURL.replace("\n","")
			return
		}
		
		if(!signatureClientToolLoaded) {
			if(isJavaEnabledClient()) {
				console.log("Loading signature client");
				signatureClientCallback = callerCallback
				window.getMessageToSignatureClient = this.getMessageToSignatureClient
				$("#votingSystemAppletFrame").attr("src", '${createLink(controller:'applet', action:'client')}');
				$("#loadingVotingSystemAppletDialog").dialog("open");
			} 
    	} else {
    		if(callerCallback != null) {
    			var appletframe = document.getElementById('votingSystemAppletFrame');
    			appletframe.contentWindow[callerCallbackName] = callerCallback
    		}
    		console.log("signature client already loaded");
    		$("#workingWithAppletDialog").dialog("open");
	    } 
	}

}

function getFnName(fn) {
	  var f = typeof fn == 'function';
	  var s = f && ((fn.name && ['', fn.name]) || fn.toString().match(/function ([^\(]+)/));
	  return (!f && 'not a function') || (s && s[1] || 'anonymous');
}

var signatureClientCallback

var validationToolLoaded = false
var signatureClientToolLoaded = false

var votingSystemClient = new VotingSystemApplet()


var SocketService = function () {

    this.socket = null;

    this.connect = function () {
    	var host = "${grailsApplication.config.grails.serverURL}/websocket/service".replace('http', 'ws')
        if ('WebSocket' in window) {
            this.socket = new WebSocket(host);
        } else if ('MozWebSocket' in window) {
            this.socket = new MozWebSocket(host);
        } else {
            console.log('<g:message code="browserWithoutWebsocketSupport"/>');
            return
        }
        this.socket.onopen = function () {
            console.log('Info: WebSocket connection opened');
        };

        this.socket.onclose = function () {
            console.log('Info: WebSocket closed.');
        };
    }

    this.sendMessage = function(message) {
        var messageStr = JSON.stringify(message);
        console.log("sendMessage to simulation service: " + messageStr)
        if(this.socket == null || 3 == this.socket.readyState) {
            console.log("missing message - socket closed")
        } else if(messageStr != ''){
            this.socket.send(messageStr);
        }
    }

    this.close = function() {
        //states: CONNECTING	0, OPEN	1, CLOSING	2, CLOSED	3
        if(this.socket == null || 3 == this.socket.readyState) {
            console.log("socket already closed")
            return
        } else console.log(" closing socket connection")
        this.socket.close()
    }

};

var socketService = new SocketService()
socketService.connect()