<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-page-tabs']"/>">

<%@ page import="org.votingsystem.model.UserVS; org.votingsystem.model.GroupVS" %>
<%  def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
def groupName = groupvsMap.name.replaceAll("'", "&apos;")
%>
<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-tabs', file: 'paper-tabs.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-menu-button', file: 'paper-menu-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-deposit-dialog.gsp']"/>">
</head>
<body>
<div class="row" style="width: 100%; margin: 0px auto 0px auto;">
    <ol class="breadcrumbVS pull-left">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'groupVS')}"><g:message code="groupvsLbl"/></a></li>
        <li class="active">
            <g:if test="${"admin".equals(params.menu)}"><g:message code="groupvsAdminPageLbl"/></g:if>
            <g:else><g:message code="groupvsPageLbl"/></g:else>
        </li>
    </ol>
</div>
<div class="pageContenDiv" style="max-width: 1000px; padding: 0px 30px 150px 30px;">
    <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
    </div>

    <g:if test="${("admin".equals(params.menu) || "superadmin".equals(params.menu)) && UserVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">
        <div id="adminButtonsDiv" class="" layout horizontal center center-justified>
                <paper-button icon="create" class="button" label="<g:message code="editDataLbl"/>"
                              raisedButton onclick="editGroup()"></paper-button>
                <paper-button icon="delete" class="button" label="<g:message code="cancelGroupVSLbl"/>"
                              raisedButton onclick="showConfirmCancelGroup()"></paper-button>
                <paper-menu-button id="selectDepositPaperButton" valign="bottom" style="width: 0px;padding:0px;">
                    <core-selector id="coreSelector" selected="{{coreSelectorValue}}" valueattr="id" onCoreSelect="optionSelected(event)">
                        <paper-item id="fromGroupToMember" label="<g:message code="makeDepositFromGroupVSToMemberLbl"/>"></paper-item>
                        <paper-item id="fromGroupToMemberGroup" label="<g:message code="makeDepositFromGroupVSToMemberGroupLbl"/>"></paper-item>
                        <paper-item id="fromGroupToAllMember" label="<g:message code="makeDepositFromGroupVSToAllMembersLbl" onclick="document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS, '${groupName}', '${groupvsMap?.IBAN}',
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', '${groupvsMap?.id}')"/>"></paper-item>
                    </core-selector>
                </paper-menu-button>
                <paper-button icon="credit-card" class="button" label="<g:message code="makeDepositFromGroupVSLbl"/>"
                              raisedButton onclick="openDepositOptions()"></paper-button>

            <script>
                function openDepositOptions() {
                    document.querySelector("#selectDepositPaperButton").opened = true
                }
                document.querySelector("#coreSelector").addEventListener('core-select', function(e) {
                    if(e.detail.isSelected) {
                        console.log("coreSelector: " + e.detail.item.id)
                        if('fromGroupToMember' == e.detail.item.id) {
                            document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER,'${groupName}', '${groupvsMap?.IBAN}',
                                    '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', '${groupvsMap?.id}')
                        } else if('fromGroupToMemberGroup' == e.detail.item.id) {
                            document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP, '${groupName}', '${groupvsMap?.IBAN}',
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', '${groupvsMap?.id}')
                        } else if('fromGroupToAllMember' == e.detail.item.id) {
                            document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS, '${groupName}', '${groupvsMap?.IBAN}',
                                    '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', '${groupvsMap?.id}')
                        }
                        document.querySelector("#coreSelector").selected = null
                    }
                })
            </script>
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
    <g:if test="${groupvsMap?.tags != null && !groupvsMap.tags.isEmpty()}">
        <div id="tagsDiv" style="padding:0px 0px 0px 30px;">
            <div style=" display: table-cell; font-size: 1.1em; font-weight: bold; vertical-align: middle;"><g:message code='tagsLbl'/>:</div>
            <div id="selectedTagDiv" class="btn-group btn-group-sm" style="margin:0px 0px 15px 0px; padding: 5px 5px 0px 5px; display: table-cell;">
                <g:each in="${groupvsMap?.tags}">
                    <a class="btn btn-default" href="#" role="button" style="margin:0px 10px 0px 0px;">${it.name}</a>
                </g:each>
            </div>
        </div>
    </g:if>

    <div style="margin: 5px 0 15px 0;">
        <div class="eventContentDiv" style="">
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

    <div class="text-center" style="font-size: 1.2em;font-weight: bold; color:#6c0404; padding: 0px 0 0 0; ">
        <g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom, weekTo]}"/>
    </div>

    <group-page-tabs style="width: 1000px;"></group-page-tabs>

    <g:if test="${!"admin".equals(params.menu) && !"user".equals(params.menu)}">
        <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
            <g:message code="clientToolNeededMsg"/>.
            <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
    </g:if>
