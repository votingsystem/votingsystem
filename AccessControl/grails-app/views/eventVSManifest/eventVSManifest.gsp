<%@ page import="org.votingsystem.model.EventVS; grails.converters.JSON" %>
<%@ page import="org.votingsystem.accesscontrol.model.*" %>
<%
	def messageToUser = null
	def eventClass = null
    if(eventMap?.state) {
        switch(EventVS.State.valueOf(eventMap?.state)) {
            case EventVS.State.CANCELLED:
                messageToUser = message(code: 'eventCancelledLbl')
                eventClass = "eventCancelleddBox"
                break;
            case EventVS.State.AWAITING:
                messageToUser = message(code: 'eventPendingLbl')
                eventClass = "eventPendingBox"
                break;
            case EventVS.State.TERMINATED:
                messageToUser =  message(code: 'eventFinishedLbl')
                eventClass = "eventFinishedBox"
                break;
        }
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

    <div class="pageHeader"> ${eventMap?.subject}</div>
	
	<div style="" class="row">
        <div id="pendingTimeDiv" style="float:left; margin:0 0 0 60px; color: #388746; font-weight: bold;"></div>
        <div class="datetime text-left" style="display:inline;margin:0px 10px 0px 60px; float:left;">
			<b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinishStr}
		</div>
		
		<g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)  ||
			EventVS.State.AWAITING.toString().equals(eventMap?.state)}">
			<div id="adminDocumentLink" class="appLink" style="float:right;margin:0px 50px 0px 0px;">
				<g:message code="adminDocumentLinkLbl"/>
			</div>
		</g:if>
	</div>

	<div class="eventPageContentDiv">
		<div style="width:100%;position:relative;">
			<div class="eventContentDiv">${raw(eventMap?.content)}</div>
		</div>
		
		<div class="row">
			<g:if test="${eventMap?.numSignatures > 0}">
				<div style="float:left;margin:10px 0px 0px 40px;">
                    <button id="requestBackupButton" type="button" class="btn btn-default btn-lg" style="margin:0px 20px 0px 0;">
                        <g:message code="numSignaturesForEvent" args="${[eventMap?.numSignatures]}"/>
                    </button>
				</div>
			</g:if>
			<div id="eventAuthorDiv" style="float:right;top:0px;">
				<b><g:message code="publishedByLbl"/>: </b>${eventMap?.userVS}
			</div>
		</div>
		
		<g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
            <button id="signManifestButton" type="submit" class="btn btn-default btn-lg" style="margin:15px 20px 30px 0px; float:right;">
                <g:message code="signManifest"/> <i class="fa fa fa-check"></i>
            </button>
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
    if(pageEvent.state == "ACTIVE") {
        $(".pageHeader").css("color", "#388746")
        var pendingMsgTemplate = '<g:message code='pendingMsgTemplate'/>'
            $("#pendingTimeDiv").text(pendingMsgTemplate.format(pageEvent.dateFinish.getElapsedTime()))
    }
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
        webAppMessage.serviceURL = "${createLink( controller:'eventVSManifestCollector', absolute:true)}/${eventMap.id}"
        webAppMessage.signedMessageSubject = "${eventMap.subject}"
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature, application/x-pkcs7-mime'
        webAppMessage.eventVS = pageEvent
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.documentURL = pageEvent.URL
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
            var caption
            var msg
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='operationOKCaption'/>"
                var msgTemplate = "<g:message code='documentCancellationOKMsg'/>"
                msg = msgTemplate.format('${eventMap?.subject}');
                callBack = function() {
                    var eventVSService = "${createLink(controller:'eventVSManifest')}/"
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