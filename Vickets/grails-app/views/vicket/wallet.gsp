<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/paper-slider', file: 'paper-slider.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/vicket-wallet']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="vicketWalletLbl"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <vicket-wallet></vicket-wallet>
    </div>
</body>
</html>