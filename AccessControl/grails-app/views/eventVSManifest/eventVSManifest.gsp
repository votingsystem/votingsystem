<%@ page import="grails.converters.JSON; org.votingsystem.model.EventVS;" %>
<!DOCTYPE html>
<html>
<head>
        <meta name="layout" content="main" />
</head>
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

        <div class="pageHeader text-center"><h3>${eventMap?.subject}</h3></div>

        <div style="" >
            <div id="pendingTimeDiv" style="float:left; margin:0 0 0 60px; color: #388746; font-weight: bold;"></div>
            <div class="datetime" style="display:inline;margin:0px 10px 0px 60px; float:left;">
                <b><g:message code="dateLimitLbl"/>: </b>${eventMap?.dateFinishStr}
            </div>
        </div>

        <div class="eventPageContentDiv">
            <div style="width:100%;position:relative;">
                <div class="eventContentDiv">${raw(eventMap?.content)}</div>
            </div>

            <div class="row">
                <g:if test="${eventMap?.numSignatures > 0}">
                    <div style="float:left;margin:10px 0px 0px 40px;">
                        <button id="requestBackupButton" type="button" class="btn btn-default btn-lg"
                                onclick="showRequestEventBackupDialog(requestBackupCallback);" style="margin:0px 20px 0px 0;">
                            <g:message code="numSignaturesForEvent" args="${[eventMap?.numSignatures]}"/>
                        </button>
                    </div>
                </g:if>
                <div id="eventAuthorDiv" style="float:right;top:0px;">
                    <b><g:message code="publishedByLbl"/>: </b>${eventMap?.userVS}
                </div>
            </div>

            <g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
                <button id="signManifestButton" type="submit" class="btn btn-default btn-lg" onclick="sendManifest();"
                        style="margin:15px 20px 30px 0px; float:right;">
                    <g:message code="signManifest"/> <i class="fa fa fa-check"></i>
                </button>
            </g:if>
        </div>
    </div>
</div>
<g:include view="/include/dialog/adminDocumentDialog.gsp"/>
<g:include view="/include/dialog/requestEventBackupDialog.gsp"/>
</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">

    var pageEvent = ${eventMap as JSON}

    $(function() {

        <g:if test="${EventVS.State.ACTIVE.toString().equals(eventMap?.state)}">
            $(".pageHeader").css("color", "#388746")
            var pendingMsgTemplate = '<g:message code='pendingMsgTemplate'/>'
                pendingMsgTemplate.format(pageEvent.dateFinish.getElapsedTime())
        </g:if>
        <g:elseif test="${EventVS.State.AWAITING.toString().equals(eventMap?.state)}">
            $(".pageHeader").css("color", "#388746")
            $("#messagePanel").addClass("eventPendingBox");
            $("#messagePanel").text("<g:message code="eventPendingLbl"/>")
        </g:elseif>
        <g:elseif test="${EventVS.State.CANCELLED.toString().equals(eventMap?.state)}">
            $(".pageHeader").css("color", "#fba131")
            $("#messagePanel").addClass("eventCancelledBox");
            $("#messagePanel").text("<g:message code="eventCancelledLbl"/>")
                        $("#messagePanel").css("display", "visible")

        </g:elseif>
        <g:elseif test="${EventVS.State.TERMINATED.toString().equals(eventMap?.state)}">
            $(".pageHeader").css("color", "#6c0404")
            $("#messagePanel").addClass("eventFinishedBox");
            $("#messagePanel").text("<g:message code="eventFinishedLbl"/>")
                        $("#messagePanel").css("display", "visible")
        </g:elseif>

        else {
            $("#signManifestButton").addClass("disabled")
            //$("#requestBackupButton").addClass("disabled")

        }
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
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        webAppMessage.eventVS = pageEvent
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.documentURL = pageEvent.URL
        webAppMessage.callerCallback = 'sendManifestCallback'
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function requestBackupCallback(appMessage) {
        console.log("requestBackupCallback");
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
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

</g:applyCodec>
</asset:script>