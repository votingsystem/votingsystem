<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/eventVSElection/eventvs-election"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="pollLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <eventvs-election id="electionVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-election>
    </div>
</body>
</html>
<asset:script>
</asset:script>