<%@ page import="grails.converters.JSON; org.votingsystem.model.SubscriptionVS" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="groupUserPageLbl"/></title>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <asset:stylesheet src="vickets.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
</head>
<body style="max-width: 600px; margin:30px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <div id="messageDiv" class="text-center" style="font-size: 1.4em; color:#6c0404; font-weight: bold;"></div>
        <div id="" style="border: 1px solid #6c0404; width: 500px;margin:auto; padding: 15px;">
            <div id="" style="font-size: 1.2em; font-weight: bold;">${subscriptionMap.uservs.NIF}</div>
            <div id="nameDiv" style="font-size: 1.2em;font-weight: bold;">${subscriptionMap.uservs.name}</div>
            <div id="contentDiv" style=""><g:message code="subscriptionRequestDateLbl"/>: <span id="dateCreatedDiv"></span></div>
        </div>
        <button id="activateUserButton" type="button" class="btn btn-warning" onclick="activateUser();"
                style="margin:10px 0px 0px 0px; "><g:message code="activateUserLbl"/> <i class="fa fa-thumbs-o-up"></i>
        </button>
        <button id="deActivateUserButton" type="button" class="btn btn-warning" onclick="showCancelSubscriptionFormDialog(deActivateUser);"
                style="margin:10px 0px 0px 10px; "><g:message code="deActivateUserLbl"/> <i class="fa fa-thumbs-o-down"></i>
        </button>
        <button id="makeDepositButton" type="button" class="btn btn-warning" onclick="makeDeposit();"
                style="margin:10px 0px 0px 10px; "><g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
        </button>
        <div id="receipt" style="display:none;">

        </div>
    </div>
<g:include view="/include/dialog/cancelSubscriptionFormDialog.gsp"/>
</body>
</html>
<asset:script>
    <g:applyCodec encodeAs="none">
        var subscriptionDataJSON = ${subscriptionMap as JSON}
    </g:applyCodec>

    $(function() {
        document.getElementById("dateCreatedDiv").innerHTML = subscriptionDataJSON.dateCreated

        var menuType = getParameterByName('menu')

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

    })

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
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        webAppMessage.callerCallback = 'activateUserCallback'
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function deActivateUser () {
        console.log("deActivateUser")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_DEACTIVATE)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'deActivateUser',absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code="deActivateGroupUserMessageSubject"/>" + " '" + subscriptionDataJSON.groupvs.name + "'"
        webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_DEACTIVATE,
            groupvs:{name:subscriptionDataJSON.groupvs.name, id:subscriptionDataJSON.groupvs.id},
            uservs:{name:subscriptionDataJSON.uservs.name, NIF:subscriptionDataJSON.uservs.NIF},
            reason:document.querySelector("#cancelUserSubscriptionReason").value}
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        webAppMessage.callerCallback = 'deActivateUserCallback'
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function makeDeposit () {
        console.log("makeDeposit")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_DEPOSIT)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink(controller:'transaction', action:'deposit',absolute:true)}/" + subscriptionDataJSON.groupvs.id
        webAppMessage.signedMessageSubject = "<g:message code="makeUserGroupDepositMessageSubject"/>" + " '" + subscriptionDataJSON.groupvs.name + "'"
        webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_DEPOSIT,
                groupvsName:subscriptionDataJSON.groupvs.name , id:subscriptionDataJSON.groupvs.id}
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        webAppMessage.callerCallback = 'makeDepositCallback'
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function activateUserCallback (appMessage) {
        console.log("activateUserCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="activateUserERRORLbl"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='activateUserOKLbl'/>"
                $("#messageDiv").text("<g:message code="userStateActiveLbl"/>")
                $("#activateUserButton").css("display", "none")
                $("#makeDepositButton").css("display", "visible")
                $("#deActivateUserButton").css("display", "visible")
            }
            showMessageVS(appMessageJSON.message, caption)
        }
    }

    function deActivateUserCallback (appMessage) {
        console.log("deActivateUserCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="deActivateUserERRORLbl"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='deActivateUserOKLbl'/>"
            }
            var msg = appMessageJSON.message
            showMessageVS(msg, caption)
        }
    }

    function makeDepositCallback(appMessage) {
        console.log("makeDepositCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="makeDepositERRORLbl"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='makeDepositOKLbl'/>"
            }
            var msg = appMessageJSON.message
            showMessageVS(msg, caption)
        }
    }

</asset:script>
<asset:deferredScripts/>