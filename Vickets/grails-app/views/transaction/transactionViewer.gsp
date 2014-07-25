<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:else><meta name="layout" content="main" /></g:else>

    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-transactionvs.gsp']"/>">
</head>
<body>
<vicket-transactionvs transactionvs="${transactionvsMap as grails.converters.JSON}" opened="true"></vicket-transactionvs>
</body>
</html>
