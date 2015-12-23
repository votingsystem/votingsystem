var extensionPort = chrome.runtime.connect();

document.querySelector("#voting_system_page").addEventListener('message-to-host',
    function(event) {
        extensionPort.postMessage({operation:'message-to-host', content:event.detail});
    })

extensionPort.onMessage.addListener(function (message) {
  if(message.message_type === "message_to_webextension") {
      if(message.operation === "dialog_closed") {
        console.log("content.js ---------- dialog_closed")
      }
  } else {
      document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('message_from_extension', {detail:message}))
  }
});

document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('message_from_extension'))

console.log("content.js - ready - chrome.runtime.id: " + chrome.runtime.id);
