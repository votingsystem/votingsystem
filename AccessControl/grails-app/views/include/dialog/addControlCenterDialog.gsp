<div id='addControlCenterDialog' class="modal fade">
    <div class="modal-dialog">
        <form id="newControlCenter">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title" style="color: #6c0404; font-weight: bold;">
                        <g:message code="controlCenterLbl"/>
                    </h4>
                </div>
                <div class="modal-body">
                    <div id="addControlCenterDialogMessageDiv" class='text-center'
                         style="color: #6c0404; font-size: 1.2em;font-weight: bold; margin-bottom: 15px;"></div>

                    <div id="formDiv">
                        <p style="text-align: center;">
                            <g:message code="controlCenterDescriptionMsg"/>
                        </p>
                        <span><g:message code="controlCenterURLLbl"/></span>
                            <input type="url" id="controlCenterURL" style="width:500px; margin:0px auto 0px auto;"
                                   oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                                   onchange="this.setCustomValidity('')" class="form-control" required/>
                            <input id="submitControlCenter" type="submit" style="display:none;">

                    </div>

                    <div id="checkControlCenterProgressDiv" style="display:none;">
                        <p style='text-align: center;'><g:message code="checkingControlCenterLbl"/></p>
                        <progress style='display:block;margin:0px auto 10px auto;'></progress>
                    </div>
                </div>
                <div class="modal-footer">
                    <button id="" type="submit" class="btn btn-accept-vs">
                        <g:message code="acceptLbl"/>
                    </button>
                    <button id="" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                        <g:message code="cancelLbl"/>
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>
<r:script>

function showVoteControlCenterDialog(callback) {
    $('#controlCenterURL').val("")
    $("#formDiv").show()
    $("#checkRepresentativeDialogProgressDiv").hide()
	$("#addControlCenterDialog").modal("show");
}


$('#newControlCenter').submit(function(event){
	event.preventDefault();
    $('#addControlCenterDialogMessageDiv').html("")
    if(!document.getElementById('newControlCenter').checkValidity()) {
	    $('#addControlCenterDialogMessageDiv').html("<g:message code="formErrorMsg"/>");
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
					$("#addControlCenterDialog").modal("show")
				}) 
		}
		}).fail(function(data) {
			var dataStr = JSON.stringify(data);  
			console.log( "error asssociating Control Center - dataStr: " + dataStr);
			console.log("error asssociating Control Center");
			showResultDialog('<g:message code="errorLbl"/>',
				'<g:message code="controlCenterURLERRORMsg"/>', function() {
					$("#addControlCenterDialog").modal("show")
				}) 
		}).always(function() {
            $("#formDiv").show()
            $("#checkControlCenterProgressDiv").hide()
		});
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
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.signedMessageSubject = '<g:message code="addControlCenterMsgSubject"/>'
		webAppMessage.serviceURL = "${createLink( controller:'subscriptionVS', absolute:true)}"
        webAppMessage.callerCallback = 'associateControlCenterCallback'
		VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
	} 

	function associateControlCenterCallback(callbackMessage){ 
		console.log("addControlCenterDialog.associateControlCenterCallback")
		var appMessageJSON = toJSON(callbackMessage)
		if(appMessageJSON != null) {
            var caption = '<g:message code="operationERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK ==  appMessageJSON.statusCode) {
                caption = '<g:message code="operationOKCaption"/>'
                var msgArg =
                msg = "<g:message code='operationOKMsg'/>";
                $("#addControlCenterDialog").dialog("open")
            }
            $("#addControlCenterDialog").modal("hide");
            showResultDialog(caption, msg)
		}
	}	
</r:script>