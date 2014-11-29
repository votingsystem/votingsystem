<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/balance/balance-weekreport"/>
</head>
<body>
    <balance-weekreport id="balanceWeekreport" balances="${balancesJSON}"></balance-weekreport>
</body>
</html>