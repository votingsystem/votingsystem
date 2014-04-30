<%@ page import="org.votingsystem.model.EventVS; grails.converters.JSON" %>
<%@ page import="org.votingsystem.accesscontrol.model.*" %>
<%
	def messageToUser = null
	def eventClass = null
    if(eventMap?.state) {
        switch(EventVS.State.valueOf(eventMap?.state)) {
            case EventVS.State.CANCELLED:
                messageToUser = message(code: 'claimEventCancelledLbl')
                eventClass = "eventCancelleddBox"
                break;
            case EventVS.State.AWAITING:
                messageToUser = message(code: 'claimEventPendingLbl')
                eventClass = "eventPendingBox"
                break;
            case EventVS.State.TERMINATED:
                messageToUser =  message(code: 'claimEventFinishedLbl')
                eventClass = "eventFinishedBox"
                break;
        }
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

    <div class="pageHeader"> ${eventMap?.subject}</div>

    <div style="" class="row">
        <div id="pendingTimeDiv" style="float:left; margin:0 0 0 60px; color: #388746; font-weight: bold;"></div>
        <div class="datetime text-left" style="display:inline;margin:0px 10px 0px 60px; float:left;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinishStr}
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

        <div class="row">
			<g:if test="${eventMap?.numSignatures > 0}">
                <div style="float:left;margin:10px 0px 0px 40px;">
                    <button id="requestBackupButton" type="button" class="btn btn-default btn-lg" style="margin:0px 20px 0px 0;">
                        <g:message code="numClaimsForEvent" args="${[eventMap?.numSignatures]}"/>
                    </button>
				</div>
			</g:if>
			<div id="eventAuthorDiv" style="float:right;top:0px;">
				<b><g:message code="publishedByLbl"/>: </b>${eventMap?.userVS}
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
				  					class='form-control'
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
                <button id="signClaimFieldButton" type="submit" class="btn btn-default btn-lg" style="margin:20px 20px 0px 0px; float:right;">
                    <g:message code="signClaim"/> <i class="fa fa-check"></i>
                </button>
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
    if(pageEvent.state == "ACTIVE") {
        $(".pageHeader").css("color", "#388746")
        var pendingMsgTemplate = '<g:message code='pendingMsgTemplate'/>'
                $("#pendingTimeDiv").text(pendingMsgTemplate.format(pageEvent.dateFinish.getElapsedTime()))
    }
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
            <g:if test="${eventMap?.backupAvailable}">
                showRequestEventBackupDialog(requestBackupCallback)
            </g:if>
            <g:else>
                showRequestEventBackupDialog(requestBackupCallback, "<g:message code="backupOnlyForPublisherMsg"/>")
            </g:else>
        })
     });

    function sendSignature() {
        console.log("sendSignature")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SMIME_CLAIM_SIGNATURE)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink( controller:'eventVSClaimCollector', absolute:true)}"
            webAppMessage.signedMessageSubject = "${eventMap.subject}"
        webAppMessage.eventVS = pageEvent

        var fieldsArray = new Array();
        <g:each in="${eventMap?.fieldsEventVS}" status="i" var="claimField">
            fieldsArray[${i}] = {id:${claimField?.id}, content:'${claimField?.content}', value:$("#claimField${claimField?.id}").val()}
        </g:each>
        pageEvent.fieldsEventVS = fieldsArray
        pageEvent.operation = Operation.SMIME_CLAIM_SIGNATURE
        webAppMessage.signedContent = pageEvent
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
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
            showResultDialog(caption, "<g:message code='operationOKMsg'/>")
        }
    }

    function adminDocumentCallback(appMessage) {
        console.log("adminDocumentCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            $("#workingWithAppletDialog").dialog("close");
            var callBack
            var caption
            var msg
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='operationOKCaption'/>"
                var msgTemplate = "<g:message code='documentCancellationOKMsg'/>"
                msg = msgTemplate.format('${eventMap?.subject}');
                callBack = function() {
                    var eventVSService = "${createLink(controller:'eventVSClaim')}/"
                    window.location.href = eventVSService.concat(pageEvent.id);
                }
            } else {
                caption = "<g:message code='operationERRORCaption'/>"
                msg = "<g:message code='operationERRORCaption'/>"
            }
            showResultDialog(caption, msg, callBack)
        }
    }
</g:applyCodec>
</r:script>