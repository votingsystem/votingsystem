<!DOCTYPE html>
<html>
<head>
  	<title><g:message code="manifestProtocolSimulationRunning"/></title>
   	<r:require modules="application"/>
	<r:layoutResources />
</head>
<body>
<div id="simulationRunningDialog">
	<div class="errorMsgWrapper" style="display: none;"></div>

	<div id="appClock" style="font-family:digi; font-size: 2em;"></div>

  	<div id="messageFromService" style="font-family:digi; font-size: 2em;">

  	</div>
  	
	<div id="progressDiv" style="vertical-align: middle;height:100%;">
		<progress style="display:block;margin:0px auto 20px auto;"></progress>
	</div>
	
	<votingSystem:simpleButton id="listenButton" isButton='true'  
		style="margin:15px 20px 0px 0px;padding:2px 5px 2px 0px; height:30px;">
			Listen broadcast
	</votingSystem:simpleButton>
  	
</div>
</body>
</html> 
<r:script>

$(".errorMsgWrapper").fadeOut()

$("#listenButton").click(function() {	
	console.log("============= listenButton")

	messageToService = {service:"manifestSimulationService",
		operation:Operation.LISTEN}
	SimulationService.initialize();
 })

startclock()

var messageToService = null

var SimulationService = {};

SimulationService.socket = null;

SimulationService.connect = (function(host) {

    if ('WebSocket' in window) {
        SimulationService.socket = new WebSocket(host);
    } else if ('MozWebSocket' in window) {
        SimulationService.socket = new MozWebSocket(host);
    } else {
        console.log('<g:message code="browserWithoutWebsocketSupport"/>');
        return;
    }

    SimulationService.socket.onopen = function () {
        console.log('Info: WebSocket connection opened.');
        if(messageToService != null) {
        	SimulationService.sendMessage(messageToService);
        }
    };

    SimulationService.socket.onclose = function () {
    	console.log('Info: WebSocket closed.');
    };

    SimulationService.socket.onmessage = function (message) {
        console.log('Message from service: ' + message.data);
        processResponse(message.data)
    };
});

function processResponse(response) {
	var responseJSON = toJSON(response)
	if(StatusCode.SC_TERMINATED == responseJSON.statusCode) {
		$("#progressDiv").hide()
	}
	$("#messageFromService").html(response)
}

SimulationService.initialize = function() {
	var serviceURL = '${grailsApplication.config.grails.serverURL}/websocket/service' 
    if (window.location.protocol == 'http:') {
        SimulationService.connect(serviceURL.replace('http', 'ws'));
        console.log('Connecting with: ' + serviceURL.replace('http', 'ws'));
    } else {
        SimulationService.connect(serviceURL.replace('http', 'wss'));
    }
};

SimulationService.sendMessage = (function(message) {
	var messageStr = JSON.stringify(message); 
	console.log("sendMessage to simulation service: " + messageStr)
    if (messageStr != '') {
        SimulationService.socket.send(messageStr);
    }
});

SimulationService.close = (function() {
	console.log("closing socket connection")
	messageToService["operation"] = Operation.CANCEL_SIMULATION
	SimulationService.sendMessage(messageToService)
	SimulationService.socket.close()
});

if(window.opener != null && window.opener.simulationData != null) {
	messageToService = window.opener.simulationData
	
	console.log(" ============= messageToService: " + messageToService)
	
	messageToService.operation = Operation.INIT_SIMULATION
	SimulationService.initialize();
} else {
	showErrorMsg('<g:message code="simulationDataNull"/>')
}

function showErrorMsg(errorMsg) {
	$(".errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$(".errorMsgWrapper").fadeIn()
}
	
</r:script>
<r:layoutResources />