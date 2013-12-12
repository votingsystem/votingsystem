<div id="adminDocumentDialog" title="<g:message code="cancelEventCaption"/>">
	<p style="text-align: center;"><g:message code="adminDocumenInfoMsg"/></p>
	<g:message code="documentStateSelectionMsg"/>:<br/>
	<div style="font-size: 0.9em; margin:10px 0 0 10px;"> 
		<div style="margin:0px 0 10px 0px;display:block;">
			<input type="checkbox" id="selectDeleteDocument"/><label for="selectDeleteDocument"><g:message code="selectDeleteDocumentMsg"/></label>
		</div>
		<input type="checkbox" id="selectCloseDocument"/><g:message code="selectCloseDocumentMsg"/>
	</div>
</div> 
<r:script>


var callerCallback

function showAdminDocumentDialog(callback) {
	$("#adminDocumentDialog").dialog("open");
	callerCallback = callback	
}


$("#selectDeleteDocument").click(function () {
	if($("#selectCloseDocument").is(':checked')) {
		$("#selectCloseDocument").prop('checked', false);
	}
})

$("#selectCloseDocument").click(function () {
	if($("#selectDeleteDocument").is(':checked')) {
		$("#selectDeleteDocument").prop('checked', false);
	}
})

$("#adminDocumentDialog").dialog({
 	  width: 600, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		submitAdminForm() 	
             		$(this).dialog( "close" );   	   			   				
 			        	}
           },
           {
        		text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
		   					$(this).dialog( "close" );
		       	 		}	
           }],
      show: {effect:"fade", duration: 300},
      hide: {effect: "fade",duration: 300}
    });

function submitAdminForm() {
	console.log("adminDocumentDialog.submitAdminForm()")
	if(!$("#selectDeleteDocument").is(':checked') &&
			!$("#selectCloseDocument").is(':checked')) {
		showResultDialog("<g:message code='errorLbl'/>", 
				"<g:message code='selectDocumentStateERRORMsg'/>", showAdminDocumentDialog)
	} else {
		var state
		if($("#selectDeleteDocument").is(':checked')) {
			state = EventVS.State.DELETED_FROM_SYSTEM
		} else if($("#selectCloseDocument").is(':checked')) {
			state = EventVS.State.CANCELLED
		}
    	var webAppMessage = new WebAppMessage(
		    	ResponseVS.SC_PROCESSING,
		    	Operation.EVENT_CANCELLATION)
    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
		webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStampVS', absolute:true)}"
		webAppMessage.receiverSignServiceURL= "${createLink(controller:'eventVS', action:'cancelled', absolute:true)}"
		var signedContent = {operation:Operation.EVENT_CANCELLATION,
				accessControlURL:"${grailsApplication.config.grails.serverURL}",
				eventId:Number("${eventMap?.id}"), state:state}
		webAppMessage.signedContent = signedContent
		pendingOperation = Operation.EVENT_CANCELLATION
		//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
		votingSystemClient.setMessageToSignatureClient(webAppMessage, callerCallback);
	}
}
</r:script>