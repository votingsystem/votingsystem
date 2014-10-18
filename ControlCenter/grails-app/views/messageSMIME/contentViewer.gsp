<!DOCTYPE html>
<html>
<head>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>

    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/alert-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/messageSMIME/'+ viewer]"/>">

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
    var viewer = null

    <g:applyCodec encodeAs="none">
        var smimeMessageContent = ${signedContentMap as grails.converters.JSON}
    </g:applyCodec>

    document.addEventListener('polymer-ready', function() {
        console.log("receiptViewer - polymer-ready")
        viewer = document.createElement('${viewer}');
        document.querySelector("#voting_system_page").appendChild(viewer)
        loadContent()
    });

    function showContent(contentStr, timeStampDateStr) {
        var b64_to_utf8 = decodeURIComponent(escape(window.atob(contentStr)))
        smimeMessageContent = JSON.parse(b64_to_utf8)
        timeStampDate = timeStampDateStr
        if(viewer != null) loadContent()
    }

    function loadContent() {
        viewer.smimeMessage = document.querySelector("#smimeMessage").innerHTML;
        viewer.smimeMessageContent = smimeMessageContent
        viewer.timeStampDate = timeStampDate
        smimeMessageContent = null
        timeStampDate = null
    }
</asset:script>
<asset:deferredScripts/>

