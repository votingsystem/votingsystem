<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/bankVS-list']"/>">
</head>
<body>
<div class="pageContentDiv">
    <votingsystem-innerpage-signal title="<g:message code="bankVSListLbl"/>"></votingsystem-innerpage-signal>
    <bankVS-list bankVSMap="${bankVSMap as grails.converters.JSON}"></bankVS-list>
</div>
</body>
</html>