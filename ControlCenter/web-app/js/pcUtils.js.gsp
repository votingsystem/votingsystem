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

function updateSubsystem(selectedSubsystem) {
	console.log(" - selectedSubsystem: " + selectedSubsystem)
	var subsystem_0_0_Link
	var subsystem_0_0_Text
	var subsystem_0_1_Link
	var subsystem_0_1_Text
	var subsystem_0_2_Link
	var subsystem_0_2_Text
	var selectedSubsystemLink
	var selectedSubsystemText
	if(SubSystem.VOTES == selectedSubsystem) {
		subsystem_0_0_Link = "${createLink(controller: 'eventVSManifest', action: 'mainPage')}"
		subsystem_0_0_Text = "<g:message code="manifestLbl"/>"
		subsystem_0_1_Link = "${createLink(controller: 'eventVSClaim', action: 'mainPage')}"
		subsystem_0_1_Text = "<g:message code="claimLbl"/>"
		selectedSubsystemLink = "${createLink(controller: 'eventVSElection', action: 'mainPage')}"
		selectedSubsystemText = "<g:message code="votingSystemLbl"/>"

	} else if(SubSystem.CLAIMS == selectedSubsystem) {
		subsystem_0_0_Link = "${createLink(controller: 'eventVSElection', action: 'mainPage')}"
		subsystem_0_0_Text = "<g:message code="votingLbl"/>"
		subsystem_0_1_Link = "${createLink(controller: 'eventVSManifest', action: 'mainPage')}"
		subsystem_0_1_Text = "<g:message code="manifestLbl"/>"
		selectedSubsystemLink = "${createLink(controller: 'eventVSClaim', action: 'mainPage')}"
		selectedSubsystemText = "<g:message code="claimSystemLbl"/>"
	} else if(SubSystem.MANIFESTS == selectedSubsystem) {
		subsystem_0_0_Link = "${createLink(controller: 'eventVSElection', action: 'mainPage')}"
		subsystem_0_0_Text = "<g:message code="votingLbl"/>"
		subsystem_0_1_Link = "${createLink(controller: 'eventVSClaim', action: 'mainPage')}"
		subsystem_0_1_Text = "<g:message code="claimLbl"/>"
		selectedSubsystemLink = "${createLink(controller: 'eventVSManifest', action: 'mainPage')}"
		selectedSubsystemText = "<g:message code="manifestSystemLbl"/>"
	} else if(SubSystem.REPRESENTATIVES == selectedSubsystem) {
		subsystem_0_0_Link = "${createLink(controller: 'eventVSElection', action: 'mainPage')}"
		subsystem_0_0_Text = "<g:message code="votingLbl"/>"
		subsystem_0_1_Link = "${createLink(controller: 'eventVSClaim', action: 'mainPage')}"
		subsystem_0_1_Text = "<g:message code="claimLbl"/>"
		subsystem_0_2_Link = "${createLink(controller: 'eventVSManifest', action: 'mainPage')}"
		subsystem_0_2_Text = "<g:message code="manifestSystemLbl"/>"
		selectedSubsystemLink = "${createLink(controller: 'representative', action: 'mainPage')}"
		selectedSubsystemText = "<g:message code="representativesPageLbl"/>"
	} else {
		console.log("### updateSubsystem - unknown subsytem -> " + selectedSubsystem)
	}
	$('#subsystem_0_0_Link').attr('href',subsystem_0_0_Link);
	$('#subsystem_0_0_Link').text(subsystem_0_0_Text)
	$('#subsystem_0_1_Link').attr('href',subsystem_0_1_Link);
	$('#subsystem_0_1_Link').text(subsystem_0_1_Text)
	$('#subsystem_0_2_Link').attr('href',subsystem_0_2_Link);
	$('#subsystem_0_2_Link').text(subsystem_0_2_Text)
	$('#selectedSubsystemLink').attr('href',selectedSubsystemLink);
	$('#selectedSubsystemLink').text(selectedSubsystemText)
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
			console.log("getMessageToSignatureClient - delivering message to org.votingsystem.applet");
			result = messageToSignatureClient
			messageToSignatureClient = null
		}
		return result
	}

	this.getMessageToValidationTool = function (appMessage) {
		var result
		if(messageToValidationTool != null) {
			console.log("getMessageToValidationTool - delivering message to org.votingsystem.applet: " + appMessage);
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
				$("#validationToolAppletFrame").attr("src", '${createLink(controller:'org.votingsystem.applet', action:'herramientaValidacion')}');
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
				$("#votingSystemAppletFrame").attr("src", '${createLink(controller:'org.votingsystem.applet', action:'cliente')}');
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

var editorConfig = {toolbar: [[ 'Bold', 'Italic', '-', 'NumberedList', 'BulletedList', '-', 'Link', 'Unlink' ],
  	      					[ 'FontSize', 'TextColor', 'BGColor' ]]}
	
//"yy/MM/dd 12:00:00"
var pickerOpts = {showOn: 'both', buttonImage: "${createLinkTo(dir: 'images', file: 'appointment.png')}", 
		buttonImageOnly: true, dateFormat: 'yy/MM/dd'};

var numMaxEventsForPage = 12

var validationToolLoaded = false
var signatureClientToolLoaded = false

var votingSystemClient = new VotingSystemApplet()
