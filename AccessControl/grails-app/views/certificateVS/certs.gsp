<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/certificateVS/cert-list"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="certsPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <cert-list id="certList" url="<g:createLink controller="certificateVS" action="certs"/>"></cert-list>
</div>
</body>
</html>
<asset:script>
</asset:script>