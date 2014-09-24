<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/user-togroup-form']"/>">
</head>
<body>
<div class="pageContentDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <div>
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'simulation', action:'vickets', absolute:true)}">
                <g:message code="vicketsOperationsLbl"/></a></li>
            <li class="active"><g:message code="addUsersToGroupButton"/></li>
        </ol>
    </div>
    <user-togroup-form id="userTogroupForm"></user-togroup-form>
</div>
</body>
</html>
<asset:script>
</asset:script>