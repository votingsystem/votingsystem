<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/certificateVS/cert-request-form"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="certRequestLbl"/>"></vs-innerpage-signal>
    <div id="contentDiv" class="pageContentDiv" style="min-height: 1000px;">
        <cert-request-form></cert-request-form>
    </div>
</body>
</html>