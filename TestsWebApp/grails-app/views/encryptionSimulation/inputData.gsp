<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/encryptionSimulation/encryption-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initEncryptionProtocolSimulationButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <encryption-form id="encryptionForm"></encryption-form>
    </div>
</body>
</html>