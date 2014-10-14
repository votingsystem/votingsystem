<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="pollLbl"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <eventvs-election id="electionVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-election>
    </div>
</body>
</html>