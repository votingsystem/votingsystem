<!DOCTYPE html>
<html>
<head>
    <title><g:message code="signedDocumentLbl"/></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-pie-chart.png')}" type="image/x-icon">
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
<div class="pageContentDiv" id="voting_system_page"></div>
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
        sendSignalVS({title:"<g:message code="signedDocumentLbl"/>"})
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

