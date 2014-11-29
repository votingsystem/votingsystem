<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/balance/balance-weekreport']"/>">
</head>
<body>
    <balance-weekreport id="balanceWeekreport" balances="${balancesJSON}"></balance-weekreport>
</body>
</html>