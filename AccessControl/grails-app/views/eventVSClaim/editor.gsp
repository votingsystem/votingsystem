<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSClaim/eventvs-claim-editor.gsp']"/>">
</head>
<body>
<div id="contentDiv" style="padding: 0px 20px 0px 20px; max-width: 1000px; margin:0px auto;">
    <eventvs-claim-editor id="claimEditor"></eventvs-claim-editor>
</div>
</body>
</html>
<asset:script>
</asset:script>