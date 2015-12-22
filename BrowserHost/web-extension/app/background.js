console.log("background.js");

var nativeClientPort

chrome.runtime.onMessage.addListener(
    function(request, sender, sendResponse) {
        processOperation(request)
    });


function processOperation(operationJSON) {
    console.log("background.js - processOperation - operation: " + operationJSON.operation + " - background:" + operationJSON.background)
    if (operationJSON.background === true) {
        switch(operationJSON.operation) {
            case "connect-native":
                if(!nativeClientPort) connectNative()
                else console.log("already connected to native port")
                break;
            case "connect-native-message":
                if(!nativeClientPort) connectNative(operationJSON)
                else nativeClientPort.postMessage(operationJSON.message);
                break;
            case "current-tab-url":
                chrome.tabs.query({currentWindow: true, active: true}, function(tabs){
                    console.log(tabs[0].url);
                });
                break;
            default:
                console.log("unknown operation: " + operationJSON.operation)
        }
    }
}

function connectNative(operationJSON) {
    console.log("connectNative")
    try {
        nativeClientPort = chrome.runtime.connectNative("org.votingsystem.webextension.native");
        nativeClientPort.onMessage.addListener(onNativeMessage);
        nativeClientPort.onDisconnect.addListener(function () {
            console.log("Failed to connect: " + chrome.runtime.lastError.message)
            nativeClientPort = null;
            chrome.runtime.sendMessage({operation: "connect-native-disconnected", message:chrome.runtime.lastError.message});
        });
        if(operationJSON) nativeClientPort.postMessage(operationJSON);
    } catch (e) {
        console.log(e)
    }
}

function onNativeMessage(message) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(message.native_message)))
    console.log("onNativeMessage - b64_to_utf8: " + b64_to_utf8)
    var messageJSON = JSON.parse(b64_to_utf8)
    activePorts.forEach(function(activePort, index) {
        try {
            activePort.postMessage({operation: "connect-native-message", native_message:messageJSON});
        } catch (e) {
            console.log(e)
        }

    })
}

chrome.runtime.onSuspend.addListener( function(request, sender, sendResponse) {
        console.log("onSuspend")
        if(nativeClientPort) nativeClientPort.disconnect()
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
    port.onMessage.addListener(function(operationJSON) {
        console.log("background.js - port.onMessage: " + JSON.stringify(operationJSON))
        processOperation(operationJSON)
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