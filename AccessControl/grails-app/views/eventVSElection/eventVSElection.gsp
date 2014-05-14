<%@ page import="grails.converters.JSON; org.votingsystem.model.EventVS;" %>
<html>
<head>
    <meta name="layout" content="main" /></head>
<body>
<div class="pageContentDiv" style="max-width: 1000px;">
    <div style="margin:0 20px 0 20px;">
        <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
        </div>

        <g:if test="${"admin".equals(params.menu)}">
            <g:include view="/include/dialog/adminDocumentDialog.gsp"/>
            <div class="text-center" style="">
                <g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state) ||
                        EventVS.State.AWAITING.toString().equals(eventMap?.state)}">
                    <button type="submit" class="btn btn-warning" onclick="showAdminDocumentDialog();"
                            style="margin:15px 20px 15px 0px;">
                        <g:message code="adminDocumentLinkLbl"/> <i class="fa fa fa-check"></i>
                    </button>
                </g:if>
            </div>
        </g:if>

        <h3><div class="pageHeader text-center">${eventMap?.subject}</div></h3>

        <div class="row" style="display:inline;">
            <div class="" style="margin:0px 0px 0px 30px; display: inline;"><b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinishStr}</div>
            <div id="pendingTimeDiv" class="text-right" style="margin:0px 40px 0px 0px; color: #388746; display: inline; white-space:nowrap;"></div>
        </div>

        <div class="eventPageContentDiv">
            <div style="width:100%;position:relative;">
                <div class="eventContentDiv">${raw(eventMap?.content)}</div>
            </div>

            <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 0px;">
                <b><g:message code="publishedByLbl"/>: </b>${eventMap?.userVS}
            </div>

            <div class="eventOptionsDiv row text-center" style="">
                <fieldset id="fieldsBox" class="fieldsBox" style="margin:30px auto 0 auto;">
                    <legend id="fieldsLegend"><g:message code="pollFieldLegend"/></legend>
                    <div id="fields" class="" style="width:100%;">
                        <g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
                            <g:each in="${eventMap?.fieldsEventVS}">
                                <div class="btn btn-default btn-lg voteOptionButton"
                                     style="width: 90%;margin: 10px auto 30px auto;"
                                     optionId = "${it.id}" optionContent="${it.content}"  onclick="return false;">
                                    ${it.content}
                                </div>
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
                        <div id="clientToolMsg" class="text-center" style="color:#870000; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
                            <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
                    </div>
                </fieldset>
            </div>
        </div>
    </div>

    <g:include view="/include/dialog/confirmOptionDialog.gsp"/>

</div>
</body>
</html>
<r:script>
    <g:applyCodec encodeAs="none">
        var votingEvent = ${eventMap as JSON}
        var selectedOption
        var elapsedTime = votingEvent.dateFinish.getElapsedTime()
        $(function() {
            <g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
                $(".pageHeader").css("color", "#388746")
                var pendingMsgTemplate = '<g:message code='pendingMsgTemplate'/>'
                if(undefined != elapsedTime) $("#pendingTimeDiv").append(pendingMsgTemplate.format(elapsedTime))
            </g:if>
            <g:if test="${EventVS.State.AWAITING.toString().equals(eventMap?.state)}">
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
                $(".pageHeader").css("color", "#870000")
                $("#messagePanel").addClass("eventFinishedBox");
                $("#messagePanel").text("<g:message code="eventFinishedLbl"/>")
                $("#messagePanel").css("display", "visible")
            </g:if>

	  		$(".voteOptionButton").click(function () {
	  			$("#optionSelectedDialogMsg").text($(this).attr("optionContent"))
	  			selectedOption = {id:Number($(this).attr("optionId")),
	   			content:$(this).attr("optionContent")}
	  			console.log(" - selectedOption: " +  JSON.stringify(selectedOption))
	  			$("#confirmOptionDialog").modal("show");
	  		});
            if(isClientToolLoaded()) $("#clientToolMsg").css("display", "none")
            else {
                $(".voteOptionButton").each(function( index ) {
                    $(this).addClass("disabled")
                });
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
			//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
			votingSystemClient.setMessageToSignatureClient(webAppMessage, sendVoteCallback); 
		}

		function sendVoteCallback(appMessage) {
			console.log("sendVoteCallback - message from native client: " + appMessage);
			var appMessageJSON = toJSON(appMessage)
			if(appMessageJSON != null) {
				var caption = '<g:message code="voteERRORCaption"/>'
				var msgTemplate = "<g:message code='voteResultMsg'/>"
				var msg
				if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
					caption = "<g:message code='voteOKCaption'/>"
					msg = msgTemplate.format('<g:message code="voteResultOKMsg"/>',appMessageJSON.message);
				} else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
					msgTemplate =  "<g:message code='accessRequestRepeatedMsg'/>"
					msg = msgTemplate.format(votingEvent.subject, appMessageJSON.message);
				} else msg = appMessageJSON.message
				showResultDialog(caption, msg)
			}
		}

    </g:applyCodec>
</r:script>