<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/cooin/cooin-wallet']"/>">

    <script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite.js'></script>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="cooinWalletLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <cooin-wallet></cooin-wallet>
    </div>
</body>
</html>