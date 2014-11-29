<!DOCTYPE html>
<html>
<head>
    <title>WebView Test</title>
    <vs:webresource dir="polymer" file="polymer.html"/>
    <vs:webresource dir="font-roboto" file="roboto.html"/>
    <vs:webcss dir="font-awesome/css" file="font-awesome.min.css"/>
    <asset:stylesheet src="cooins.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <vs:webcomponent path="/element/alert-dialog"/>
    <vs:webresource dir="paper-button" file="paper-button.html"/>
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