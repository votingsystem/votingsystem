<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/vicket-wallet']"/>">
</head>
<body>
    <vs-innerpage-signal title="<g:message code="vicketWalletLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <vicket-wallet></vicket-wallet>
    </div>
</body>
</html>