<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/balance/balance-weekreport']"/>">
</head>
<body>
    <balance-weekreport id="balanceWeekreport" balances="${balancesMap as grails.converters.JSON}"></balance-weekreport>
</body>
</html>