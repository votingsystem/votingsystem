<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/userVS/bankVS-list"/>
</head>
<body>
<div class="pageContentDiv">
    <vs-innerpage-signal caption="<g:message code="bankVSListLbl"/>"></vs-innerpage-signal>
    <bankVS-list bankVSMap="${bankVSMap as grails.converters.JSON}"></bankVS-list>
</div>
</body>
</html>