<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/cooin/cooin-wallet"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="cooinWalletLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <cooin-wallet></cooin-wallet>
    </div>
</body>
</html>