<%@ page import="org.votingsystem.model.EventVS; grails.converters.JSON" %>
<%@ page import="org.votingsystem.accesscontrol.model.*" %>
<%
	def messageToUser = null
	def eventClass = null
	if(EventVS.State.TERMINATED.toString().equals(eventMap?.state)) {
		messageToUser =  message(code: 'eventFinishedLbl')
		eventClass = "eventFinishedBox"
	} else if(EventVS.State.AWAITING.toString().equals(eventMap?.state)) {
		messageToUser = message(code: 'eventPendingLbl')
		eventClass = "eventPendingBox"
	} else if(EventVS.State.CANCELLED.toString().equals(eventMap?.state)) {
		messageToUser = message(code: 'eventCancelledLbl')
		eventClass = "eventFinishedBox"
	}
%>
<!DOCTYPE html>
<html>
<head>
        <meta name="layout" content="main" />
</head>
<body>

	<g:if test="${messageToUser != null}">
		<div id="eventMessagePanel" class="eventMessagePanel">
			<p class="messageContent">
				${messageToUser}
			</p>
		</div>
	</g:if>

    <div class="publishPageTitle"> ${eventMap?.subject}</div>
	
	<div style="display:inline-block; width:100%; font-size:0.8em;">
		<div class="datetime"  style="display:inline;margin:0px 20px 0px 60px;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinishStr}
		</div>
		
		<g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)  ||
			EventVS.State.AWAITING.toString().equals(eventMap?.state)}">
			<div id="adminDocumentLink" class="appLink" style="float:right;margin:0px 20px 0px 0px;">
				<g:message code="adminDocumentLinkLbl"/>
			</div>
		</g:if>
	</div>

	<div class="eventPageContentDiv">
		<div style="width:100%;position:relative;">
			<div class="eventContentDiv">${raw(eventMap?.content)}</div>
		</div>
		
		<div style="width:100%; height: 50px;">
			<g:if test="${eventMap?.numSignatures > 0}">
				<div style="float:left;margin:0px 0px 0px 40px;">
					<votingSystem:simpleButton id="requestBackupButton"  
						style="margin:0px 20px 0px 0">
						<g:message code="numSignaturesForEvent" args="${[eventMap?.numSignatures]}"/>
					</votingSystem:simpleButton>
				</div>
			</g:if>
			<div id="eventAuthorDiv" style="float:right;top:0px;">
				<b><g:message code="publisshedByLbl"/>: </b>${eventMap?.userVS}
			</div>
		</div>
		
		<g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
			<votingSystem:simpleButton id="signManifestButton"  isSubmitButton='true'
				style="margin:15px 20px 30px 0px; float:right;"
				imgSrc="${resource(dir:'images/fatcow_16',file:'accept.png')}">
					<g:message code="signManifest"/>
			</votingSystem:simpleButton>
		</g:if>
	</div>

	<g:render template="/template/signatureMechanismAdvert"/>


<g:include view="/include/dialog/adminDocumentDialog.gsp"/>
<g:include view="/include/dialog/requestEventBackupDialog.gsp"/>
</body>
</html>
<r:script>
<g:applyCodec encodeAs="none">
        	var pageEvent = ${eventMap as JSON} 
		 	$(function() {
				if(${messageToUser != null?true:false}) { 
					$("#eventMessagePanel").addClass("${eventClass}");
				}
			 	
	    		$("#adminDocumentLink").click(function () {
	    			showAdminDocumentDialog(adminDocumentCallback)
		    	})
		    	
	    		$("#signManifestButton").click(function () {
	    			sendManifest();
		    	})
		    	
	    		$("#requestBackupButton").click(function () {
	    			showRequestEventBackupDialog(requestBackupCallback)
		    	})
		    	
			 });

			function sendManifest() {
				console.log("sendManifest")
		    	var webAppMessage = new WebAppMessage(
				    	ResponseVS.SC_PROCESSING,
				    	Operation.MANIFEST_SIGN)
		    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
	    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
    			webAppMessage.receiverSignServiceURL = "${createLink( controller:'eventVSManifestCollector', absolute:true)}/${eventMap.id}"
   				webAppMessage.signedMessageSubject = "${eventMap.subject}"
		    	//signed and encrypted
    			webAppMessage.contentType = 'application/x-pkcs7-signature, application/x-pkcs7-mime'
   				webAppMessage.isResponseWithReceipt = false
	    		webAppMessage.eventVS = pageEvent
				webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStampVS', absolute:true)}"
				webAppMessage.urlDocumento = pageEvent.URL
				votingSystemClient.setMessageToSignatureClient(webAppMessage, sendManifestCallback); 
			}

			function requestBackupCallback(appMessage) {
				console.log("requestBackupCallback");
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="operationERRORCaption"/>'
					if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
						caption = "<g:message code='operationOKCaption'/>"
					}
					var msg = appMessageJSON.message
					showResultDialog(caption, msg)
				}
			}

			function sendManifestCallback(appMessage) {
				console.log("eventSignatureCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="operationERRORCaption"/>'
					if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
						caption = "<g:message code='operationOKCaption'/>"
					} else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
						caption = "<g:message code='operationCANCELLEDLbl'/>"
					}
					var msg = appMessageJSON.message
					showResultDialog(caption, msg)
				}
			}

			function adminDocumentCallback(appMessage) {
				console.log("adminDocumentCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog").dialog("close");
					var callBack
					if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
						caption = "<g:message code='operationOKCaption'/>"
						msgTemplate = "<g:message code='documentCancellationOKMsg'/>"
						msg = msgTemplate.format('${eventMap?.subject}');
						callBack = function() {
							window.location.href = "${createLink(controller:'eventVSClaim')}/" + claimEvent.id;
						}
					}
					showResultDialog(caption, msg, callBack)
				}
			}
</g:applyCodec>
</r:script>