<!DOCTYPE html>
<html>
<head>
    <title>WebView Test</title>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <asset:stylesheet src="vickets.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/alert-dialog.gsp']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/vs-button', file: 'vs-button.html')}">
</head>
<body id="voting_system_page">
WebView Test
</body>
</html>
<asset:script>

    function webViewLoaded() {
        console.log("webViewLoaded")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, 'init')
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function serverMessage(message) {
        var webAppMessage = new WebAppMessage(ResponseVS.OK, 'message: ' + message)
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    document.addEventListener('polymer-ready', function() {
        webViewLoaded()
    })

</asset:script>
<asset:deferredScripts/>