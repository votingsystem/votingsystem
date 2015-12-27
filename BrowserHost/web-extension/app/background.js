var hostPort

function processOperation(messageJSON) {
    console.table("background.js - processOperation")
    switch(messageJSON.operation) {
        case "message-to-host":
            console.table("background.js - message-to-host - operation: " + messageJSON.content.operation)
            messageJSON.content.tabId = messageJSON.tabId
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
            console.log("- Failed to connect: " + chrome.runtime.lastError.message)
            hostPort = null;
            chrome.tabs.query({active: true, currentWindow: true}, function(tabs){
                chrome.tabs.sendMessage(tabs[0].id, {operation: "host-disconnected", message:chrome.runtime.lastError.message},
                    function(response) {});
            });
        });
        if(messageJSON) hostPort.postMessage(messageJSON);
    } catch (e) {
        console.log(e)
    }
}

function messageFromHost(msg) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(msg.native_message)))
    console.log("background.js - messageFromHost: " + b64_to_utf8)
    var messageJSON = JSON.parse(b64_to_utf8)
    if(messageJSON.message_type === "message_to_webextension") {
        switch (messageJSON.operation) {
            case "url_tab":
                chrome.tabs.create({ url: messageJSON.url });
                return;
        }
    }
    chrome.tabs.sendMessage(parseInt(messageJSON.tabId), messageJSON, function(response) {});
}

chrome.runtime.onSuspend.addListener( function(request, sender, sendResponse) {
        console.log("background.js - onSuspend")
        if(hostPort) hostPort.disconnect()
    });

chrome.runtime.onMessageExternal.addListener(function (request, sender, sendResponse) {
    console.log("background.js - onMessageExternal - request: " + request + " - sender: " + sender)
})

chrome.extension.onMessage.addListener(function(request, sender, sendResponse) {
    request.tabId = sender.tab.id
    processOperation(request)
});

chrome.runtime.onInstalled.addListener(function() {
    console.log("background.js - onInstalled");
});