<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/balance/balance-uservs']"/>">
</head>
<body>
    <vs-innerpage-signal title="<g:message code="balanceLbl"/>"></vs-innerpage-signal>
    <div style="max-width: 1200px; margin: 0 auto;">
        <balance-uservs balance="${balanceMap}"></balance-uservs>
    </div>
</body>
</html>