<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/representative/representative-info']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="representativeLbl"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <representative-info id="representative" representative="${representativeMap as grails.converters.JSON}"></representative-info>
    </div>
</body>
</html>
<asset:script>
</asset:script>