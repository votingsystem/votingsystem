<%@ page import="org.votingsystem.model.UserVS; org.votingsystem.model.GroupVS" %>
<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/send-message-dialog.gsp']"/>">
</head>
<body>
<div class="pageContenDiv" style="max-width: 1000px; padding:0px 30px 0px 30px;">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'userVS', action:'search')}"><g:message code="usersvsLbl"/></a></li>
        <li class="active"><g:message code="usersvsPageLbl"/></li>
    </ol>
    <div class="pageContenDiv" style="max-width: 1000px; padding: 20px;">
        <div style="margin:0px 30px 0px 30px;">
            <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
            </div>

            <div id="adminButtonsDiv" class="" style="">
                <button id="sendMessageVSButton" type="submit" class="btn btn-warning" onclick="showMessageVSDialog();"
                        style="margin:10px 20px 0px 0px;display:none;">
                    <g:message code="sendMessageVSLbl"/> <i class="fa fa fa-envelope-square"></i>
                </button>
                <button id="blockUserVSButton" type="submit" class="btn btn-warning"
                        style="margin:10px 20px 0px 0px;display:none;" onclick="showBlockUserVSDialog('${uservsMap.name}', '${uservsMap.id}')">
                    <g:message code="blockUserVSLbl"/> <i class="fa fa fa-thumbs-o-down"></i>
                </button>
                <button id="makeDepositButton" type="submit" class="btn btn-warning" onclick="makeDepositDialog('${uservsMap.id}');"
                        style="margin:10px 20px 0px 0px;display:none;">
                    <g:message code="makeDepositLbl"/> <i class="fa fa fa-money"></i>
                </button>
            </div>

            <h3><div class="pageHeader text-center"> ${uservsMap?.name}</div></h3>

            <div style="margin: 15px 0 15px 0;">
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
            <g:if test="${!"admin".equals(params.menu) && !"user".equals(params.menu)}">
                <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
                    <g:message code="clientToolNeededMsg"/>.
                    <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/></div>
            </g:if>
        </div>
    </div>
</div>
<div id="certificateListDiv" style="display:none;">${uservsMap.certificateList as grails.converters.JSON}</div>
<div layout horizontal center center-justified style="top:100px;">
    <send-message-dialog id="sendMessageDialog"></send-message-dialog>
</div>
</body>
</html>
<asset:script>
<g:applyCodec encodeAs="none">
    document.addEventListener('polymer-ready', function() {
        document.querySelector("#sendMessageDialog").addEventListener('message-response', function(e) {
            var appMessageJSON = JSON.parse(e.detail)
            if(appMessageJSON != null) {
                var caption = '<g:message code="sendMessageERRORCaption"/>'
                var msg = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = '<g:message code="sendMessageOKCaption"/>'
                }
                showMessageVS(msg, caption)
            }
            window.scrollTo(0,0);
        });
    });

    <g:if test="${uservsMap?.description == null}">
        document.querySelector("#userDescriptionDiv").style.display = 'none'
    </g:if>
    <g:if test="${UserVS.State.ACTIVE.toString().equals(uservsMap?.state)}">

    </g:if>

    <g:if test="${"admin".equals(params.menu)}">
        enableAdminMenu()
    </g:if>
    <g:elseif test="${"user".equals(params.menu)}">
        enableUserMenu()
    </g:elseif>
    <g:elseif test="${"superadmin".equals(params.menu)}">

    </g:elseif>
    <g:if test="${UserVS.State.PENDING.toString().equals(uservsMap?.state)}">
        document.querySelector("pageHeader").style.color = "#fba131"
        document.querySelector("#messagePanel").className += "groupvsPendingBox";
        document.querySelector("#messagePanel").innerHTML = "<g:message code="groupvsPendingLbl"/>"
        document.querySelector("pageHeader").style.display = 'block'
    </g:if>
    <g:if test="${UserVS.State.CANCELLED.toString().equals(uservsMap?.state)}">
        document.querySelector("pageHeader").style.color = "#6c0404"
        document.querySelector("#messagePanel").className += "groupvsClosedBox";
        document.querySelector("#messagePanel").innerHTML = "<g:message code="groupvsClosedLbl"/>"
        document.querySelector("pageHeader").style.display = 'block'
        document.querySelector("adminButtonsDiv").style.display = 'none'
    </g:if>

    function enableAdminMenu() { }

    function enableUserMenu() {
        document.querySelector("#sendMessageVSButton").style.display = 'inline'
        document.querySelector("#makeDepositButton").style.display = 'inline'
    }

    function showMessageVSDialog() {
        document.querySelector("#sendMessageDialog").show("${uservsMap?.nif}",
            "<g:message code="uservsMessageVSLbl" args="${[uservsMap?.name]}"/>",
            toJSON(document.getElementById("certificateListDiv").innerHTML))
    }

    function editGroup() {
        window.location.href = updateMenuLink("${createLink( controller:'groupVS', action:'edit', absolute:true)}/${uservsMap.id}")
    }

    function adminGroupUsers(groupId) {
        window.location.href = "${createLink( controller:'groupVS', absolute:true)}/" + groupId + "/users?menu=admin"
    }

    addClientToolListener(function() {
        if(document.querySelector("#clientToolMsg") != null) document.querySelector("#clientToolMsg").style.display = 'none'
    })
</g:applyCodec>
</asset:script>