<div id="addControlCenterDialog" title="<g:message code="controlCenterLbl"/>" style="display:none;">
  	<div id="formDiv">
        <p style="text-align: center;">
            <g:message code="controlCenterDescriptionMsg"/>
        </p>
   		<span><g:message code="controlCenterURLLbl"/></span>
   		<form id="newControlCenter">
   			<input type="url" id="controlCenterURL" style="width:500px; margin:0px auto 0px auto;" 
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"
   				class="text ui-widget-content ui-corner-all" required/>
  				<input id="submitControlCenter" type="submit" style="display:none;">
   		</form>

  	</div>

    <div id="checkControlCenterProgressDiv" style="display:none;">
        <p style='text-align: center;'><g:message code="checkingControlCenterLbl"/></p>
        <progress style='display:block;margin:0px auto 10px auto;'></progress>
    </div>
</div> 
<r:script>

function showVoteControlCenterDialog(callback) {
	$("#addControlCenterDialog").dialog("open");
}


$('#newControlCenter').submit(function(event){
	event.preventDefault();	      
	if(!document.getElementById('controlCenterURL').validity.valid) {
		$("#controlCenterURL").addClass( "formFieldError" );
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
	controlCenterURL = controlCenterURL + "serverInfo"
	if(controlCenterURL.indexOf("http://") != 0) {
		controlCenterURL = "http://" + controlCenterURL
	}
    $("#formDiv").hide()
    $("#checkControlCenterProgressDiv").show()

	console.log("checking control center at: " + controlCenterURL)
	var jqxhr = $.getJSON(controlCenterURL, function() {});
	jqxhr.done(function(data) {
		//var dataStr = JSON.stringify(data);  
		//console.log( "second success - dataStr: " + dataStr);
		if("CONTROL_CENTER" == data.serverType) {
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
		}).always(function() {
            $("#formDiv").show()
            $("#checkControlCenterProgressDiv").hide()
		});
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
        }}],
        show: {effect:"fade", duration: 700},
        hide: {effect: "fade",duration: 700},
        open: function( event, ui ) {
            $('#controlCenterURL').val("")
            $("#formDiv").show()
            $("#checkRepresentativeDialogProgressDiv").hide()
        }
        });
    
	function associateControlCenter(controlCenterURL){ 
		console.log("addControlCenterDialog.associateControlCenter - controlCenterURL: " + controlCenterURL);
	 	var webAppMessage = new WebAppMessage(
	   		ResponseVS.SC_PROCESSING,
	   		Operation.CONTROL_CENTER_ASSOCIATION)
	 	var signatureContent = {
			serverURL:controlCenterURL,
			operation:Operation.CONTROL_CENTER_ASSOCIATION}
	 	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
		webAppMessage.signedContent = signatureContent
		webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
		webAppMessage.serviceURL = "${createLink( controller:'subscriptionVS', absolute:true)}"
		votingSystemClient.setMessageToSignatureClient(webAppMessage, associateControlCenterCallback)
	} 

	function associateControlCenterCallback(callbackMessage){ 
		console.log("addControlCenterDialog.associateControlCenterCallback")
		var appMessageJSON = toJSON(callbackMessage)
		if(appMessageJSON != null) {
			if(ResponseVS.SC_PROCESSING ==  appMessageJSON.statusCode){
				$("#loadingVotingSystemAppletDialog").dialog("close");
				$("#workingWithAppletDialog").dialog("open");
			} else {
				$("#workingWithAppletDialog" ).dialog("close");
				var caption = '<g:message code="operationERRORCaption"/>'
				var msg = appMessageJSON.message
				if(ResponseVS.SC_OK ==  appMessageJSON.statusCode) {
					caption = '<g:message code="operationOKCaption"/>'
					var msgArg = 
			    	msg = "<g:message code='operationOKMsg'/>";
			    	$("#addControlCenterDialog").dialog("open")
				}
                $("#addControlCenterDialog").dialog("close");
				showResultDialog(caption, msg)
			}
		}
	}	
</r:script>