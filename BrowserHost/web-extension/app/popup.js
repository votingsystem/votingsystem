console.log("popup.js - chrome.runtime.id: " + chrome.runtime.id)


document.addEventListener('DOMContentLoaded', function () {
    chrome.tabs.create({ url: "https://192.168.1.5/CurrencyServer/" });
});
