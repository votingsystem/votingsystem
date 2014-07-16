<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-details']"/>">
</head>
<body>

<groupvs-details id="groupvsDetails"></groupvs-details>
</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        <g:applyCodec encodeAs="none">
            document.querySelector("#groupvsDetails").groupvs = ${groupvsMap as grails.converters.JSON}
        </g:applyCodec>
    });
</asset:script>