<%@ page import="org.votingsystem.model.UserVS; org.votingsystem.model.GroupVS" %>
<!DOCTYPE html>
<html>
<head>
    <link href="${resource(dir: 'css', file:'vicket_groupvs.css')}" type="text/css" rel="stylesheet"/>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv" style="max-width: 1000px; padding:0px 30px 0px 30px;">
    <div class="row" style="margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'userVS', action:'search')}"><g:message code="usersvsLbl"/></a></li>
            <li class="active"><g:message code="vicketSourcePageLbl"/></li>
        </ol>
    </div>
    <div class="pageContenDiv" style="max-width: 1000px; padding: 20px;">
        <div style="margin:0px 30px 0px 30px;">
            <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
            </div>

            <h3><div class="pageHeader text-center"> ${uservsMap?.name}</div></h3>

            <div style="margin: 15px 0 0px 0;">
                <div id="userDescriptionDiv" class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
                    ${raw(uservsMap?.description)}
                </div>
            </div>
            <div class="text-right" style="margin: 0px 0 15px 0; font-size: 0.9em;"><b>IBAN: </b>${uservsMap?.IBAN}</div>


            <g:if test="${"user".equals(params.menu)}">
                <g:if test="${UserVS.State.ACTIVE.toString().equals(uservsMap?.state)}">
                    <div class="row text-right">
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
                <table class="table white_headers_table" id="transaction_table" style="">
                    <thead>
                    <tr style="color: #ff0000;">
                        <th style="max-width:80px;"><g:message code="amountLbl"/></th>
                        <th style="max-width:60px;"><g:message code="currencyLbl"/></th>
                        <th style="width:170px;"><g:message code="dateLbl"/></th>
                        <th style="min-width:300px;"><g:message code="subjectLbl"/></th>
                        <th style="min-width:200px;"><g:message code="recipientLbl"/></th>
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
    </div>
</div>
<div id="certificateListDiv" style="display:none;">${uservsMap.certificateList as grails.converters.JSON}</div>
<g:include view="/include/dialog/resultDialog.gsp"/>
<g:include view="/include/dialog/sendMessageVSDialog.gsp"/>
</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">

    $(function() {
        <g:if test="${uservsMap?.description == null}">
            document.getElementById("userDescriptionDiv").style.display = 'none'
        </g:if>
        <g:if test="${UserVS.State.ACTIVE.toString().equals(uservsMap?.state)}">

        </g:if>

    });

    function showMessageVSDialog() {
        console.log(showMessageVSDialog)
        showSendMessageVSDialog("${uservsMap?.nif}",
            "<g:message code="uservsMessageVSLbl" args="${[uservsMap?.name]}"/>", sendMessageVSCallback)
    }

    function sendMessageVSCallback(appMessage) {
        var appMessageJSON = toJSON(appMessage)
        var callBackResult = null
        if(appMessageJSON != null) {
            var caption = '<g:message code="sendMessageERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="sendMessageOKCaption"/>'
            }
            showResultDialog(caption, msg, callBackResult)
        }
        window.scrollTo(0,0);
    }


    function editGroup() {
        window.location.href = updateMenuLink("${createLink( controller:'groupVS', action:'edit', absolute:true)}/${uservsMap.id}")
    }

    function adminGroupUsers(groupId) {
        window.location.href = "${createLink( controller:'groupVS', absolute:true)}/" + groupId + "/users?menu=admin"
    }

    addClientToolListener(function() {
        if(document.getElementById("clientToolMsg") != null)
            document.getElementById("clientToolMsg").style.display = 'none'
    })

</g:applyCodec>
</asset:script>