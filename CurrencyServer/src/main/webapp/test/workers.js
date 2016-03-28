function messageHandler(event) {
    var messageSent = event.data;
    console.log("messageSent", messageSent)
    var messageReturned = "Message '" + messageSent + "' from a separate votingsytem browser thread!";
    this.postMessage(messageReturned);
}

this.addEventListener('message', messageHandler, false);