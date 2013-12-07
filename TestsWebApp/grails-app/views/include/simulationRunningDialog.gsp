<div id="simulationRunningDialog" title="<g:message code="manifestProtocolSimulationRunning"/>">
	<div class="errorMsgWrapper" style="display:none; margin:"></div>

	<div id="appClock" style="font-family:digi; font-size: 2em;"></div>

  	<div id="messageFromService" style="font-family:digi; font-size: 2em;">

  	</div>
  	
	<div id="progressDiv" style="vertical-align: middle;height:100%;">
		<progress style="display:block;margin:0px auto 20px auto;"></progress>
	</div>
	
	<votingSystem:simpleButton id="listenButton" isSubmitButton='true' style="margin:15px 20px 0px 0px;">
			Listen broadcast
	</votingSystem:simpleButton>
  	
</div>

<r:script>

$("#listenButton").click(function() {	
	messageToService = {service:"manifestSimulationService", status:"LISTEN"}
	SimulationService.initialize();
 })

startclock()

$("#simulationRunningDialog").dialog({
   	  width: 700, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
 			   				$("#submitManifestProtocolSinulationDataForm").click() 	   	   			   				
 			        	}},
           {text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
             		SimulationService.close()
	   				$(this).dialog("close");
       	 	}}],
      show: {effect:"fade", duration: 700},
      hide: {effect: "fade",duration: 700},
      open: function( event, ui ) {

	  }
    });


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
	if(ResponseVS.SC_TERMINATED == responseJSON.statusCode) {
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
	messageToService["operation"] = "FINISH_SIMULATION"
	SimulationService.sendMessage(messageToService)
	SimulationService.socket.close()
});


function showSimulationRunningDialog(simulationData) {
	$("#simulationRunningDialog").dialog("open");
	messageToService = simulationData
	messageToService.operation = "INIT_SIMULATION"
	SimulationService.initialize();
}

</r:script>