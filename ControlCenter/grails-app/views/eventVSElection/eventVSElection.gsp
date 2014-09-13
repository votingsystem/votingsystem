<%@ page import="org.votingsystem.model.EventVS; grails.converters.JSON" %>
<html>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
<body>
<div class="pageContentDiv" style="max-width: 1000px;">
    <div style="margin:0 20px 0 20px;">
        <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
        </div>

    <div class="pageHeader text-center"><h3>${eventMap?.subject}</h3></div>

    <div>
        <div id="pendingTimeDiv" style="float:left; margin:0 0 0 60px; color: #388746; font-weight: bold;"></div>
        <div class="datetime text-left" style="display:inline;margin:0px 10px 0px 60px; float:left;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinishStr}
		</div>
	</div>

	<div>
		<div style="width:100%;position:relative;">
			<div class="eventContentDiv">${raw(eventMap?.content)}</div>
		</div>

        <div id="eventAuthorDiv" class="text-left" style="margin:0px 20px 20px 0px;">
            <b><g:message code="accessControlEventvsURL" args="${[eventMap?.URL]}"/></b>
        </div>

        <div id="eventAuthorDiv" class="text-right" style="margin:0px 20px 20px 0px;">
            <b><g:message code="publishedByLbl"/>: </b>${eventMap?.userVS}
        </div>

        <div class="fieldsBox">
            <fieldset style="margin:30px auto 0 auto;">
				<legend><g:message code="pollFieldLegend"/></legend>
				<div id="fields" style="width:100%;">
                    <g:each in="${eventMap?.fieldsEventVS}">
                        <div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
                            - ${it.content}
                        </div>
                    </g:each>
				</div>
			</fieldset>
		</div>
	</div>
    </div>
</div>

</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">
		var votingEvent = ${eventMap as JSON}
        var elapsedTime = votingEvent.dateFinish.getElapsedTime()

		var selectedOption
		$(function() {
            <g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
                $(".pageHeader").css("color", "#388746")
                var pendingMsgTemplate = '<g:message code='pendingMsgTemplate'/>'
                        if(undefined != elapsedTime) $("#pendingTimeDiv").append(pendingMsgTemplate.format(elapsedTime))
            </g:if>
            <g:if test="${EventVS.State.PENDING.toString().equals(eventMap?.state)}">
                $(".pageHeader").css("color", "#388746")
                $("#messagePanel").addClass("eventPendingBox");
                $("#messagePanel").text("<g:message code="eventPendingLbl"/>")
            </g:if>
            <g:if test="${EventVS.State.CANCELLED.toString().equals(eventMap?.state)}">
                $(".pageHeader").css("color", "#fba131")
                $("#messagePanel").addClass("eventCancelledBox");
                $("#messagePanel").text("<g:message code="eventCancelledLbl"/>")
                        $("#messagePanel").css("display", "visible")

            </g:if>
            <g:if test="${EventVS.State.TERMINATED.toString().equals(eventMap?.state)}">
                $(".pageHeader").css("color", "#6c0404")
                $("#messagePanel").addClass("eventFinishedBox");
                $("#messagePanel").text("<g:message code="eventFinishedLbl"/>")
                        $("#messagePanel").css("display", "visible")
            </g:if>


			if(${messageToUser != null?true:false}) {
				$("#eventMessagePanel").addClass("${eventClass}");
			}
		 });
		         
		function sendVote() {
			console.log("sendVote")
			var voteVS = {optionSelected:selectedOption, eventId:votingEvent.id, eventURL:votingEvent.URL}
		   	var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SEND_SMIME_VOTE)
		   	webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
			webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
			votingEvent.voteVS = voteVS
			webAppMessage.eventVS = votingEvent
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            webAppMessage.callerCallback = 'sendVoteCallback'
			//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
			VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
		}

		function sendVoteCallback(appMessage) {
			console.log("sendVoteCallback - message: " + appMessage);
			var appMessageJSON = toJSON(appMessage)
				caption = '<g:message code="voteERRORCaption"/>'
				var msgTemplate = "<g:message code='voteResultMsg'/>"
				var msg
				if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
					caption = "<g:message code='voteOKCaption'/>"
					msg = msgTemplate.format(
							'<g:message code="voteResultOKMsg"/>',
							appMessageJSON.message);
				} else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
					msgTemplate =  "<g:message code='accessRequestRepeatedMsg'/>"
					msg = msgTemplate.format(
						msgTemplate1.format('${eventMap?.subject}'),
						appMessageJSON.message);
				} else msg = appMessageJSON.message
				showResultDialog(caption, msg)
		}

		function adminDocumentCallback(appMessage) {
			console.log("adminDocumentCallback - message: " + appMessage);
			var appMessageJSON = toJSON(appMessage)
			if(appMessageJSON != null) {
				var callBack
                caption = "<g:message code='operationOKCaption'/>"
                msgTemplate = "<g:message code='documentCancellationOKMsg'/>"
                msg = msgTemplate.format('${eventMap?.subject}');
                callBack = function() {
                    window.location.href = "${createLink(controller:'eventVSClaim')}/" + claimEvent.id;
                }
				showResultDialog(caption, msg, callBack)
			}
		}
</g:applyCodec>
</asset:script>