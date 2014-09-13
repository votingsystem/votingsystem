<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/claimProtocolSimulation/claim-simulation-form']"/>">
</head>
<body>
<div class="pageContentDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <div>
        <ol class="breadcrumbVS">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'simulation', action:'votingSystem', absolute:true)}">
                <g:message code="votingSystemOperationsLbl"/></a></li>
            <li class="active"><g:message code="initClaimProtocolSimulationButton"/></li>
        </ol>
    </div>
    <claim-simulation-form id="claimSimulationForm"></claim-simulation-form>
</div>
</body>
</html> 
<asset:script>
</asset:script>