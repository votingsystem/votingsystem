var hostPort


function processOperation(messageJSON) {
    console.log("background.js - processOperation - operation: " + messageJSON.operation)
    switch(messageJSON.operation) {
        case "message-to-host":
            if(!hostPort) connectToHost(messageJSON.content)
            else hostPort.postMessage(messageJSON.content);
            break;
        default:
            console.log("unknown operation: " + messageJSON.operation)
    }
}

function connectToHost(messageJSON) {
    console.log("background.js - connectToHost")
    try {
        hostPort = chrome.runtime.connectNative("org.votingsystem.webextension.native");
        hostPort.onMessage.addListener(messageFromHost);
        hostPort.onDisconnect.addListener(function () {
            console.log("Failed to connect: " + chrome.runtime.lastError.message)
            hostPort = null;
            chrome.runtime.sendMessage({operation: "host-disconnected", message:chrome.runtime.lastError.message});
        });
        if(messageJSON) hostPort.postMessage(messageJSON);
    } catch (e) {
        console.log(e)
    }
}

function messageFromHost(message) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(message.native_message)))
    console.log("background.js - messageFromHost: " + b64_to_utf8)
    var messageJSON = JSON.parse(b64_to_utf8)
    if(messageJSON.message_type === "message_to_webextension") {
        switch (messageJSON.operation) {
            case "url_tab":
                chrome.tabs.create({ url: messageJSON.url });
                break;
            default:
                broadcastNativeMessage(messageJSON)
        }
    } else broadcastNativeMessage(messageJSON)
}

function broadcastNativeMessage(messageJSON) {
    activePorts.forEach(function(activePort, index) {
        try {
            activePort.postMessage(messageJSON);
        } catch (e) {
            console.log(e)
        }
    })
}

chrome.runtime.onSuspend.addListener( function(request, sender, sendResponse) {
        console.log("background.js - onSuspend")
        if(hostPort) hostPort.disconnect()
        activePorts.forEach(function(activePort) {
            try {activePort.disconnect()} catch (e) {}
        })
    });

var activePorts = []
chrome.runtime.onConnect.addListener(function(port) {
    console.log("background.js - port connected: " + port.name)
    activePorts.push(port)
    port.onDisconnect.addListener(function() {
        var index = activePorts.indexOf(port);
        if (index > -1) activePorts.splice(index, 1);
    });
    port.onMessage.addListener(function(messageJSON) {
        processOperation(messageJSON)
        /*
         This function becomes invalid when the event listener returns, unless you return true from the event listener
         to indicate you wish to send a response asynchronously (this will keep the message channel open to the other
         end until sendResponse is called). https://developer.chrome.com/extensions/runtime#event-onMessage
         */
        return true
    });
    return true
});

chrome.runtime.onMessageExternal.addListener(function (request, sender, sendResponse) {
    console.log("background.js - onMessageExternal - request: " + request + " - sender: " + sender)
})