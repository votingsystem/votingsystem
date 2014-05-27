<div id='adminDocumentDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="" class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="cancelEventCaption"/>
                </h4>
            </div>
            <div class="modal-body">
                <p style="text-align: center;"><g:message code="adminDocumenInfoMsg"/></p>
                <g:message code="documentStateSelectionMsg"/>:<br/>
                <div style="font-size: 0.9em; margin:10px 0 0 10px;">
                    <div class="radio">
                        <label>
                            <input type="radio" name="optionsRadios" id="selectDeleteDocument" value="">
                            <g:message code="selectDeleteDocumentMsg"/>
                        </label>
                    </div>
                    <div class="radio">
                        <label>
                            <input type="radio" name="optionsRadios" id="selectCloseDocument" value="">
                            <g:message code="selectCloseDocumentMsg"/>
                        </label>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-accept-vs" data-dismiss="modal" onclick="submitAdminForm();">
                    <g:message code="acceptLbl"/></button>
                <button type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal"><g:message code="cancelLbl"/></button>
            </div>
        </div>
    </div>
</div>
<r:script>

function showAdminDocumentDialog(callback) {
	$("#adminDocumentDialog").modal('show');
}

function submitAdminForm() {
	console.log("adminDocumentDialog.submitAdminForm()")
	if(!$("#selectDeleteDocument").is(':checked') &&
			!$("#selectCloseDocument").is(':checked')) {
		showResultDialog("<g:message code='errorLbl'/>", 
				"<g:message code='selectDocumentStateERRORMsg'/>", showAdminDocumentDialog)
	} else {
		var state
		var messageSubject
		if($("#selectDeleteDocument").is(':checked')) {
			state = EventVS.State.DELETED_FROM_SYSTEM
			messageSubject = '<g:message code="deleteEventVSMsgSubject"/>'
		} else if($("#selectCloseDocument").is(':checked')) {
			state = EventVS.State.CANCELLED
			messageSubject = '<g:message code="cancelEventVSMsgSubject"/>'
		}
    	var webAppMessage = new WebAppMessage(
		    	ResponseVS.SC_PROCESSING,
		    	Operation.EVENT_CANCELLATION)
    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
		webAppMessage.serviceURL= "${createLink(controller:'eventVS', action:'cancelled', absolute:true)}"
		var signedContent = {operation:Operation.EVENT_CANCELLATION,
				accessControlURL:"${grailsApplication.config.grails.serverURL}",
				eventId:Number("${eventMap?.id}"), state:state}
		webAppMessage.signedContent = signedContent
        webAppMessage.signedMessageSubject = messageSubject
		console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
        webAppMessage.callerCallback = 'adminDocumentCallback'
		VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
	}
}

function adminDocumentCallback(appMessage) {
    console.log("adminDocumentCallback - message from native client: " + appMessage);
    var appMessageJSON = toJSON(appMessage)
    if(appMessageJSON != null) {
        var callBack
        var caption
        var msg
        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
            caption = "<g:message code='operationOKCaption'/>"
            var msgTemplate = "<g:message code='documentCancellationOKMsg'/>"
            msg = msgTemplate.format('${eventMap?.subject}');
            callBack = function() {
                var eventVSService = "${createLink(controller:'eventVSElection')}/"
                window.location.href = eventVSService.concat(votingEvent.id);
            }
        } else {
            caption = "<g:message code='operationERRORCaption'/>"
            msg = appMessageJSON.message
        }
        showResultDialog(caption, msg, callBack)
    }
}
</r:script>