<!DOCTYPE html>
<html>
<head>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>

    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/votingsystem-message-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/receipt/'+ viewer]"/>">

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-credit-card.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="signedDocumentLbl"/></title>
    <g:include view="/include/styles.gsp"/>

</head>
<body>
<div class="pageContentDiv" id="voting_system_page">

</div>
<div id="smimeMessage" style="display:none;">${smimeMessage}</div>
</body>
</html>
<asset:script>
    var timeStampDate = "${timeStampDate}"

    <g:applyCodec encodeAs="none">var receiptJSON = ${signedContentMap as grails.converters.JSON}</g:applyCodec>

    document.addEventListener('polymer-ready', function() {
        console.log("receiptViewer - polymer-ready")
        var viewer = document.createElement('${viewer}');
        viewer.receipt = receiptJSON
        viewer.timeStampDate = timeStampDate
        viewer.smimeMessage = document.querySelector("#smimeMessage").innerHTML;
        document.querySelector("#voting_system_page").appendChild(viewer)
    });

    function showContent(contentStr, timeStampDateStr) {
        receiptJSON = JSON.parse(contentStr)
        timeStampDate = timeStampDateStr
        document.querySelector("#receiptViewer").receipt = receiptJSON
        document.querySelector("#receiptViewer").timeStampDate = timeStampDate
        receiptJSON = null
        timeStampDate = null
    }
</asset:script>
<asset:deferredScripts/>

