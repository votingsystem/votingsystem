<div id="removeRepresentativeDialog" title="<g:message code="removeRepresentativeLbl"/>"  style="padding:20px 20px 20px 20px">
	<p style="text-align: center;"><g:message code="removeRepresentativeMsg"/></p>
	<p style="text-align: center;"><g:message code="clickAcceptToContinueLbl"/></p>
</div> 
<r:script>

$("#removeRepresentativeDialog").dialog({
   	  width: 450, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		$("#removeRepresentativeDialog").dialog("close");
	             	removeRepresentative() 
	             }},{
        		text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
	   				$(this).dialog( "close" );
	       	 	}}],
      show: { effect: "fade", duration: 100 },
      hide: { effect: "fade", duration: 100 }
    });

function removeRepresentative() {
	console.log("removeRepresentative")
   	var webAppMessage = new WebAppMessage(
	    	ResponseVS.SC_PROCESSING,
	    	Operation.REPRESENTATIVE_REVOKE)
   	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
	webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
	webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_REVOKE}
	webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStampVS', absolute:true)}"
	webAppMessage.receiverSignServiceURL = "${createLink(controller:'representative', action:'revoke', absolute:true)}"
	webAppMessage.signedMessageSubject = '<g:message code="removeRepresentativeMsgSubject"/>'
	votingSystemClient.setMessageToSignatureClient(webAppMessage, removeRepresentativeCallback); 
}

function removeRepresentativeCallback(appMessage) {
	console.log("removeRepresentativeCallback - message from native client: " + appMessage);
	var appMessageJSON = toJSON(appMessage)
	if(appMessageJSON != null) {
		$("#workingWithAppletDialog" ).dialog("close");
		var caption = '<g:message code="operationERRORCaption"/>'
		var msg = appMessageJSON.message
		if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
			caption = "<g:message code='operationOKCaption'/>"
			msg = "<g:message code='removeRepresentativeOKMsg'/>";
		} else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
			caption = "<g:message code='operationCANCELLEDLbl'/>"
		}
		showResultDialog(caption, msg)
	}
}

</r:script>