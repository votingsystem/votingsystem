<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/balance-details']"/>">
</head>
<body>
    <div style="max-width: 1000px; margin: 0 auto;">
        <balance-details id="balanceDetails" balance="${balanceMap as grails.converters.JSON}" opened="true"></balance-details>
    </div>
</body>
</html>