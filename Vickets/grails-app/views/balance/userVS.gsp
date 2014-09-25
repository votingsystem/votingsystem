<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/balance/balance-uservs']"/>">
</head>
<body>
    <div style="max-width: 1000px; margin: 0 auto;">
        <balance-uservs id="balanceDetails" balance="${balanceMap as grails.converters.JSON}" opened="true"></balance-uservs>
    </div>
</body>
</html>