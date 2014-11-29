<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/groupVS/groupvs-details"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="groupLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <groupvs-details groupvs="${groupvsMap as grails.converters.JSON}"></groupvs-details>
    </div>
</body>
</html>
