<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/multiSignSimulation/multisign-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initMultiSignProtocolSimulationButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <multisign-form id="multisignForm"></multisign-form>
    </div>
</body>
</html>