<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/eventVSClaim/eventvs-claim"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="claimLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <eventvs-claim id="claimVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-claim>
</div>
</body>
</html>
<asset:script>
</asset:script>