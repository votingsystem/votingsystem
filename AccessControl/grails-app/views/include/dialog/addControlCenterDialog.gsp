<div id="addControlCenterDialog" title="<g:message code="controlCenterLbl"/>">
	<p style="text-align: center;">
		<g:message code="controlCenterDescriptionMsg"/>
  	</p>
  	<div>
   		<span><g:message code="controlCenterURLLbl"/></span>
   		<form id="newControlCenter">
   			<input type="url" id="controlCenterURL" style="width:500px; margin:0px auto 0px auto;" 
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"
   				class="text ui-widget-content ui-corner-all" required/>
  				<input id="submitControlCenter" type="submit" style="display:none;">
   		</form>

  	</div>
</div> 
<r:script>

var callerCallback

function showVoteControlCenterDialog(callback) {
	$("#addControlCenterDialog").dialog("open");
	callerCallback = callback	
}


$('#newControlCenter').submit(function(event){
	event.preventDefault();	      
	if(!document.getElementById('controlCenterURL').validity.valid) {
		$("#controlCenterURL").addClass( "ui-state-error" );
		showResultDialog('<g:message code="dataFormERRORLbl"/>', 
			'<g:message code="emptyFieldMsg"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}	      
	var controlCenterURL = $('#controlCenterURL').val()
	var suffix = "/"
	if((controlCenterURL.indexOf(suffix, controlCenterURL.length - suffix.length) == -1)) {
		controlCenterURL = controlCenterURL + "/"
	}
	controlCenterURL = controlCenterURL + "infoServidor"
	if(controlCenterURL.indexOf("http://") != 0) {
		controlCenterURL = "http://" + controlCenterURL
	}
	console.log("checking control center at: " + controlCenterURL)
	var jqxhr = $.getJSON(controlCenterURL, function() {});
	jqxhr.done(function(data) {
		//var dataStr = JSON.stringify(data);  
		//console.log( "second success - dataStr: " + dataStr);
		if(DataType.CONTROL_CENTER == data.serverType) { 
			associateControlCenter(data.serverURL)
		} else {
			console.log( "Server type wrong -> " + data.serverType);
			showResultDialog('<g:message code="errorLbl"/>',
				'<g:message code="controlCenterURLERRORMsg"/>', function() {
					$("#addControlCenterDialog").dialog("open")
				}) 
		}
		}).fail(function(data) {
			var dataStr = JSON.stringify(data);  
			console.log( "error asssociating Control Center - dataStr: " + dataStr);
			console.log("error asssociating Control Center");
			showResultDialog('<g:message code="errorLbl"/>',
				'<g:message code="controlCenterURLERRORMsg"/>', function() {
					$("#addControlCenterDialog").dialog("open")
				}) 
		}).always(function() {});
});


$("#addControlCenterDialog").dialog({
   	  width: 600, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
 			   				$("#submitControlCenter").click() 	   	   			   				
 			        	}},
           {text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
	   				$(this).dialog( "close" );
	   				if(callerCallback != null) callerCallback()
       	 	}}],
      show: {effect:"fade", duration: 700},
      hide: {effect: "fade",duration: 700},
      open: function( event, ui ) {
    	  $('#controlCenterURL').val("")
	  }
    });
    
	function associateControlCenter(controlCenterURL){ 
		console.log("addControlCenterDialog.associateControlCenter - controlCenterURL: " + controlCenterURL);
	 	var webAppMessage = new WebAppMessage(
	   		StatusCode.SC_PROCESSING, 
	   		Operation.CONTROL_CENTER_ASSOCIATION)
	 	var signatureContent = {
			serverURL:controlCenterURL,
			operation:Operation.CONTROL_CENTER_ASSOCIATION}
	 	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
		webAppMessage.contenidoFirma = signatureContent
		webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
		webAppMessage.urlEnvioDocumento = "${createLink( controller:'subscripcion', absolute:true)}"
		votingSystemClient.setMessageToSignatureClient(webAppMessage, associateControlCenterCallback)
	} 

	function associateControlCenterCallback(callbackMessage){ 
		console.log("addControlCenterDialog.associateControlCenterCallback")
		var appMessageJSON = toJSON(callbackMessage)
		if(appMessageJSON != null) {
			if(StatusCode.SC_PROCESSING ==  appMessageJSON.statusCode){
				$("#loadingVotingSystemAppletDialog").dialog("close");
				$("#workingWithAppletDialog").dialog("open");
			} else {
				$("#workingWithAppletDialog" ).dialog("close");
				var caption = '<g:message code="operationERRORCaption"/>'
				var msg = appMessageJSON.message
				if(StatusCode.SC_OK ==  appMessageJSON.statusCode) { 
					caption = '<g:message code="operationOKCaption"/>'
					var msgArg = 
			    	msg = "<g:message code='operationOKMsg' args='${[message(code:'addControlCenterOperation')]}'/>";
			    	$("#addControlCenterDialog").dialog("open")
				}
				showResultDialog(caption, msg)
			}
		}
	}	
</r:script>