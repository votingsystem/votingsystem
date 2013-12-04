<!DOCTYPE html>
<html>
<head>
  	<title><g:message code="manifestProtocolSimulationCaption"/></title>
   	<r:require modules="application"/>
	<r:layoutResources />
</head>
<div id="multiSignProtocolSinulationDataDialog" title="<g:message code="initMultiSignProtocolSimulationButton"/>"
	style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
	<div class="errorMsgWrapper" style="display:none;"></div>
    <div style="display:block;overflow: hidden; position: relative; margin:0px 0px 1px 0px; padding: 0 0 10px 0; ">
        <div style="margin:10px 0px 0px 0px; display: inline; position: absolute; right: 0px; left: 0px;">
            <p style="text-align: center;font-weight: bold; font-size: 1.4em; color: #48802c;">
                <g:message code="initMultiSignProtocolSimulationMsg"/>
            </p>
        </div>
        <votingSystem:simpleButton id="testButton"
            style="margin:0px 20px 0px 0px; padding: 0px 10px 0px 10px; float:right; display: inline;">
            Test
        </votingSystem:simpleButton>
    </div>
  	<div id="formDataDiv">
   		<form id="multiSignProtocolSinulationDataForm">
			<input type="hidden" autofocus="autofocus" />
			<input id="resetMultiSignProtocolSinulationDataForm" type="reset" style="display:none;">

            <div style="display: block;">
                <label><g:message code="numRequestsProjectedLbl"/></label>
                <input type="number" id="numRequestsProjected" min="1" value="1" required
                       style="width:110px;margin:0px 20px 0px 3px;"
                       title="<g:message code="numRequestsProjectedLbl"/>"
                       placeholder="<g:message code="numRequestsProjectedLbl"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')">
                <label><g:message code="maxPendingResponsesLbl"/></label>
                <input type="number" id="maxPendingResponses" min="1" value="10" required
                       style="width:110px;margin:10px 20px 0px 3px;"
                       title="<g:message code="maxPendingResponsesLbl"/>"
                       placeholder="<g:message code="maxPendingResponsesLbl"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')">
            </div>


            <div style="margin:10px 0px 10px 0px">
                <label><g:message code="serverURLMsg"/></label>
                <input type="url" id="serverURL" style="width:500px; margin:0px 0px 0px 3px;" required
                       value="http://192.168.1.20:8080/AccessControl"
                       title="<g:message code="serverURLMsg"/>"
                       placeholder="<g:message code="serverURLMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>

            <div id="timerDiv" style="margin:10px 0px 10px 10px; overflow: hidden; height: 50px; display: table;">
                <div class="checkBox" style="display:table-cell;vertical-align: middle;">
                    <input type="checkbox" id="isWithTimer"/><label for="isWithTimer"><g:message code="simulationTimerDataMsg"/></label>
                </div>
                <div id="timerInputDiv" style="display:table-cell;vertical-align: middle;">

                </div>
            </div>

            <div style="position: relative; overflow:hidden; ">
                <votingSystem:simpleButton id="submitButton" isSubmitButton='true'
                    style="margin:15px 20px 20px 0px;padding:2px 5px 2px 0px; height:30px; width:400px; float:right;">
                    <g:message code="initMultiSignProtocolSimulationButton"/>
                </votingSystem:simpleButton>
            </div>

   		</form>

  	</div>
</div>

<div id="simulationListenerDiv" style="display: none;">
    <g:include view="/include/listenSimulation.gsp"/>
</div>

<template id="timerTemplate" style="display:none;">
    <input type="time" id="timerData" style="" required
           title="<g:message code="simulationTimerDataMsg"/>"
           placeholder="<g:message code="simulationTimerDataMsg"/>"
           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
           onchange="this.setCustomValidity('')">
</template>

</html> 
<r:script>

$("#isWithTimer").click(function () {
	if($("#isWithTimer").is(':checked')) {
		$('#timerInputDiv').append($('#timerTemplate').html());
	} else {
		$('#timerInputDiv').html("");
	}
})

var isFormView = true

$("#testButton").click(function () {
    if(isFormView) {
        $('#formDataDiv').fadeOut()
        $('#simulationListenerDiv').fadeIn()
        $('#pageTitle').text('<g:message code="listeningMultiSignProtocolSimulationMsg"/>' + ": '" + $('#subject').val() + "'")
        isFormView = false;
    } else {
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initMultiSignProtocolSimulationMsg"/>')
        isFormView = true;
    }

});

$('#multiSignProtocolSinulationDataForm').submit(function(event){
	 event.preventDefault();


    console.log("======== timerData: " + $('#timerData').val())

    var timer = {active:$("#isWithTimer").is(':checked'),
        time:$('#timerData').val()}
	
	 var simulationData = {service:"multiSignSimulationService", status:"INIT_SIMULATION",
	         serverURL:$('#serverURL').val(),
			 maxPendingResponses: $('#maxPendingResponses').val(), 
			 numRequestsProjected: $('#numRequestsProjected').val(),
			 timer:timer}


    $('#formDataDiv').fadeOut()
    $('#simulationListenerDiv').fadeIn()
     showSimulationProgress(simulationData)
	return false
});

function isValidForm() {
	if(!document.getElementById('serverURL').validity.valid) {
		$("#serverURL").addClass("formFieldError");
		showResultDialog('<g:message code="dataFormERRORLbl"/>', 
			'<g:message code="emptyFieldLbl"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}	      
	var serverURL = $('#serverURL').val()
	var suffix = "/"
	if((serverURL.indexOf(suffix, serverURL.length - suffix.length) == -1)) {
		serverURL = serverURL + "/"
	}
	serverURL = serverURL + "serverInfo"
	if(serverURL.indexOf("http://") != 0) {
		serverURL = "http://" + serverURL
	}
	return true
}

function showErrorMsg(errorMsg) {
	$("#multiSignProtocolSinulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#multiSignProtocolSinulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>
	<r:layoutResources />