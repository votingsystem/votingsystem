<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <title>WebView Test</title>
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${config.resourceURL}/font-roboto/roboto.html" rel="import"/>
    <link href="${config.resourceURL}/font-awesome/css/font-awesome.min.css" media="all" rel="stylesheet" />
    <link href="${config.webURL}/css/cryptocurrency.css" media="all" rel="stylesheet" />
    <script src="${config.webURL}/js/utilsVS.js" type="text/javascript"></script>
    <jsp:include page="/include/utils_js.jsp"/>
    <link href="${config.webURL}/element/alert-dialog.vsp" rel="import"/>
    <link href="${config.resourceURL}/paper-button/paper-button.html" rel="import"/>
</head>
<body id="voting_system_page">
WebView Test
</body>
</html>
<script>
    function webViewLoaded() {
        console.log("webViewLoaded")
        var webAppMessage = new WebAppMessage( 'init')
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function serverMessage(message) {
        var webAppMessage = new WebAppMessage('message: ' + message, ResponseVS.OK)
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    document.addEventListener('polymer-ready', function() {
        webViewLoaded()
    })

</script>
