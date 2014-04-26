<div id='requestEventBackupDialog' title='<g:message code="backupRequestCaption"/>' style="display:none;">
	<div id="messageDiv" style='text-align:center;'>
		<g:message code="backupRequestMsg"/>
	</div>
	<form id="requestEventBackupForm">
		<div style="margin:15px 0px 20px 0px">
			<input type="email" id="eventBackupUserEmailText" style="width:360px; margin:0px auto 0px auto;" required
				title='<g:message code='enterEmailLbl'/>' class="form-control"
				placeholder='<g:message code='emailInputLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
				onchange="this.setCustomValidity('')"/>
		</div>
		<input id="submitBackupRequest" type="submit" style="display:none;">
	</form>
</div>
<r:script>

var callerCallback

function showRequestEventBackupDialog(callback, messageToUser) {
	$("#requestEventBackupDialog").dialog("open");
	callerCallback = callback
	if(messageToUser != null) $("#messageDiv").html(messageToUser);
}

$('#requestEventBackupForm').submit(function(event){
	event.preventDefault();
   	var webAppMessage = new WebAppMessage(
	    	ResponseVS.SC_PROCESSING,
	    	Operation.BACKUP_REQUEST)
   	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
	webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
	webAppMessage.serviceURL = "${createLink(controller:'backupVS', absolute:true)}"
	webAppMessage.signedMessageSubject = "${eventMap.subject}"
	webAppMessage.eventVS = pageEvent
	pageEvent.operation = Operation.BACKUP_REQUEST
	webAppMessage.signedContent = pageEvent
	webAppMessage.email = $("#eventBackupUserEmailText").val()
    webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
	pendingOperation = Operation.SMIME_CLAIM_SIGNATURE
	votingSystemClient.setMessageToSignatureClient(webAppMessage, callerCallback); 
});

$("#requestEventBackupDialog").dialog({
 	  width: 400, autoOpen: false, modal: true,
    buttons: [{id: "acceptButton",
      		text:"<g:message code="acceptLbl"/>",
             	icons: { primary: "ui-icon-check"},
           	click:function() {
           		$("#submitBackupRequest").click()
           		$(this).dialog( "close" ); 	   			   				
	        	}}, {id: "cancelButton",
	        		text:"<g:message code="cancelLbl"/>",
	               	icons: { primary: "ui-icon-closethick"},
	             	click:function() {
	   					$(this).dialog( "close" );
	       	 		}}],
    show: {effect:"fade", duration: 300},
    hide: {effect: "fade",duration: 300}
});
</r:script>