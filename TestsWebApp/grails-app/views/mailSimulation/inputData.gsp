<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/mailSimulation/mail-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initMailProtocolSimulationMsg"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <mail-form id="mailForm"></mail-form>
    </div>
</body>
</html>