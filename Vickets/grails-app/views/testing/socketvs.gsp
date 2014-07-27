<!DOCTYPE html>
<html>
<head>
    <title>votingsystem-socket</title>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-socket', file: 'votingsystem-socket.html')}">
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContenDiv">
    <votingsystem-socket id="socketvs" url="${grailsApplication.config.webSocketURL}">
        <input id="messageBox" class="text" value="{locale:'es', operation:'LISTEN_TRANSACTIONS'}" name="text">
        <button onclick="sendMessage()">Send message</button>
    </votingsystem-socket>
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