<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/eventVSClaim/eventvs-claim.gsp']"/>">
</head>
<body>
<div class="pageContenDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'eventVSClaim')}"><g:message code="claimSystemLbl"/></a></li>
        <li class="active"><g:message code="claimLbl"/></li>
    </ol>

    <eventvs-claim id="claimVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-claim>
</div>

</body>
</html>
<asset:script>
</asset:script>