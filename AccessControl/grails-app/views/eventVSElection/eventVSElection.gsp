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
<html>
<head><meta name="layout" content="main" /></head>
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
	
	<div style="display:inline-block; width:100%; font-size:0.8em;">
		<div style="display:inline;margin:0px 20px 0px 20px;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinish}
		</div>
		<g:if test="${EventVS.State.ACTIVE.toString() == eventMap?.state ||
			EventVS.State.AWAITING.toString()}">
			<div id="adminDocumentLink" class="appLink" style="float:right;margin:0px 20px 0px 0px;">
				<g:message code="adminDocumentLinkLbl"/>
			</div>
		</g:if>
	</div>

	<div class="eventPageContentDiv">
		<div style="width:100%;position:relative;">
			<div class="eventContentDiv">${raw(eventMap?.content)}</div>
		</div>

		<div style="width:100%;position:relative;margin:0px 0px 0px 0px;">
			<div id="eventAuthorDiv"><b>
				<g:message code="publisshedByLbl"/>:</b>${eventMap?.userVS}
			</div>
		</div>
	
		<div class="eventOptionsDiv">
			<fieldset id="fieldsBox" style="">
				<legend id="fieldsLegend"><g:message code="pollFieldLegend"/></legend>
				<div id="fields" style="width:100%;">
					<g:if test="${EventVS.State.ACTIVE.toString() == eventMap?.state}">
						<g:each in="${eventMap?.fieldsEventVS}">
							<button class="voteOptionButton button_base"
								style="width: 90%;margin: 10px auto 30px auto;"
								optionId = "${it.id}" optionContent="${it.content}"  onclick="return false;">
								${it.content}
							</button>
						</g:each>
					</g:if>
					<g:if test="${EventVS.State.CANCELLED.toString() == eventMap?.state ||
						EventVS.State.TERMINATED.toString() == eventMap?.state ||
						EventVS.State.AWAITING.toString() == eventMap?.state}">
						<g:each in="${eventMap?.fieldsEventVS}">
							<div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
								 - ${it.content}
							</div>
						</g:each>
					</g:if>
				</div>
			</fieldset>
		</div>
	</div>

	<g:render template="/template/signatureMechanismAdvert"/>

<g:include view="/include/dialog/confirmOptionDialog.gsp"/>
<g:include view="/include/dialog/adminDocumentDialog.gsp"/>

</body>
</html>
<r:script>
<g:applyCodec encodeAs="none">
		var votingEvent = ${eventMap as JSON} 
		var selectedOption
		$(function() {
			if(${messageToUser != null?true:false}) {
				$("#eventMessagePanel").addClass("${eventClass}");
			}

	  		$(".voteOptionButton").click(function () { 
	  			$("#optionSelectedDialogMsg").text($(this).attr("optionContent"))
	  			selectedOption = {id:Number($(this).attr("optionId")),
	   			content:$(this).attr("optionContent")}
	  			console.log(" - selectedOption: " +  JSON.stringify(selectedOption))
	  			$("#confirmOptionDialog").dialog("open");
	  		});
		
	  		$("#adminDocumentLink").click(function () {
    			showAdminDocumentDialog(adminDocumentCallback)
		   	})

		 });
		         
		function sendVote() {
			console.log("sendVote")
			var voteVS = {optionSelected:selectedOption, eventId:votingEvent.id, eventURL:votingEvent.URL}
		   	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SEND_SMIME_VOTE)
		   	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
			webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
			votingEvent.voteVS = voteVS
			webAppMessage.eventVS = votingEvent
			webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStampVS', absolute:true)}"
			//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
			votingSystemClient.setMessageToSignatureClient(webAppMessage, sendVoteCallback); 
		}

		function sendVoteCallback(appMessage) {
			console.log("sendVoteCallback - message from native client: " + appMessage);
			var appMessageJSON = toJSON(appMessage)
			if(appMessageJSON != null) {
				$("#workingWithAppletDialog").dialog("close");
				caption = '<g:message code="voteERRORCaption"/>'
				var msgTemplate = "<g:message code='voteResultMsg'/>"
				var msg
				if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
					caption = "<g:message code='voteOKCaption'/>"
					msg = msgTemplate.format('<g:message code="voteResultOKMsg"/>',appMessageJSON.message);
				} else if(ResponseVS.SC_ERROR_VOTE_REPEATED == appMessageJSON.statusCode) {
					msgTemplate =  "<g:message code='accessRequestRepeatedMsg'/>" 
					msg = msgTemplate.format(msgTemplate1.format('${eventMap?.subject}'),appMessageJSON.message);
				} else msg = appMessageJSON.message
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