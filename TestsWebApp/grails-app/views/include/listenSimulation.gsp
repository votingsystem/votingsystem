<div>
<div id="simulationRunningDialog" style="padding: 20px;">
	<div class="errorMsgWrapper" style="display: none;"></div>

    <div id="progressDiv" style="vertical-align: middle;height:100%;">
        <progress style="display:block;margin:20px auto 20px auto;"></progress>
    </div>

  	<div id="messageFromService" class="messageFromServiceBox" style="display:none;"></div>

</div>

<template id="processingMessageTemplate" style="display:none;">
    <div style="display:block; overflow:hidden; margin: 0 0 20px 0;">
        <div style="display: inline;">
            <label style="font-weight: bold;"><g:message code="statusCodelLbl"/>:</label>
            <div style="display: inline;margin: 0 20px 0 0;font-size: 2em;font-weight: bold;" id="statusCode">{0}</div>
        </div>
        <div style="display: inline; float: right; margin: 0 20px 0 0;">
            <label style="font-weight: bold;"><g:message code="timDurationLbl"/>:</label>
            <div style="display: inline;font-family:digi;font-size: 2em;">{5}</div>
        </div>
    </div>
    <div id="messageDiv" style="margin: 0 0 15px 0;">
        <label style="font-weight: bold;"><g:message code="messageLbl"/>:</label>
        <div id="message">{1}</div>
    </div>
    <div id="numRequestsDiv" style="margin: 0 0 15px 0;">
        <label style="font-weight: bold;"><g:message code="numRequestsLbl"/>:</label><label>{2}</label>
    </div>
    <div>
        <div id="numRequestsOKDiv" style="display: inline; width: 700px; margin: 0 50px 0 0;">
            <label style="font-weight: bold;"><g:message code="numRequestsOKLbl"/>:</label><label>{3}</label>
        </div>
        <div id="numRequestsERRORDiv" style="display: inline; width: 400px;">
            <label style="font-weight: bold;"><g:message code="numRequestsERRORLbl"/>:</label><label>{4}</label>
        </div>
    </div>
    <div id="errorsDiv">
        <label style="font-weight: bold;"><g:message code="errorsListLbl"/>:</label>
        <div>{6}</div>
    </div>
</template>
<template id="messageTemplate" style="display:none;">
    <div style="display:block; overflow:hidden; margin: 0 0 20px 0;">
        <div style="display: inline;">
            <label style="font-weight: bold;"><g:message code="statusCodelLbl"/>:</label>
            <div style="display: inline;margin: 0 20px 0 0;font-size: 2em;font-weight: bold;">{0}</div>
        </div>
    </div>
    <div style="margin: 0 0 15px 0;">
        <label style="font-weight: bold;"><g:message code="messageLbl"/>:</label>
        <div>{1}</div>
    </div>
</template>
</div>
<r:script>

$("#listenButton").click(function() {	
	messageToService = {service:"claimSimulationService", operation:"LISTEN"}
	SimulationService.initialize();
 })

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

var processingMessageTemplate = $('#processingMessageTemplate').html()
var messageTemplate = $('#messageTemplate').html()

function processResponse(response) {
	var responseJSON = toJSON(response)
	if(responseJSON != null) {

	    if(responseJSON.statusCode == ResponseVS.SC_PROCESSING) {
            if(!$("#progressDiv").is(":visible")) $("#progressDiv").fadeIn();
	    }  else $("#progressDiv").hide()

        var messageFromServiceHTML

	    if(responseJSON.simulationData != null) {
            messageFromServiceHTML = processingMessageTemplate.format(responseJSON.statusCode, responseJSON.message,
                responseJSON.simulationData.numRequestsProjected, responseJSON.simulationData.numRequestsOK,
                responseJSON.simulationData.numRequestsERROR, responseJSON.simulationData.timeDuration,
                responseJSON.simulationData.errorList);
	    } else if(responseJSON.message != null && '' != responseJSON.message) {
            messageFromServiceHTML = messageTemplate.format(responseJSON.statusCode, responseJSON.message)
	    }

        $("#messageFromService").html(messageFromServiceHTML)

        if(responseJSON.message == null || '' == responseJSON.message) {
	        $("#messageDiv").hide()
	    }
	    if(responseJSON.simulationData != null) {
	        $("#errorsDiv").hide()
	    }
	    if(!$("#messageFromService").is(":visible")) $("#messageFromService").fadeIn();
	} else console.log("NULL message -> "+ JSON.stringify(message))
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
    if(SimulationService.socket == null || 3 == SimulationService.socket.readyState) {
        console.log("missing message - socket closed")
    } else if(messageStr != ''){
        SimulationService.socket.send(messageStr);
    }
});

SimulationService.close = (function() {
	//states: CONNECTING	0, OPEN	1, CLOSING	2, CLOSED	3
    if(SimulationService.socket == null || 3 == SimulationService.socket.readyState) {
        console.log("socket already closed")
        return
    } else console.log(" closing socket connection")
	if(messageToService != null) {
        messageToService.status = Status.FINISH_SIMULATION
	    SimulationService.sendMessage(messageToService)
	}
	if(SimulationService.socket != null) SimulationService.socket.close()
});


function showSimulationProgress(simulationData) {
    console.log("listenSimulation.showSimulationProgress")
    if(simulationData != null) {
        $("#messageFromService").hide()
        if(!$("#progressDiv").is(":visible")) $("#progressDiv").fadeIn();
        messageToService = simulationData
        SimulationService.initialize();
        $(".errorMsgWrapper").fadeOut()
    } else showErrorMsg('<g:message code="simulationDataNull"/>')
}

function showErrorMsg(errorMsg) {
	$(".errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$(".errorMsgWrapper").fadeIn()
}
	
</r:script>