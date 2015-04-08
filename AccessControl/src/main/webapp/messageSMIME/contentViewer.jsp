<!DOCTYPE html>
<html>
<head>
    <title>${msg.signedDocumentLbl}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${contextURL}/resources/images/icon_16/fa-pie-chart.png" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="${resourceURL}/webcomponentsjs/webcomponents.min.js" type="text/javascript"></script>
    <link href="${resourceURL}/polymer/polymer.html" rel="import"/>
    <script src="${contextURL}/resources/js/utilsVS.js" type="text/javascript"></script>
    <script src="${elementURL}/resources/js/utils_js.jsp" type="text/javascript"></script>
    <link href="${elementURL}/element/alert-dialog.vsp" rel="import"/>
    <link href="${elementURL}/messageSMIME/${viewer}" rel="import"/>
    <link href="${contextURL}/resources/css/votingSystem.css" media="all" rel="stylesheet" />
    <link href="${resourceURL}/font-awesome/css/font-awesome.min.css" media="all" rel="stylesheet" />
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


