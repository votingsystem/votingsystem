<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/transactionvs-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="makeTransactionVSButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <transactionvs-form></transactionvs-form>
    </div>
</body>
</html>