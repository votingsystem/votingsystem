<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-details']"/>">
</head>
<body>
    <vs-innerpage-signal title="<g:message code="groupLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <groupvs-details groupvs="${groupvsMap}"></groupvs-details>
    </div>
</body>
</html>
