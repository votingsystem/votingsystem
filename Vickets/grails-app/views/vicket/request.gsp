<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/vicket-request-form']"/>">
</head>
<body>
<vs-innerpage-signal caption="<g:message code="doVicketRequestLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <vicket-request-form></vicket-request-form>
</div>
</body>
</html>