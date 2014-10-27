<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/app/uservs-home']"/>">
</head>
<body>
<div class="pageContentDiv" style="max-width: 1300px;">
<uservs-home dataMap="${dataMap}"></uservs-home>
</div>
</body>
</html>
<asset:script>


</asset:script>

