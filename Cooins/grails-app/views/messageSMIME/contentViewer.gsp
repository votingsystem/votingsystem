<!DOCTYPE html>
<html>
<head>
    <title><g:message code="signedDocumentLbl"/></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-credit-card.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <vs:webscript dir='webcomponentsjs' file="webcomponents.min.js"/>
    <vs:webresource dir="polymer" file="polymer.html"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <vs:webcomponent path="/element/alert-dialog"/>
    <vs:webcomponent path="/messageSMIME/${viewer}"/>
    <g:include view="/include/styles.gsp"/>
</head>
<body>
<div id="voting_system_page" layout horizontal center center-justified>
</div>
<div id="smimeMessage" style="display:none;">${smimeMessage}</div>
</body>
</html>
<asset:script>
    var timeStampDate = "${timeStampDate}"
    var viewer = null

    <g:applyCodec encodeAs="none">
        var receiptJSON = ${signedContentMap as grails.converters.JSON}
    </g:applyCodec>

    document.addEventListener('polymer-ready', function() {
        console.log("receiptViewer - polymer-ready")
        viewer = document.createElement('${viewer}');
        sendSignalVS({title:"<g:message code="signedDocumentLbl"/>"})
        document.querySelector("#voting_system_page").appendChild(viewer)
        loadContent()
    });

    function showContent(contentStr, timeStampDateStr) {
        var b64_to_utf8 = decodeURIComponent(escape(window.atob(contentStr)))
        receiptJSON = JSON.parse(b64_to_utf8)
        timeStampDate = timeStampDateStr
        if(viewer != null) loadContent()
    }

    function loadContent() {
        viewer.signedDocument = receiptJSON
        viewer.timeStampDate = timeStampDate
        viewer.smimeMessage = document.querySelector("#smimeMessage").innerHTML;
        receiptJSON = null
        timeStampDate = null
    }
</asset:script>
<asset:deferredScripts/>

