<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/vote-editor.gsp']"/>">
</head>
<body>
    <div id="contentDiv" style="padding: 0px 20px 0px 20px;">
        <vote-editor id="voteEditor"></vote-editor>
    </div>
</body>
</html>
<asset:script>
</asset:script>