//"yy/MM/dd 12:00:00"
var pickerOpts = {showOn: 'both', buttonImage: "${createLinkTo(dir: 'images', file: 'appointment.png')}", 
		buttonImageOnly: true, dateFormat: 'yy/MM/dd', defaultDate: null};


var VotingSystemClient = function () {

	this.setMessageToSignatureClient = function (messageJSON) {
		var message = JSON.stringify(messageJSON)
		//console.log("---- setMessageToSignatureClient: " + message);
		androidClient.setVotingWebAppMessage(message);
	}
}

var votingSystemClient = new VotingSystemClient()