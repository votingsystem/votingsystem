<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/userVS/uservs-data"/>
</head>
<body>
    <div class="pageContentDiv" style="max-width: 900px;">
        <uservs-data userVSData="${uservsMap as grails.converters.JSON}" messageToUser="${messageToUser}"></uservs-data>
    </div>
</body>
</html>