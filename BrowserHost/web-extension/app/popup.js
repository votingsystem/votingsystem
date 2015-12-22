console.log("popup.js - chrome.runtime.id: " + chrome.runtime.id)


document.addEventListener('DOMContentLoaded', function () {
    chrome.tabs.create({ url: "http://localhost:8080/Currency/" });
});
