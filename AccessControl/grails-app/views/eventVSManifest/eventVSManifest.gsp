<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSManifest/eventvs-manifest.gsp']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="manifestLbl"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <eventvs-manifest id="manifestVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-manifest>
    </div>
</body>
</html>
<asset:script>
</asset:script>