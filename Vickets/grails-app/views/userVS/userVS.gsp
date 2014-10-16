<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-data']"/>">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="usersvsPageLbl"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv" style="max-width: 900px;">
    <uservs-data id="userData" userVSData="${uservsMap as grails.converters.JSON}"></uservs-data>
</div>
</body>
</html>
<asset:script>
</asset:script>
<asset:deferredScripts/>