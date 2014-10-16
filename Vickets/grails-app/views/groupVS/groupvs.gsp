<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-details']"/>">
</head>
<body>
    <div class="pageContentDiv">
        <groupvs-details groupvs="${groupvsMap as grails.converters.JSON}" id="groupvsDetails"></groupvs-details>
    </div>
</body>
</html>
