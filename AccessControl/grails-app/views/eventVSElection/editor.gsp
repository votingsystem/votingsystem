<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election-editor.gsp']"/>">
</head>
<body>
    <div id="contentDiv" style="padding: 0px 20px 0px 20px; max-width: 1000px; margin:0px auto;">
        <eventvs-vote-editor id="voteEditor"></eventvs-vote-editor>
    </div>
</body>
</html>
<asset:script>
</asset:script>