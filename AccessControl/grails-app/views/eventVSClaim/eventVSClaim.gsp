<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSClaim/eventvs-claim.gsp']"/>">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="claimLbl"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv">
    <eventvs-claim id="claimVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-claim>
</div>
</body>
</html>
<asset:script>
</asset:script>