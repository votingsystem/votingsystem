<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSManifest/eventvs-manifest.gsp']"/>">
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="manifestLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <eventvs-manifest id="manifestVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-manifest>
    </div>
</body>
</html>
<asset:script>
</asset:script>