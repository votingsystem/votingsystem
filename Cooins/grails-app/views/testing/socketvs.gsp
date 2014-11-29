<!DOCTYPE html>
<html>
<head>
    <title>vs-socket</title>
    <vs:webresource dir="polymer" file="polymer.html"/>
    <link rel="import" href="${resource(dir: '/bower_components/vs-socket', file: 'vs-socket.html')}">
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContentDiv">
    <vs-socket id="socketvs" socketservice="${grailsApplication.config.webSocketURL}">
        <input id="messageBox" class="text" value="{locale:'es', operation:'LISTEN_TRANSACTIONS'}" name="text">
        <button onclick="sendMessage()">Send message</button>
    </vs-socket>
</div>
</body>
</html>
<asset:script>
    document.querySelector("#socketvs").addEventListener('on-message', function (e) {
        console.log("message: " + JSON.stringify(e.detail))
    })

    document.addEventListener('polymer-ready', function() {
        document.querySelector("#socketvs").sendMessage("{locale:'es', operation:'LISTEN_TRANSACTIONS'}")
    });

    function sendMessage() {
        document.querySelector("#socketvs").sendMessage(document.querySelector("#messageBox").value)
    }
</asset:script>
<asset:deferredScripts/>