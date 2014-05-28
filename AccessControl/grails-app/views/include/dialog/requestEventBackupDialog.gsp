<div id='requestEventBackupDialog' class="modal fade">
    <div class="modal-dialog">
        <form id="requestEventBackupForm">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="resultCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="backupRequestCaption"/>
                </h4>
            </div>
            <div class="modal-body">
                <div id="requestEventBackupDialogMessageDiv" class='text-center'
                     style="color: #6c0404; font-size: 1.2em;font-weight: bold; margin-bottom: 15px;"></div>

                <div class='text-center'>
                    <g:message code="backupRequestMsg"/>
                </div>

                    <div style="margin:15px 0px 20px 0px">
                        <input type="email" id="eventBackupUserEmailText" style="width:360px; margin:0px auto 0px auto;" required
                               title='<g:message code='enterEmailLbl'/>' class="form-control"
                               placeholder='<g:message code='emailInputLbl'/>'
                               oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
                               onchange="this.setCustomValidity('')"/>
                    </div>
                    <input id="submitBackupRequest" type="submit" style="display:none;">
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
<asset:script>

var callerCallback

function showRequestEventBackupDialog(callback, messageToUser) {
	$("#requestEventBackupDialog").modal("show");
	callerCallback = callback
	if(messageToUser != null) $("#requestEventBackupDialogMessageDiv").html(messageToUser);
}

$('#requestEventBackupForm').submit(function(event){
	event.preventDefault();
	$('#requestEventBackupDialogMessageDiv').html("")
	if(!document.getElementById('requestEventBackupForm').checkValidity()) {
	    $('#requestEventBackupDialogMessageDiv').html("<g:message code="formErrorMsg"/>");
        return false
	}
   	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.BACKUP_REQUEST)
   	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
	webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
	webAppMessage.serviceURL = "${createLink(controller:'backupVS', absolute:true)}"
    webAppMessage.signedMessageSubject = '<g:message code="requestEventvsBackupMsgSubject"/>'
	webAppMessage.eventVS = pageEvent
	pageEvent.operation = Operation.BACKUP_REQUEST
	webAppMessage.signedContent = pageEvent
	webAppMessage.email = $("#eventBackupUserEmailText").val()
    webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
    webAppMessage.callerCallback = getFnName(callerCallback)

	VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
});

</asset:script>