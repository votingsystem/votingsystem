<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/claimProtocolSimulation/claim-simulation-form']"/>">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="initClaimProtocolSimulationButton"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv">
    <claim-simulation-form id="claimSimulationForm"></claim-simulation-form>
</div>
</body>
</html> 
<asset:script>
</asset:script>