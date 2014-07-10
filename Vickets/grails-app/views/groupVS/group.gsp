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
    <link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
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
<div class="pageContenDiv" style="max-width: 1000px; padding: 0px 30px 150px 30px;">
    <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
    </div>

    <g:if test="${("admin".equals(params.menu) || "superadmin".equals(params.menu)) && UserVS.State.ACTIVE.toString().equals(groupvsMap?.state)}">
        <div id="adminButtonsDiv" class="">
            <button id="editGroupVSButton" type="submit" class="btn btn-warning" onclick="editGroup();"
                    style="margin:10px 20px 0px 0px;">
                <g:message code="editDataLbl"/> <i class="fa fa-edit"></i>
            </button>
            <button id="cancelGroupVSButton" type="submit" class="btn btn-warning"
                    style="margin:10px 20px 0px 0px;" onclick="document.querySelector('#cancelGroupDialog').show()">
                <g:message code="cancelGroupVSLbl"/> <i class="fa fa-times"></i>
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



    <g:if test="${!"admin".equals(params.menu) && !"user".equals(params.menu)}">
        <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
            <g:message code="clientToolNeededMsg"/>.
            <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
    </g:if>

</div>

<g:include view="/include/dialog/cancel-group-dialog.gsp"/>
<div style="position:absolute; top:0px; width:100%;">
    <div layout horizontal center center-justified style="">
        <cancel-group-dialog id="cancelGroupDialog"></cancel-group-dialog>
    </div>
</div>

<footer>
    <p style="text-align: center;"><small><a  class="appLink" href="mailto:jgzornoza@gmail.com">Contact</a></small></p>
</footer>
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
            showMessageVS(msg, caption)
        }
    }
</g:applyCodec>
</asset:script>