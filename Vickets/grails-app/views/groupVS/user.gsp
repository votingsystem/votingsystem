<%@ page import="grails.converters.JSON; org.votingsystem.model.SubscriptionVS" %>
<!DOCTYPE html>
<html>
<head>
    <title><g:message code="groupUserPageLbl"/></title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<style>

</style>
<body>
    <div style="max-width: 600px; margin:0px auto 0px auto;">
        <div id="messageDiv" class="text-center" style="font-size: 1.4em; color:#6c0404; font-weight: bold;"></div>
        <div id="" style="border: 1px solid #6c0404; width: 500px;margin:auto; padding: 15px;">
            <div id="" style="font-size: 1.2em; font-weight: bold;">${subscriptionMap.uservs.NIF}</div>
            <div id="nameDiv" style="font-size: 1.2em;font-weight: bold;">${subscriptionMap.uservs.name}</div>
            <div id="contentDiv" style=""><g:message code="subscriptionRequestDateLbl"/>: <span id="dateCreatedDiv"></span></div>
        </div>
        <button id="activateUserButton" type="button" class="btn btn-default" onclick="activateUser();"
                style="margin:10px 0px 0px 0px;"><g:message code="activateUserLbl"/> <i class="fa fa-thumbs-o-up"></i>
        </button>
        <button id="deActivateUserButton" type="button" class="btn btn-default" onclick="document.querySelector('#reasonDialog').toggle();"
                style="margin:10px 0px 0px 10px; "><g:message code="deActivateUserLbl"/> <i class="fa fa-thumbs-o-down"></i>
        </button>
        <button id="makeDepositButton" type="button" class="btn btn-default" onclick="makeDeposit();"
                style="margin:10px 0px 0px 10px; "><g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
        </button>
        <div id="receipt" style="display:none;">

        </div>
    </div>
<g:include view="/polymer/dialog/get-reason-dialog.gsp"/>
<get-reason-dialog id="reasonDialog" caption="<g:message code="cancelSubscriptionFormCaption"/>" opened="false"
       messageToUser="<g:message code="cancelSubscriptionFormMsg"/>"></get-reason-dialog>
</body>
</html>
<asset:script id="vicketScript">
    <g:applyCodec encodeAs="none">
        var subscriptionDataJSON = ${subscriptionMap as JSON}
    </g:applyCodec>

    document.addEventListener('polymer-ready', function() {
        document.querySelector("#reasonDialog").addEventListener('on-submit', function (e) {
            console.log("deActivateUser")
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_DEACTIVATE)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'deActivateUser',absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code="deActivateGroupUserMessageSubject"/>" + " '" + subscriptionDataJSON.groupvs.name + "'"
            webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_DEACTIVATE,
                groupvs:{name:subscriptionDataJSON.groupvs.name, id:subscriptionDataJSON.groupvs.id},
                uservs:{name:subscriptionDataJSON.uservs.name, NIF:subscriptionDataJSON.uservs.NIF}, reason:e.detail}
            webAppMessage.contentType = 'application/x-pkcs7-signature'
            var objectId = Math.random().toString(36).substring(7)
            window[objectId] = {setClientToolMessage: function(appMessage) {
                console.log("deActivateUserCallback - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                var caption = '<g:message code="deActivateUserERRORLbl"/>'
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "<g:message code='deActivateUserOKLbl'/>"
                }
                var msg = appMessageJSON.message
                showMessageVS(msg, caption)
                }}
            webAppMessage.callerCallback = objectId
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        })
    });

    document.getElementById("dateCreatedDiv").innerHTML = subscriptionDataJSON.dateCreated

    <g:if test="${SubscriptionVS.State.ACTIVE.toString().equals(subscriptionMap.state)}">
        document.getElementById("messageDiv").innerHTML = "<g:message code="userStateActiveLbl"/>"
        document.getElementById("activateUserButton").style.display = 'none'
        if("admin" != menuType) document.getElementById("deActivateUserButton").style.display = 'none'
    </g:if>
    <g:if test="${SubscriptionVS.State.PENDING.toString().equals(subscriptionMap.state)}">
        document.getElementById("messageDiv").innerHTML = "<g:message code="userStatePendingLbl"/>"
            document.getElementById("makeDepositButton").style.display = 'none'
    </g:if>
    <g:if test="${SubscriptionVS.State.CANCELLED.toString().equals(subscriptionMap.state)}">
        document.getElementById("messageDiv").innerHTML = "<g:message code="userStateCancelledLbl"/>"
        document.getElementById("deActivateUserButton").style.display = 'none'
        document.getElementById("makeDepositButton").style.display = 'none'
    </g:if>

    function activateUser () {
        console.log("activateUser")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_ACTIVATE)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'activateUser',absolute:true)}"

        webAppMessage.signedMessageSubject = "<g:message code="activateGroupUserMessageSubject"/>" + " '" + subscriptionDataJSON.groupvs.name + "'"
        webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_ACTIVATE,
            groupvs:{name:subscriptionDataJSON.groupvs.name, id:subscriptionDataJSON.groupvs.id},
            uservs:{name:subscriptionDataJSON.uservs.name, NIF:subscriptionDataJSON.uservs.NIF}}
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        var objectId = Math.random().toString(36).substring(7)
        webAppMessage.callerCallback = objectId
        window[objectId] = {setClientToolMessage: function(appMessage) {
                console.log("activateUserCallback - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                if(appMessageJSON != null) {
                    var caption = '<g:message code="activateUserERRORLbl"/>'
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='activateUserOKLbl'/>"
                        document.querySelector("#messageDiv").innerHTML = "<g:message code="userStateActiveLbl"/>"
                        document.querySelector("#activateUserButton").style.display = 'none'
                        document.querySelector("#makeDepositButton").style.display = 'block'
                        document.querySelector("#deActivateUserButton").style.display = 'block'
                    }
                    showMessageVS(appMessageJSON.message, caption)
                }}}
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function makeDeposit () {
        console.log("makeDeposit")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_DEPOSIT)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink(controller:'transactionVS', action:'deposit',absolute:true)}/" + subscriptionDataJSON.groupvs.id
        webAppMessage.signedMessageSubject = "<g:message code="makeUserGroupDepositMessageSubject"/>" + " '" + subscriptionDataJSON.groupvs.name + "'"
        webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_DEPOSIT,
                groupvsName:subscriptionDataJSON.groupvs.name , id:subscriptionDataJSON.groupvs.id}
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
            console.log("makeDepositCallback - message: " + appMessage);
            var appMessageJSON = toJSON(appMessage)
            if(appMessageJSON != null) {
                var caption = '<g:message code="makeDepositERRORLbl"/>'
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "<g:message code='makeDepositOKLbl'/>"
                }
                var msg = appMessageJSON.message
                showMessageVS(msg, caption) }}}
        webAppMessage.callerCallback = objectId
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

</asset:script>
<asset:deferredScripts/>