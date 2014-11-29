<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/representative/representative-info"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="representativeLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <representative-info id="representative" representative="${representativeMap as grails.converters.JSON}"></representative-info>
    </div>
</body>
</html>
<asset:script>
</asset:script>