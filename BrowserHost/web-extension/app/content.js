var extensionPort = chrome.runtime.connect();

window.addEventListener("message", function(event) {
  if (event.source != window)  return;

  if (event.data.operation && event.data.message_type === 'message_to_extension') {
    event.data.background = true
    console.log("content.js - message_to_extension: " + JSON.stringify(event.data));
    extensionPort.postMessage(event.data);
  }
}, false)

extensionPort.onMessage.addListener(function onNativeMessage(message) {
  message.message_type = 'message_from_extension'
  window.postMessage(message, "*");
});

console.log("content.js - ready - chrome.runtime.id: " + chrome.runtime.id);
