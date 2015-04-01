<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <title>VotingSystem WebSocket Test</title>
    <style type="text/css" media="screen">
        #console {
            border: 1px solid #CCCCCC;
            border-right-color: #999999;
            border-bottom-color: #999999;
            height: 170px;
            overflow-y: scroll;
            padding: 5px;
            width: 100%;
        }
    </style>
</head>
<body>
<div class="pageContentDiv">
    <div style="max-width: 1300px; margin: 20px auto 0px auto;">
        <div id="connect-container">
            <div>
                <div>
                    <span>Connect to:</span>
                </div>
                <div>
                    <input id="target" type="text" size="40" style="width: 350px" value="ws://currency/CurrencyServer/websocket/service"/>
                </div>
                <div>
                    <button id="connect" onclick="connect();">Connect</button>
                    <button id="disconnect" disabled="disabled" onclick="disconnect();">Disconnect</button>
                </div>
            </div>
            <div class="">
                    <textarea id="message" style="width: 350px;">{locale:'es', operation:'LISTEN_TRANSACTIONS'}</textarea>
                    <button id="echo" onclick="sendMessage();" disabled="disabled">Send message</button>
            </div>
        </div>
        <div id="console-container">
            <div id="console" style="width:100%; height:700px;"/>
        </div>
    </div>
</div>
<div class="noscript">
    <h2 style="color: #ff0000">Seems your browser doesn't support Javascript! Websockets rely on Javascript being enabled. Please enable
    Javascript and reload this page!</h2>
</div>

</body>
</html>
<script>
    var ws = null;

function setConnected(connected) {
    document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
    document.getElementById('echo').disabled = !connected;
}

function connect() {
    var target = document.getElementById('target').value;
    if (target == '') {
        alert('Please select enter service URL');
        return;
    }
    if ('WebSocket' in window) {
        ws = new WebSocket(target);
    } else if ('MozWebSocket' in window) {
        ws = new MozWebSocket(target);
    } else {
        alert('WebSocket is not supported by this browser.');
        return;
    }
    ws.onopen = function () {
        setConnected(true);
        log('Info: WebSocket connection opened.');
    };
    ws.onmessage = function (event) {
        log('Received: ' + event.data);
    };
    ws.onclose = function (event) {
        setConnected(false);
        log('Info: WebSocket connection closed, Code: ' + event.code + (event.reason == "" ? "" : ", Reason: " + event.reason));
    };
}

function disconnect() {
    if (ws != null) {
        ws.close();
        ws = null;
    }
    setConnected(false);
}

function sendMessage() {
    if (ws != null) {
        var message = document.getElementById('message').value;
        log('Sent: ' + message);
        ws.send(message);
    } else {
        alert('WebSocket connection not established, please connect.');
    }
}

function updateTarget(target) {
    if (window.location.protocol == 'http:') {
        document.getElementById('target').value = 'ws://' + window.location.host + target;
    } else {
        document.getElementById('target').value = 'wss://' + window.location.host + target;
    }
}

function log(message) {
    var console = document.getElementById('console');
    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.appendChild(document.createTextNode(message));
    console.appendChild(p);
    while (console.childNodes.length > 25) {
        console.removeChild(console.firstChild);
    }
    console.scrollTop = console.scrollHeight;
}


document.addEventListener("DOMContentLoaded", function() {
    // Remove elements with "noscript" class - <noscript> is not allowed in XHTML
    var noscripts = document.getElementsByClassName("noscript");
    for (var i = 0; i < noscripts.length; i++) {
        noscripts[i].parentNode.removeChild(noscripts[i]);
    }
}, false);

</script>
