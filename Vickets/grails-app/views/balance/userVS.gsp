<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/balance-details.gsp']"/>">
</head>
<body>
    <balance-details id="balanceDetails" url="${createLink(controller: 'balance', action: 'userVS', absolute:true)}/${params.userId}" opened="true"></balance-details>
</body>
</html>