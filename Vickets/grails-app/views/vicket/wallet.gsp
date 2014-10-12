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
<div class="pageContentDiv">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li class="active"><g:message code="vicketWalletLbl"/></li>
    </ol>

    <vicket-wallet></vicket-wallet>


</div>
</body>

</html>
<asset:script>

</asset:script>
<asset:deferredScripts/>