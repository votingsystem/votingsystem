<!DOCTYPE html>
<html>
<head>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/receipt/receipt-votevs.gsp']"/>">

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-credit-card.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="signedDocumentLbl"/></title>
    <g:include view="/include/styles.gsp"/>
</head>
<body>
    <receipt-votevs id="receiptVoteVS"></receipt-votevs>
</body>
</html>
<asset:script>
    function showContent(contentStr) {
        document.querySelector("#receiptVoteVS").receipt =  JSON.parse(contentStr)
    }
</asset:script>
<asset:deferredScripts/>

