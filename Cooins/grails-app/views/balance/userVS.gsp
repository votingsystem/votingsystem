<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/balance/balance-uservs"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="balanceLbl"/>"></vs-innerpage-signal>
    <div style="max-width: 1200px; margin: 0 auto;">
        <balance-uservs balance="${balanceMap}"></balance-uservs>
    </div>
</body>
</html>