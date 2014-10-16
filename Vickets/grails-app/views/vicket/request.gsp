<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/vicket-request-form']"/>">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="doVicketRequestLbl"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv">
    <vicket-request-form></vicket-request-form>
</div>
</body>
</html>