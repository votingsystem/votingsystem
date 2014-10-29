<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-data']"/>">
</head>
<body>
    <div class="pageContentDiv" style="max-width: 900px;">
        <uservs-data userVSData="${uservsMap as grails.converters.JSON}"></uservs-data>
    </div>
</body>
</html>