<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/user-togroup-form']"/>">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="addUsersToGroupButton"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv">
    <user-togroup-form id="userTogroupForm"></user-togroup-form>
</div>
</body>
</html>
<asset:script>
</asset:script>