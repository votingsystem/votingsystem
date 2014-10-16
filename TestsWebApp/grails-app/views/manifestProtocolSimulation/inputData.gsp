<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/manifestProtocolSimulation/manifest-simulation-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initManifestProtocolSimulationButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <manifest-simulation-form id="manifestSimulationForm"></manifest-simulation-form>
    </div>
</body>
</html>