<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <title>vs-socket</title>
    <link href="${config.resourceURL}/polymer/polymer.html" rel="import"/>
    <link rel="import" href="${config.resourceURL}/vs-socket/vs-socket.html">
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContentDiv">
    <vs-socket id="socketvs" socketservice="${config.webSocketURL}">
        <input id="messageBox" class="text" value="{locale:'es', operation:'LISTEN_TRANSACTIONS'}" name="text">
        <button onclick="sendMessage()">Send message</button>
    </vs-socket>
</div>
</body>
</html>
<script>
    document.querySelector("#socketvs").addEventListener('on-message', function (e) {
        console.log("message: " + JSON.stringify(e.detail))
    })

    document.addEventListener('polymer-ready', function() {
        document.querySelector("#socketvs").sendMessage("{locale:'es', operation:'LISTEN_TRANSACTIONS'}")
    });

    function sendMessage() {
        document.querySelector("#socketvs").sendMessage(document.querySelector("#messageBox").value)
    }
</script>
