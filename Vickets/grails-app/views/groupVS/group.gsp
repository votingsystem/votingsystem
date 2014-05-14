<%@ page import="org.votingsystem.model.GroupVS" %>
<!DOCTYPE html>
<html>
<head>
    <link href="${resource(dir: 'css', file:'vicket_groupvs.css')}" type="text/css" rel="stylesheet"/>
    <meta name="layout" content="main" />
</head>
<body>
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'groupVS')}"><g:message code="groupvsLbl"/></a></li>
            <li class="active">
                <g:if test="${"admin".equals(params.menu)}"><g:message code="groupvsAdminPageLbl"/></g:if>
                <g:else><g:message code="groupvsPageLbl"/></g:else>
            </li>
        </ol>
    </div>
<div class="pageContenDiv" style="max-width: 1000px; padding: 20px;">
    <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
    </div>

    <g:if test="${"admin".equals(params.menu)}">
        <div class="">
            <button id="editGroupVSButton" type="submit" class="btn btn-warning" onclick="subscribeToGroup();"
                    style="margin:15px 20px 15px 0px;">
                <g:message code="editGroupVSLbl"/> <i class="fa fa fa-check"></i>
            </button>
            <button id="cancelGroupVSButton" type="submit" class="btn btn-warning" onclick="subscribeToGroup();"
                    style="margin:15px 20px 15px 0px;">
                <g:message code="cancelGroupVSLbl"/> <i class="fa fa fa-check"></i>
            </button>
            <button id="adminGroupVSUsersButton" type="submit" class="btn btn-warning" onclick="subscribeToGroup();"
                    style="margin:15px 20px 15px 0px;">
                <g:message code="adminGroupVSUsersLbl"/> <i class="fa fa fa-check"></i>
            </button>
            <button id="makeDepositButton" type="submit" class="btn btn-warning" onclick="subscribeToGroup();"
                    style="margin:15px 20px 15px 0px;">
                <g:message code="makeDepositLbl"/> <i class="fa fa fa-check"></i>
            </button>
        </div>
    </g:if>

    <h3><div class="pageHeader text-center"> ${groupvsMap?.name}</div></h3>

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

    <div id="clientToolMsg" class="text-center" style="color:#870000; font-size: 1.2em;"><g:message code="clientToolNeededMsg"/>.
        <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>

</div>
<g:include view="/include/dialog/resultDialog.gsp"/>
</body>
</html>
<r:script>
<g:applyCodec encodeAs="none">

    var groupvsRepresentative = {id:${groupvsMap.representative.id}, nif:"${groupvsMap.representative.nif}"}
    var groupVSData = {id:${groupvsMap.id}, name:"${groupvsMap.name}" , representative:groupvsRepresentative}


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
    if(isClientToolLoaded()) $("#clientToolMsg").css("display", "none")

});

function subscribeToGroup() {
console.log("sendManifest")
var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_SUBSCRIBE)
webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink( controller:'groupVS', absolute:true)}/${groupvsMap.id}/subscribe"
        webAppMessage.signedMessageSubject = "<g:message code="subscribeToVicketGroupMsg"/>"
        webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_SUBSCRIBE, groupvs:groupVSData}
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature, application/x-pkcs7-mime'
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        votingSystemClient.setMessageToSignatureClient(webAppMessage, subscribeToGroupCallback);
    }

    function subscribeToGroupCallback(appMessage) {
        console.log("eventSignatureCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
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