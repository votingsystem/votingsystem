<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/encryptionSimulation/encryption-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initEncryptionProtocolSimulationButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <encryption-form id="encryptionForm"></encryption-form>
    </div>
</body>
</html>