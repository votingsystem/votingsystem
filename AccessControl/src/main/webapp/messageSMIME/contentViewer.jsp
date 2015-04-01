<!DOCTYPE html>
<html>
<head>
    <title>${msg.signedDocumentLbl}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${config.webURL}/images/icon_16/fa-pie-chart.png" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="${config.resourceURL}/webcomponentsjs/webcomponents.min.js" type="text/javascript"></script>
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <script src="${config.webURL}/js/utilsVS.js" type="text/javascript"></script>
    <jsp:include page="/include/utils_js.jsp"/>
    <link href="${config.webURL}/element/alert-dialog.vsp" rel="import"/>
    <link href="${config.webURL}/messageSMIME/${viewer}" rel="import"/>
    <link href="${config.contextURL}/css/votingSystem.css" media="all" rel="stylesheet" />
    <link href="${config.resourceURL}/font-awesome/css/font-awesome.min.css" media="all" rel="stylesheet" />
</head>
<body>
<div id="voting_system_page" layout horizontal center center-justified>
</div>
<div id="smimeMessage" style="display:none;">${smimeMessage}</div>
</body>
</html>
<script>
    var timeStampDate = "${timeStampDate}"
    var viewer = null
    var smimeMessageContent = toJSON('${signedContentMap}')

    document.addEventListener('polymer-ready', function() {
        console.log("receiptViewer - polymer-ready")
        viewer = document.createElement('${viewer}');
        sendSignalVS({title:"${msg.signedDocumentLbl}"})
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
</script>


