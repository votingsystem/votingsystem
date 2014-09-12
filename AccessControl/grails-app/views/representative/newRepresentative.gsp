<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/representative/representative-editor']"/>">
</head>
<body>
<div layout vertical class="pageContentDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <ol class="breadcrumbVS pull-left">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'representative')}"><g:message code="representativesPageLbl"/></a></li>
        <li class="active"><g:message code="newRepresentativeLbl"/></li>
    </ol>

    <representative-editor id="representativeEditor"></representative-editor>

</div>
</body>
</html>
<asset:script>
</asset:script>