<%@ page import="org.votingsystem.model.UserVS; org.votingsystem.model.GroupVS" %>
<!DOCTYPE html>
<html>
<head>
    <link href="${resource(dir: 'css', file:'vicket_groupvs.css')}" type="text/css" rel="stylesheet"/>
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <asset:javascript src="jquery.dynatable.js"/>
    <asset:stylesheet src="jquery.dynatable.css"/>
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
<div class="pageContenDiv" style="max-width: 1000px; padding: 20px 30px 0px 30px;">
    <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
    </div>

    <g:if test="${"admin".equals(params.menu) || "superadmin".equals(params.menu)}">
        <div id="adminButtonsDiv" class="">
            <button id="editGroupVSButton" type="submit" class="btn btn-warning" onclick="editGroup();"
                    style="margin:10px 20px 0px 0px;">
                <g:message code="editGroupVSLbl"/> <i class="fa fa fa-check"></i>
            </button>
            <button id="cancelGroupVSButton" type="submit" class="btn btn-warning"
                    style="margin:10px 20px 0px 0px;" onclick="showCancelGroupVSDialog('${groupvsMap.name}', '${groupvsMap.id}')">
                <g:message code="cancelGroupVSLbl"/> <i class="fa fa fa-check"></i>
            </button>
            <button id="adminGroupVSUsersButton" type="submit" class="btn btn-warning" onclick="adminGroupUsers('${groupvsMap.id}');"
                    style="margin:10px 20px 0px 0px;">
                <g:message code="adminGroupVSUsersLbl"/> <i class="fa fa fa-check"></i>
            </button>
            <button id="makeDepositButton" type="submit" class="btn btn-warning" onclick="makeDeposit();"
                    style="margin:10px 20px 0px 0px;">
                <g:message code="makeDepositLbl"/> <i class="fa fa fa-check"></i>
            </button>
        </div>
    </g:if>

    <h3><div class="pageHeader text-center"> ${groupvsMap?.name}</div></h3>

    <div style="margin: 15px 0 15px 0;">
        <div class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
            ${raw(groupvsMap?.description)}
        </div>
        <div class="row" style="width:1200px;">
            <div id="" style="margin:0px 30px 0px 0px;" class="col-sm-3 text-left">
                <b><g:message code="IBANLbl"/>: </b>${groupvsMap?.IBAN}
            </div>
            <div id="" style="margin:0px 30px 0px 0px;" class="col-sm-5 text-right">
                <b><g:message code="groupRepresentativeLbl"/>: </b>${groupvsMap?.representative.firstName} ${groupvsMap?.representative.lastName}
            </div>
        </div>
    </div>

    <g:if test="${"user".equals(params.menu)}">
        <g:if test="${UserVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">
            <div class="row text-right">
                <button id="subscribeButton" type="submit" class="btn btn-default" onclick="subscribeToGroup();"
                        style="margin:15px 20px 15px 0px;">
                    <g:message code="subscribeGroupVSLbl"/> <i class="fa fa fa-check"></i>
                </button>
            </div>
        </g:if>
    </g:if>

    <%  def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
    def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
    def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
    %>

    <div class="text-center" style="font-size: 1.2em;font-weight: bold;">
        <g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom, weekTo]}"/>
    </div>

    <div id="transaction_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
        <table class="table dynatable-vickets" id="transaction_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="amount" style="max-width:80px;"><g:message code="amountLbl"/></th>
                <th data-dynatable-column="currency" style="max-width:60px;"><g:message code="currencyLbl"/></th>
                <th data-dynatable-column="dateCreated" style="width:170px;"><g:message code="dateLbl"/></th>
                <th data-dynatable-column="subject" style="min-width:300px;"><g:message code="subjectLbl"/></th>
                <th data-dynatable-column="receiver" style="min-width:200px;"><g:message code="recipientLbl"/></th>
                <!--<th data-dynatable-no-sort="true"><g:message code="voucherLbl"/></th>-->
            </tr>
            </thead>
            <tbody>
            <g:each in="${uservsMap?.transactionList}">
                <g:set var="transactionURL" value="${createLink(uri:'/transaction', absolute:true)}/${it.id}" scope="page" />
                <% def transactionDate = formatDate(date:it.dateCreated, formatName:'webViewDateFormat')%>
                <tr>
                    <td class="text-center">${it.amount}</td>
                    <td class="text-center">${it.currency}</td>
                    <td class="text-center">${transactionDate}</td>
                    <td class="text-center">
                        <a href="#" onclick="openWindow('${transactionURL}')">${it.subject}</a>
                    </td>
                    <td class="text-center">${it.toUserVS?.name}</td>
                </tr>
            </g:each>

            </tbody>

        </table>
    </div>

    <g:if test="${!"admin".equals(params.menu) && !"user".equals(params.menu)}">
        <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
            <g:message code="clientToolNeededMsg"/>.
            <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
    </g:if>
</div>
<g:include view="/include/dialog/resultDialog.gsp"/>
<g:include view="/include/dialog/cancelGroupVSDialog.gsp"/>
</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">

    var groupvsRepresentative = {id:${groupvsMap.representative.id}, nif:"${groupvsMap.representative.nif}"}
    var groupVSData = {id:${groupvsMap.id}, name:"${groupvsMap.name}" , representative:groupvsRepresentative}


    $(function() {

        <g:if test="${UserVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">

        </g:if>
        <g:if test="${UserVS.State.PENDING.toString().equals(groupvsMap?.state)}">
            $(".pageHeader").css("color", "#fba131")
            $("#messagePanel").addClass("groupvsPendingBox");
            $("#messagePanel").text("<g:message code="groupvsPendingLbl"/>")
            $("#messagePanel").css("display", "visible")

        </g:if>
        <g:if test="${UserVS.State.CANCELLED.toString().equals(groupvsMap?.state)}">
            $(".pageHeader").css("color", "#6c0404")
            $("#messagePanel").addClass("groupvsClosedBox");
            $("#messagePanel").text("<g:message code="groupvsClosedLbl"/>")
            $("#messagePanel").css("display", "visible")
            $("#adminButtonsDiv").css("display", "none")
        </g:if>
    });

    function editGroup() {
        window.location.href = "${createLink( controller:'groupVS', action:'edit', absolute:true)}/${groupvsMap.id}?menu=admin"
    }

    function adminGroupUsers(groupId) {
        window.location.href = "${createLink( controller:'groupVS', absolute:true)}/" + groupId + "/users?menu=admin"
    }

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
        webAppMessage.callerCallback = 'subscribeToGroupCallback'
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function subscribeToGroupCallback(appMessage) {
        console.log("eventSignatureCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="groupSubscriptionERRORLbl"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='groupSubscriptionOKLbl'/>"
            }
            var msg = appMessageJSON.message
            showResultDialog(caption, msg)
        }
    }

</g:applyCodec>
</asset:script>