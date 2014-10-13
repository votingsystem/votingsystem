<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-details']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/innerpage-signal']"/>">
</head>
<body>
    <groupvs-details groupvs="${groupvsMap as grails.converters.JSON}" id="groupvsDetails"></groupvs-details>
</body>
</html>
