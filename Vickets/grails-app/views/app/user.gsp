<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/app/uservs-dashboard']"/>">
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="dashBoardLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width: 1300px;">
    <uservs-dashboard dataMap="${dataMap}"></uservs-dashboard>
</div>
</body>
</html>
