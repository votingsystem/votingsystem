<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/app/uservs-dashboard"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="dashBoardLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width: 1300px;">
    <uservs-dashboard dataMap="${dataMap}"></uservs-dashboard>
</div>
</body>
</html>
