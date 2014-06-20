<%@ page import="org.votingsystem.model.UserVS; org.votingsystem.model.GroupVS" %>
<%  def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
    def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
    def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
    def groupName = groupvsMap.name.replaceAll("'", "&apos;")
%>
<!DOCTYPE html>
<html>
<head>
    <link href="${resource(dir: 'css', file:'vicket_groupvs.css')}" type="text/css" rel="stylesheet"/>
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <asset:javascript src="jquery.dynatable.js"/>
    <asset:stylesheet src="jquery.dynatable.css"/>
    <asset:stylesheet src="bootstrapValidator.min.css"/>
    <asset:javascript src="bootstrapValidator.min.js"/>
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
<div class="pageContenDiv" style="max-width: 1000px; padding: 0px 30px 0px 30px;">
    <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
    </div>

    <g:if test="${("admin".equals(params.menu) || "superadmin".equals(params.menu)) && UserVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">
        <div id="adminButtonsDiv" class="">
            <button id="editGroupVSButton" type="submit" class="btn btn-warning" onclick="editGroup();"
                    style="margin:10px 20px 0px 0px;">
                <g:message code="editDataLbl"/> <i class="fa fa-edit"></i>
            </button>
            <button id="cancelGroupVSButton" type="submit" class="btn btn-warning"
                    style="margin:10px 20px 0px 0px;" onclick="$('#cancelGroupVSDialog').modal('show')">
                <g:message code="cancelGroupVSLbl"/> <i class="fa fa-times"></i>
            </button>
            <button id="adminGroupVSUsersButton" type="submit" class="btn btn-warning" onclick="adminGroupUsers('${groupvsMap.id}');"
                    style="margin:10px 20px 0px 0px;">
                <g:message code="adminGroupVSUsersLbl"/> <i class="fa fa-users"></i>
            </button>

            <div class="btn-group">
                <button type="button" class="btn btn-warning dropdown-toggle" data-toggle="dropdown" style="margin:10px 20px 0px 0px;">
                    <g:message code="makeDepositFromGroupVSLbl"/>  <i class="fa fa-money"></i>
                    <span class="caret"></span>
                </button>
                <ul class="dropdown-menu">
                    <li onclick="showDepositDialog(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER,'${groupName}', '${groupvsMap?.IBAN}',
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', '${groupvsMap?.id}')"><a href="#">
                        <g:message code="makeDepositFromGroupVSToMemberLbl"/></a></li>
                    <li onclick="showDepositDialog(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP, '${groupName}', '${groupvsMap?.IBAN}',
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', '${groupvsMap?.id}')"><a href="#">
                        <g:message code="makeDepositFromGroupVSToMemberGroupLbl"/></a></li>
                    <li onclick="showDepositDialog(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS, '${groupName}', '${groupvsMap?.IBAN}',
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', '${groupvsMap?.id}')"><a href="#">
                        <g:message code="makeDepositFromGroupVSToAllMembersLbl"/></a></li>
                </ul>
            </div>

        </div>
    </g:if>

    <g:if test="${"user".equals(params.menu)}">
        <g:if test="${UserVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">
            <div class="row">
                <button id="subscribeButton" type="submit" class="btn btn-default" onclick="subscribeToGroup();"
                        style="margin:15px 0px 0px 30px;color:#6c0404;">
                    <g:message code="subscribeGroupVSLbl"/> <i class="fa fa-sign-in"></i>
                </button>

                <button id="subscribeButton" type="submit" class="btn btn-default" onclick="subscribeToGroup();"
                        style="margin:15px 0px 0px 30px;color:#6c0404;">
                    <g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
                </button>
            </div>
        </g:if>
    </g:if>

    <h3><div class="pageHeader text-center"> ${groupvsMap?.name}</div></h3>

    <div style="margin: 15px 0 15px 0;">
        <div class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
            ${raw(groupvsMap?.description)}
        </div>
        <div class="row" style="width:1000px;">
            <div id="" style="margin:0px 30px 0px 10px; font-size: 0.85em; color:#888;" class="col-sm-4 text-left">
                <b><g:message code="IBANLbl"/>: </b>${groupvsMap?.IBAN}
            </div>
            <div id="" style="margin:0px 40px 0px 0px; font-size: 0.85em; float:right; color:#888;" class="col-sm-6 text-right">
                <b><g:message code="groupRepresentativeLbl"/>: </b>${groupvsMap?.representative.firstName} ${groupvsMap?.representative.lastName}
            </div>
        </div>
    </div>

    <div style="border: 1px solid #ccc; padding: 10px; margin: 20px 0 0 0;">
        <div class="text-center" style="font-size: 1.2em;font-weight: bold; color:#6c0404; padding: 0px 0 0 0; ">
            <g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom, weekTo]}"/>
        </div>

        <!-- Nav tabs -->
        <ul class="nav nav-tabs">
            <li class="active" style="">
                <a href="#transactionsTo" data-toggle="tab" style="padding: 5px 30px 5px 30px;"><g:message code="incomeLbl"/></a>
            </li>
            <li style="">
                <a href="#transactionsFrom" data-toggle="tab" style="padding: 5px 30px 5px 30px;"><g:message code="expensesLbl"/></a>
            </li>
        </ul>

        <g:set var="transactionVSService" bean="transactionVSService"/>

        <!-- Tab panes -->
        <div class="tab-content" style="min-height: 600px;">
            <div class="tab-pane fade in active" id="transactionsTo">
                <div id="transaction_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
                    <table class="table white_headers_table" id="transaction_table" style="">
                        <thead>
                        <tr style="color: #ff0000;">
                            <th data-dynatable-column="type" style="width: 290px;"><g:message code="typeLbl"/></th>
                            <th data-dynatable-column="amount" style="max-width:80px;"><g:message code="amountLbl"/></th>
                            <th data-dynatable-column="dateCreated" style="width:180px;"><g:message code="dateLbl"/></th>
                            <th data-dynatable-column="subject" style="min-width:300px;"><g:message code="subjectLbl"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${groupvsMap?.transactionToList}">
                            <g:set var="transactionURL" value="${createLink(uri:'/transaction', absolute:true)}/${it.id}" scope="page" />
                            <% def transactionToDate = formatDate(date:it.dateCreated, formatName:'webViewDateFormat')%>
                            <tr>
                                <td class="text-center">${transactionVSService.getTransactionTypeDescription(it.type, request.locale)}</td>
                                <td class="text-center">${it.amount} ${it.currency}</td>
                                <td class="text-center">${transactionToDate}</td>
                                <td class="text-center">
                                    <a href="#" onclick="openWindow('${transactionURL}')">${it.subject}</a>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="tab-pane fade" id="transactionsFrom" style="top:0px;">
                <div id="transaction_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
                    <table class="table white_headers_table" id="transaction_table" style="">
                        <thead>
                        <tr style="color: #ff0000;">
                            <th data-dynatable-column="type" style="width: 290px;"><g:message code="typeLbl"/></th>
                            <th data-dynatable-column="amount" style="width:150px;"><g:message code="amountLbl"/></th>
                            <th data-dynatable-column="dateCreated" style="width:180px;"><g:message code="dateLbl"/></th>
                            <th data-dynatable-column="subject" style="min-width:300px;"><g:message code="subjectLbl"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${groupvsMap?.transactionFromList}">
                            <g:set var="transactionURL" value="${createLink(uri:'/transaction', absolute:true)}/${it.id}" scope="page" />
                            <% def transactionDate = formatDate(date:it.dateCreated, formatName:'webViewDateFormat')%>
                            <tr>
                                <td class="text-center">${it.type}</td>
                                <td class="text-right">${it.amount} ${it.currency}</td>
                                <td class="text-center">${transactionDate}</td>
                                <td class="text-center">
                                    <a href="#" onclick="openWindow('${transactionURL}')">${it.subject}</a>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

    </div>

    <g:if test="${!"admin".equals(params.menu) && !"user".equals(params.menu)}">
        <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
            <g:message code="clientToolNeededMsg"/>.
            <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
    </g:if>
