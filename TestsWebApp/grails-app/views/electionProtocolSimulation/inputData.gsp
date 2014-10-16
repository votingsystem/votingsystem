<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/electionProtocolSimulation/election-simulation-form']"/>">

</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initElectionProtocolSimulationButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <election-simulation-form id="electionSimulationForm"></election-simulation-form>
    </div>
</body>
</html>