</div>

<div layout horizontal center center-justified style="position:absolute; top:80px; width: 100%; max-width: 1200px; margin: 0px auto 0px auto;">
    <div>
        <vicket-deposit-dialog id="depositDialog"></vicket-deposit-dialog>
    </div>
</div>
</body>
</html>
<asset:script>
    <g:applyCodec encodeAs="none">


    var groupvsRepresentative = {id:${groupvsMap.representative.id}, nif:"${groupvsMap.representative.nif}"}
    var groupVSData = {id:${groupvsMap.id}, name:escape('${groupvsMap.name.replaceAll("'", "&apos;")}') , representative:groupvsRepresentative}

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
        webAppMessage.contentType = 'application/x-pkcs7-signature'
        var objectId = Math.random().toString(36).substring(7)
        window[objectId] =  {setClientToolMessage: function(appMessage) {
                console.log("subscribeToGroupCallback - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                if(appMessageJSON != null) {
                    var caption = '<g:message code="groupSubscriptionERRORLbl"/>'
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='groupSubscriptionOKLbl'/>"
                    }
                    var msg = appMessageJSON.message
                    showMessageVS(msg, caption)
                }
            }}
        webAppMessage.callerCallback = objectId
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function showConfirmCancelGroup() {
        showMessageVS("<g:message code="cancelGroupVSDialogMsg" args="${[groupvsMap.name]}"/>",
            "<g:message code="confirmOperationMsg"/>", 'cancel_group', true)
    }

    var appMessageJSON
    var resultCallbackId = 'resultCallbackId'
    document.querySelector("#_votingsystemMessageDialog").addEventListener('message-accepted', function(e) {
        console.log("message-accepted - cancelgroup: " + e.detail)
        if('cancel_group' == e.detail) {
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_CANCEL)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'cancel',absolute:true)}/${groupvsMap.id}"
            webAppMessage.signedMessageSubject = "<g:message code="cancelGroupVSSignedMessageSubject"/>"
            webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_CANCEL, groupvsName:"${groupvsMap.name}", id:${groupvsMap.id}}
            webAppMessage.contentType = 'application/x-pkcs7-signature'
            var objectId = Math.random().toString(36).substring(7)
            window[objectId] = {setClientToolMessage: function(appMessage) {
                    appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="groupCancelERRORLbl"/>'
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='groupCancelOKLbl'/>"
                        }
                        showMessageVS(appMessageJSON.message, caption, resultCallbackId)
                    }
                }}

            webAppMessage.callerCallback = this.objectId
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            appMessageJSON = null
        }
    });

    document.querySelector("#_votingsystemMessageDialog").addEventListener('message-closed', function(e) {
        console.log("message-closed - detail: " + e.detail)
        if(resultCallbackId == e.detail) {
            if(appMessageJSON != null && ResponseVS.SC_OK == appMessageJSON.statusCode) {
                window.location.href = updateMenuLink(appMessageJSON.URL)
            }
        }
    })

    </g:applyCodec>
</asset:script>