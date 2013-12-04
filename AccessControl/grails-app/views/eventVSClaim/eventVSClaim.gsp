<%@ page import="org.votingsystem.model.EventVS; grails.converters.JSON" %>
<%@ page import="org.votingsystem.accesscontrol.model.*" %>
<%
	def messageToUser = null
	def eventClass = null
	if(EventVS.State.TERMINATED.toString().equals(eventMap?.state)) {
		messageToUser =  message(code: 'claimEventFinishedLbl')
		eventClass = "eventFinishedBox"
	} else if(EventVS.State.AWAITING.toString().equals(eventMap?.state)) {
		messageToUser = message(code: 'claimEventPendingLbl')
		eventClass = "eventPendingBox"
	} else if(EventVS.State.CANCELLED.toString().equals(eventMap?.state)) {
		messageToUser = message(code: 'claimEventCancelledLbl')
		eventClass = "eventFinishedBox"
	}
%>
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

	<div class="publishPageTitle" style="margin:0px 0px 0px 0px;">
		<p style="margin: 0px 0px 0px 0px; text-align:center; width:100%;">
			${eventMap?.subject}
		</p>
	</div>
	
	<div style="width:100%; font-size:0.8em; margin:2px 0px 25px 0px;">
		<div style="display:inline;margin:0px 20px 0px 20px;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinish}
		</div>
		<g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state) ||
			EventVS.State.AWAITING.toString().equals(eventMap?.state)}">
			<div id="adminDocumentLink" class="appLink" style="float:right;margin:0px 20px 0px 0px;">
				<g:message code="adminDocumentLinkLbl"/>
			</div>
		</g:if>
	</div>

	<div class="eventPageContentDiv">
		<div style="">
			<div class="eventContentDiv">${raw(eventMap?.content)}</div>
		</div>
		
		<div style="width:100%; height: 50px;">
			<g:if test="${eventMap?.numSignatures > 0}">
				<div style="float:left;margin:0px 0px 0px 40px;">
					<votingSystem:simpleButton id="requestBackupButton"  
						style="margin:0px 20px 0px 0">
						<g:message code="numClaimsForEvent" args="${[eventMap?.numSignatures]}"/>
					</votingSystem:simpleButton>
				</div>
			</g:if>
			<div id="eventAuthorDiv" style="float:right;top:0px;">
				<b><g:message code="publisshedByLbl"/>:</b>${eventMap?.userVS}
			</div>
		</div>
		
		<form id="submitClaimForm">
		<g:if test="${eventMap?.fieldsEventVS.size() > 0}">
			<div class="eventOptionsDiv">
				<fieldset id="fieldsBox" style="">
					<legend id="fieldsLegend"><g:message code="claimsFieldLegend"/></legend>
					<div id="fields" style="width:100%;">
						<g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
							<g:each in="${eventMap?.fieldsEventVS}">
				  				<input type='text' id='claimField${it.id}' required 
				  					class='claimFieldInput'
	  								title='${it.content}' placeholder='${it.content}'
	   								oninvalid="this.setCustomValidity('<g:message code='emptyFieldLbl'/>')"
	   								onchange="this.setCustomValidity('')" />
							</g:each>
						</g:if>
						<g:if test="${EventVS.State.CANCELLED.toString().equals(eventMap?.state) ||
							EventVS.State.TERMINATED.toString().equals(eventMap?.state) ||
							EventVS.State.AWAITING.toString().equals(eventMap?.state)}">
							<g:each in="${eventMap?.fieldsEventVS}">
								<div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
									 - ${it.content}
								</div>
							</g:each>
						</g:if>
					</div>
				</fieldset>
			</div>
		</g:if>
		<g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
			<div style="overflow: hidden;">
				<votingSystem:simpleButton id="signClaimFieldButton" isSubmitButton='true'
					style="margin:0px 20px 0px 0px; float:right;">
						<g:message code="signClaim"/>
				</votingSystem:simpleButton>
			</div>
		</g:if>
		</form>
	</div>
	
	<g:render template="/template/signatureMechanismAdvert"/>	

<g:include view="/include/dialog/adminDocumentDialog.gsp"/>
<g:include view="/include/dialog/requestEventBackupDialog.gsp"/>
	
</body>
</html>
<r:script>
<g:applyCodec encodeAs="none">
       	var pageEvent = ${eventMap as JSON} 
       	var fieldsArray = new Array();
	 	$(function() {
			if(${messageToUser != null?true:false}) { 
				$("#eventMessagePanel").addClass("${eventClass}");
			}
			
    		$("#adminDocumentLink").click(function () {
    			showAdminDocumentDialog(adminDocumentCallback)
	    	})

		    $('#submitClaimForm').submit(function(event){
		        event.preventDefault();
		        sendSignature()
		    });
    		$("#requestBackupButton").click(function () {
    			showRequestEventBackupDialog(requestBackupCallback)
	    	})
			    
		 });

		function sendSignature() {
			console.log("sendSignature")
	    	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SMIME_CLAIM_SIGNATURE)
	    	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
    		webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
   			webAppMessage.receiverSignServiceURL = "${createLink( controller:'eventVSClaimCollector', absolute:true)}"
  				webAppMessage.signedMessageSubject = "${eventMap.subject}"
			webAppMessage.eventVS = pageEvent

			var fieldsArray = new Array();
			<g:each in="${eventMap?.fieldsEventVS}" status="i" var="claimField">
				fieldsArray[${i}] = {id:${claimField?.id}, content:'${claimField?.content}', value:$("#claimField${claimField?.id}").val()}
			</g:each>
			pageEvent.fieldsEventVS = fieldsArray
			pageEvent.operation = Operation.SMIME_CLAIM_SIGNATURE
			webAppMessage.signedContent = pageEvent
			webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStampVS', absolute:true)}"
			webAppMessage.isResponseWithReceipt = true
			//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
			votingSystemClient.setMessageToSignatureClient(webAppMessage, sendSignatureCallback); 
		}

		function requestBackupCallback(appMessage) {
			console.log("requestBackupCallback");
			var appMessageJSON = toJSON(appMessage)
			if(appMessageJSON != null) {
				if(ResponseVS.SC_PROCESSING == appMessageJSON.statusCode){
					$("#loadingVotingSystemAppletDialog").dialog("close");
					$("#workingWithAppletDialog").dialog("open");
				} else {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="operationERRORCaption"/>'
					if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
						caption = "<g:message code='operationOKCaption'/>"
					}
					var msg = appMessageJSON.message
					showResultDialog(caption, msg)
				}
			}
		}

		function sendSignatureCallback(appMessage) {
			console.log("sendSignatureCallback - message from native client: " + appMessage);
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