</div>
<g:include view="/include/dialog/cancelGroupVSDialog.gsp"/>
<g:include view="/include/dialog/depositDialog.gsp"/>
<g:include view="/include/dialog/resultDialog.gsp"/>
</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">

    var groupvsRepresentative = {id:${groupvsMap.representative.id}, nif:"${groupvsMap.representative.nif}"}
    var groupVSData = {id:${groupvsMap.id}, name:escape('${groupvsMap.name.replaceAll("'", "&apos;")}') , representative:groupvsRepresentative}


    $(function() {

        <g:if test="${UserVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">

        </g:if>
        <g:if test="${UserVS.State.PENDING.toString().equals(groupvsMap?.state)}">
            $(".pageHeader").css("color", "#fba131")
            $("#messagePanel").addClass("groupvsPendingBox");
            $("#messagePanel").text("<g:message code="groupvsPendingLbl"/>")
            $("#messagePanel").css("display", "block")

        </g:if>
        <g:if test="${UserVS.State.CANCELLED.toString().equals(groupvsMap?.state)}">
            $(".pageHeader").css("color", "#6c0404")
            $("#messagePanel").addClass("groupvsClosedBox");
            $("#messagePanel").text("<g:message code="groupvsClosedLbl"/>")
            $("#messagePanel").css("display", "block")
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
        webAppMessage.contentType = 'application/x-pkcs7-signature'
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

    function rowWriter(rowIndex, jsonTransactionData, columns, cellWriter) {
        var transactionType
        switch(jsonTransactionData.type) {
            case 'VICKET_SEND':
                transactionType = '<g:message code="selectVicketSendLbl"/>'
                break;
            case 'USER_ALLOCATION':
                transactionType = '<g:message code="selectUserAllocationLbl"/>'
                break;
            case 'USER_ALLOCATION_INPUT':
                transactionType = '<g:message code="selectUserAllocationInputLbl"/>'
                break;
            case 'VICKET_REQUEST':
                transactionType = '<g:message code="selectVicketRequestLbl"/>'
                break;
            case 'VICKET_CANCELLATION':
                transactionType = '<g:message code="selectVicketCancellationLbl"/>'
                break;
            case 'VICKET_SOURCE_INPUT':
                transactionType = '<g:message code="vicketSourceInputLbl"/>'
                break;
            default:
                transactionType = jsonTransactionData.type
        }
        var transactionURL = jsonTransactionData.id

        var cssClass = "span4", tr;
        var amount = jsonTransactionData.amount + " " + jsonTransactionData.currency
        if (rowIndex % 3 === 0) { cssClass += ' first'; }
        tr = '<tr><td title="' + transactionType + '" class="text-center"><a href="#" onclick="openWindow(\'' + transactionURL + '\')">' +
            transactionType + '</a></td><td class="text-center">' + amount + '</td><td class="text-center">' + jsonTransactionData.dateCreated +
            '</td><td title="' + jsonTransactionData.subject + '" class="text-center">' + jsonTransactionData.subject + '</td></tr>'
        return tr
    }

// VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER, VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP, VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS,
    function makeDepositFromGroupVSToMember() {
        alert("makeDepositFromGroupVSToMember")
    }

    function makeDepositFromGroupVSToMemberGroup() {
        alert("makeDepositFromGroupVSToMemberGroup")
    }

    function makeDepositFromGroupVSToAllMembers() {
        alert("makeDepositFromGroupVSToAllMembers")
    }

</g:applyCodec>
</asset:script>