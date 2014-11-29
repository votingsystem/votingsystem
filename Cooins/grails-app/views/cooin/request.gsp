<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/cooin/cooin-request-form']"/>">
</head>
<body>
<vs-innerpage-signal caption="<g:message code="doCooinRequestLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <cooin-request-form></cooin-request-form>
</div>
</body>
</html>