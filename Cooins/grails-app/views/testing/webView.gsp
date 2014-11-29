<!DOCTYPE html>
<html>
<head>
    <title>WebView Test</title>
    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <asset:stylesheet src="cooins.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/alert-dialog.gsp']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
</head>
<body id="voting_system_page">
WebView Test
</body>
</html>
<asset:script>

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

</asset:script>
<asset:deferredScripts/>