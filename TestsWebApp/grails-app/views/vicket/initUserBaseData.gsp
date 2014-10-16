<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/user-base-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="initUserBaseDataButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <user-base-form id="userBaseForm"></user-base-form>
    </div>
</body>
</html>