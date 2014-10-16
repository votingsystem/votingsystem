<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/timeStampSimulation/timestamp-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initTimeStampProtocolSimulationButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <timestamp-form id="timestampForm"></timestamp-form>
    </div>
</body>
</html>