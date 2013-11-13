<div id='requestEventBackupDialog' title='<g:message code="backupRequestCaption"/>' style="display:none;">
	<div style='text-align:center;'>
		<g:message code="backupRequestMsg"/>
	</div>
	<form id="requestEventBackupForm">
		<div style="margin:15px 0px 20px 0px">
			<input type="email" id="eventBackupUserEmailText" style="width:360px; margin:0px auto 0px auto;" required
				title='<g:message code='enterEmailLbl'/>'
				placeholder='<g:message code='emailInputLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
				onchange="this.setCustomValidity('')"/>
		</div>
		<input id="submitBackupRequest" type="submit" style="display:none;">
	</form>
</div>
<r:script>

var callerCallback

function showRequestEventBackupDialog(callback) {
	$("#requestEventBackupDialog").dialog("open");
	callerCallback = callback	
}

$('#requestEventBackupForm').submit(function(event){
	event.preventDefault();
   	var webAppMessage = new WebAppMessage(
	    	StatusCode.SC_PROCESSING, 
	    	Operation.BACKUP_REQUEST)
   	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
	webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
	webAppMessage.urlEnvioDocumento = "${createLink(controller:'solicitudCopia', absolute:true)}"
	webAppMessage.asuntoMensajeFirmado = "${eventMap.asunto}"
	webAppMessage.evento = pageEvent
	pageEvent.operation = Operation.BACKUP_REQUEST
	webAppMessage.contenidoFirma = pageEvent
	webAppMessage.emailSolicitante = $("#eventBackupUserEmailText").val()
	webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStamp', absolute:true)}"
	webAppMessage.respuestaConRecibo = true
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