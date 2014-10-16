<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/transactionvs-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="makeTransactionVSButton"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <transactionvs-form></transactionvs-form>
    </div>
</body>
</html>