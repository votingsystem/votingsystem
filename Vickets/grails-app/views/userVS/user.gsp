<%@ page import="org.votingsystem.model.UserVS; org.votingsystem.model.GroupVS" %>
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
    <div style="margin:0px 30px 0px 30px;">
        <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
        </div>

        <g:if test="${"admin".equals(params.menu)}">
            <div id="adminButtonsDiv" class="">
                <button id="editGroupVSButton" type="submit" class="btn btn-warning" onclick="editGroup();"
                        style="margin:10px 20px 0px 0px;">
                    <g:message code="editGroupVSLbl"/> <i class="fa fa fa-check"></i>
                </button>
                <button id="cancelGroupVSButton" type="submit" class="btn btn-warning"
                        style="margin:10px 20px 0px 0px;" onclick="showCancelGroupVSDialog('${uservsMap.name}', '${uservsMap.id}')">
                    <g:message code="cancelGroupVSLbl"/> <i class="fa fa fa-check"></i>
                </button>
                <button id="adminGroupVSUsersButton" type="submit" class="btn btn-warning" onclick="adminGroupUsers('${uservsMap.id}');"
                        style="margin:10px 20px 0px 0px;">
                    <g:message code="adminGroupVSUsersLbl"/> <i class="fa fa fa-check"></i>
                </button>
                <button id="makeDepositButton" type="submit" class="btn btn-warning" onclick="makeDeposit();"
                        style="margin:10px 20px 0px 0px;">
                    <g:message code="makeDepositLbl"/> <i class="fa fa fa-check"></i>
                </button>
            </div>
        </g:if>

        <h3><div class="pageHeader text-center"> ${uservsMap?.name}</div></h3>

        <div style="margin: 15px 0 15px 0;">
            <div class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
                ${raw(uservsMap?.description)}
            </div>
        </div>

        <g:if test="${"user".equals(params.menu)}">
            <g:if test="${UserVS.State.ACTIVE.toString().equals(uservsMap?.state)}">
                <div class="row text-right">
                    <button id="subscribeButton" type="submit" class="btn btn-default btn-lg" onclick=""
                            style="margin:15px 20px 15px 0px;">
                        <g:message code="subscribeGroupVSLbl"/> <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </g:if>
        </g:if>
        <g:if test="${!"admin".equals(params.menu) && !"user".equals(params.menu)}">
            <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
                <g:message code="clientToolNeededMsg"/>.
                <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
        </g:if>
    </div>
</div>
<g:include view="/include/dialog/resultDialog.gsp"/>
<g:include view="/include/dialog/cancelGroupVSDialog.gsp"/>
</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">


    $(function() {

        <g:if test="${UserVS.State.ACTIVE.toString().equals(uservsMap?.state)}">

        </g:if>
        <g:if test="${UserVS.State.PENDING.toString().equals(uservsMap?.state)}">
            $(".pageHeader").css("color", "#fba131")
            $("#messagePanel").addClass("groupvsPendingBox");
            $("#messagePanel").text("<g:message code="groupvsPendingLbl"/>")
            $("#messagePanel").css("display", "visible")

        </g:if>
        <g:if test="${UserVS.State.CANCELLED.toString().equals(uservsMap?.state)}">
            $(".pageHeader").css("color", "#6c0404")
            $("#messagePanel").addClass("groupvsClosedBox");
            $("#messagePanel").text("<g:message code="groupvsClosedLbl"/>")
            $("#messagePanel").css("display", "visible")
            $("#adminButtonsDiv").css("display", "none")
        </g:if>
    });

    function editGroup() {
        window.location.href = "${createLink( controller:'groupVS', action:'edit', absolute:true)}/${uservsMap.id}?menu=admin"
    }

    function adminGroupUsers(groupId) {
        window.location.href = "${createLink( controller:'groupVS', absolute:true)}/" + groupId + "/users?menu=admin"
    }


</g:applyCodec>
</asset:script>