<%@ page import="org.votingsystem.model.GroupVS; grails.converters.JSON" %>
<!DOCTYPE html>
<html>
<head>
    <link href="${resource(dir: 'css', file:'vicket_groupvs.css')}" type="text/css" rel="stylesheet"/>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv" style="max-width: 1000px; padding: 20px;">
    <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
    </div>

    <div class="pageHeader text-center"> ${groupvsMap?.name}</div>

    <div style="margin: 15px 0 15px 0; padding:10px; border: 1px solid #388746;">
        <div class="eventContentDiv">${raw(groupvsMap?.description)}</div>
    </div>

    <div class="row">
        <div id="eventAuthorDiv" style="float:right;top:0px;">
            <b><g:message code="groupRepresentativeLbl"/>: </b>${groupvsMap?.representative.firstName} ${groupvsMap?.representative.lastName}
        </div>
    </div>

    <g:if test="${GroupVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">
        <div class="row text-right">
            <button id="subscribeButton" type="submit" class="btn btn-default btn-lg" onclick="subscribeToGroup();"
                    style="margin:15px 20px 15px 0px;">
                <g:message code="subscribeGroupVSLbl"/> <i class="fa fa fa-check"></i>
            </button>
        </div>
    </g:if>

    <div class="row"><g:render template="/template/signatureMechanismAdvert"/></div>
</div>
<g:include view="/include/dialog/loadingAppletDialog.gsp"/>
<g:include view="/include/dialog/workingWithAppletDialog.gsp"/>
<g:include view="/include/dialog/resultDialog.gsp"/>
<div id="appletsFrame"  style="width:0px; height:0px;">
    <iframe id="votingSystemAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
</div>
</body>
</html>
<r:script>
<g:applyCodec encodeAs="none">

    var groupvs = ${groupvsMap as JSON}

    $(function() {

        <g:if test="${GroupVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">
            $(".pageHeader").css("color", "#388746")
        </g:if>
        <g:if test="${GroupVS.State.PENDING.toString().equals(groupvsMap?.state)}">
            $(".pageHeader").css("color", "#fba131")
            $("#messagePanel").addClass("groupvsPendingBox");
            $("#messagePanel").text("<g:message code="groupvsPendingLbl"/>")
            $("#messagePanel").css("display", "visible")

        </g:if>
        <g:if test="${GroupVS.State.CLOSED.toString().equals(groupvsMap?.state)}">
            $(".pageHeader").css("color", "#870000")
            $("#messagePanel").addClass("groupvsClosedBox");
            $("#messagePanel").text("<g:message code="groupvsClosedLbl"/>")
            $("#messagePanel").css("display", "visible")
        </g:if>
    });

    function subscribeToGroup() {
        console.log("sendManifest")
        var webAppMessage = new WebAppMessage(
                ResponseVS.SC_PROCESSING,
                Operation.MANIFEST_SIGN)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink( controller:'GroupVSManifestCollector', absolute:true)}/${groupvsMap.id}"
        webAppMessage.signedMessageSubject = "${groupvsMap.subject}"
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature, application/x-pkcs7-mime'
        webAppMessage.GroupVS = groupvs
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        webAppMessage.documentURL = groupvs.URL
        votingSystemClient.setMessageToSignatureClient(webAppMessage, subscribeToGroupCallback);
    }

    function subscribeToGroupCallback(appMessage) {
        console.log("eventSignatureCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            $("#workingWithAppletDialog" ).dialog("close");
            var caption = '<g:message code="groupSubscriptionERRORLbl"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='groupSubscriptionOKLbl'/>"
            } else if (ResponseVS.SC_CANCELLED == appMessageJSON.statusCode) {
                caption = "<g:message code='groupSubscriptionCANCELLEDLbl'/>"
            }
            var msg = appMessageJSON.message
            showResultDialog(caption, msg)
        }
    }

</g:applyCodec>
</r:script>