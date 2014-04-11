<!DOCTYPE html>
<html>
<head>
  	<title><g:message code="manifestProtocolSimulationCaption"/></title>
    <r:external uri="/images/TestWebApp.ico"/>
   	<r:require modules="application"/>
	<r:layoutResources />
</head>
<div id="multiSignProtocolSimulationDataDialog" title="<g:message code="initMultiSignProtocolSimulationButton"/>"
	style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
	<div class="errorMsgWrapper" style="display:none;"></div>
    <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
        <div id="pageTitle" style="display:table-cell;font-weight: bold; font-size: 1.4em; color: #48802c; text-align:center;vertical-align: middle;">
            <g:message code="initMultiSignProtocolSimulationMsg"/>
        </div>
        <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
            <votingSystem:simpleButton id="testButton" style="margin:0px 0px 0px 30px;">
                <g:message code="goToResultViewMsg"/></votingSystem:simpleButton>
        </div>
    </div>
  	<div id="formDataDiv">
   		<form id="multiSignProtocolSimulationDataForm">
			<input type="hidden" autofocus="autofocus" />
			<input id="resetMultiSignProtocolSimulationDataForm" type="reset" style="display:none;">

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
                       value="http://sistemavotacion.org/AccessControl"
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
                    style="margin:15px 20px 20px 0px; width:400px; float:right;">
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

$("#testButtonDiv").hide()

function showListenerDiv(isListening) {
     $("#testButtonDiv").show()
    if(isListening) {
        $("#testButton").text("<g:message code="goToFormViewMsg"/>")
        $('#formDataDiv').fadeOut()
        $('#simulationListenerDiv').fadeIn()
        $('#pageTitle').text('<g:message code="listeningMultiSignProtocolSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initMultiSignProtocolSimulationMsg"/>')
    }
}

$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});

$('#multiSignProtocolSimulationDataForm').submit(function(event){
	 event.preventDefault();

    var timer = {active:$("#isWithTimer").is(':checked'),
        time:$('#timerData').val()}
	
	 var simulationData = {service:"multiSignSimulationService", status:"INIT_SIMULATION",
	         serverURL:$('#serverURL').val(),
			 maxPendingResponses: $('#maxPendingResponses').val(), 
			 numRequestsProjected: $('#numRequestsProjected').val(),
			 timer:timer}

     showListenerDiv(true)
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
	$("#multiSignProtocolSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#multiSignProtocolSimulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>
	<r:layoutResources />