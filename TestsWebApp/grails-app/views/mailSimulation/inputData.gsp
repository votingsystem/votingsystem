<!DOCTYPE html>
<html>
<head>
  	<title><g:message code="mailSimulationCaption"/></title>
   	<r:require modules="application"/>
	<r:layoutResources />
</head>
<style>
    #formDataDiv label {
        width:240px;
        display:inline-block;
    }
</style>
<div id="mailProtocolSinulationDataDialog" title="<g:message code="initMailProtocolSimulationButton"/>"
	style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
	<div class="errorMsgWrapper" style="display:none;"></div>
    <div style="display:block;overflow: hidden; position: relative; margin:0px 0px 1px 0px; padding: 0 0 10px 0; ">
        <div style="margin:10px 0px 0px 0px; display: inline; position: absolute; right: 0px; left: 0px;">
            <p style="text-align: center;font-weight: bold; font-size: 1.4em; color: #48802c;">
                <g:message code="initMailProtocolSimulationMsg"/>
            </p>
        </div>
        <votingSystem:simpleButton id="testButton"
            style="margin:0px 20px 0px 0px; padding: 0px 10px 0px 10px; float:right; display: inline;">
            Test
        </votingSystem:simpleButton>
    </div>
  	<div id="formDataDiv">
   		<form id="mailProtocolSinulationDataForm">
			<input type="hidden" autofocus="autofocus" />
			<input id="resetMailProtocolSinulationDataForm" type="reset" style="display:none;">

            <div style="display: block;">
                <label><g:message code="numRequestsProjectedLbl"/></label>
                <input type="number" id="numRequestsProjected" min="1" value="1" required
                       style="width:110px;margin:0px 20px 0px 3px;"
                       title="<g:message code="numRequestsProjectedLbl"/>"
                       placeholder="<g:message code="numRequestsProjectedLbl"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')">
                <label ><g:message code="maxPendingResponsesLbl"/></label>
                <input type="number" id="maxPendingResponses" min="1" value="10" required
                       style="width:110px;margin:10px 20px 0px 3px;"
                       title="<g:message code="maxPendingResponsesLbl"/>"
                       placeholder="<g:message code="maxPendingResponsesLbl"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')">
            </div>


            <div style="margin:10px 0px 10px 0px">
                <label><g:message code="smtpHostNameMsg"/></label>
                <input id="smtpHostName" style="width:500px; margin:0px 0px 0px 3px;"
                       value="localhost"
                       title="<g:message code="smtpHostNameMsg"/>"
                       placeholder="<g:message code="smtpHostNameMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>

            <div style="margin:10px 0px 10px 0px">
                <label><g:message code="pop3HostNameMsg"/></label>
                <input id="pop3HostName" style="width:500px; margin:0px 0px 0px 3px;" required
                       value="localhost"
                       title="<g:message code="pop3HostNameMsg"/>"
                       placeholder="<g:message code="pop3HostNameMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>



            <div style="margin:10px 0px 10px 0px">
                <label><g:message code="userNameMsg"/></label>
                <input id="userName" style="width:500px; margin:0px 0px 0px 3px;" required
                       value="voting_system_access_control"
                       title="<g:message code="userNameMsg"/>"
                       placeholder="<g:message code="userNameMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>

            <div style="margin:10px 0px 10px 0px">
                <label><g:message code="domainNameMsg"/></label>
                <input id="domainName" style="width:500px; margin:0px 0px 0px 3px;" required
                       value="sistemavotacion.org"
                       title="<g:message code="domainNameMsg"/>"
                       placeholder="<g:message code="domainNameMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>

            <div style="margin:10px 0px 10px 0px">
                <label><g:message code="passwordMsg"/></label>
                <input id="password" style="width:500px; margin:0px 0px 0px 3px;" required
                       value="123456"
                       title="<g:message code="passwordMsg"/>"
                       placeholder="<g:message code="passwordMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>

            <div id="timerDiv" style="margin:10px 0px 10px 10px; overflow: hidden; height: 50px; display: table;">
                <div class="checkBox" style="display:table-cell;vertical-align: middle;">
                    <input type="checkbox" id="isWithTimer"/><label for="isWithTimer" style="width:600px;">
                        <g:message code="simulationTimerDataMsg"/></label>
                </div>
                <div id="timerInputDiv" style="display:table-cell;vertical-align: middle;">

                </div>
            </div>

            <div style="position: relative; overflow:hidden; ">
                <votingSystem:simpleButton id="submitButton" isButton='true'
                    style="margin:15px 20px 20px 0px;padding:2px 5px 2px 0px; height:30px; width:400px; float:right;">
                    <g:message code="initMailProtocolSimulationButton"/>
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
        $('#pageTitle').text('<g:message code="listeningMailProtocolSimulationMsg"/>' + ": '" + $('#subject').val() + "'")
        isFormView = false;
    } else {
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initMailProtocolSimulationMsg"/>')
        isFormView = true;
    }

});

$('#mailProtocolSinulationDataForm').submit(function(event){
	 event.preventDefault();

    var timer = {active:$("#isWithTimer").is(':checked'),
        time:$('#timerData').val()}


	 var simulationData = {service:"mailSimulationService", status:"INIT_SIMULATION",
	         smtpHostName:$('#smtpHostName').val(),
			 pop3HostName: $('#pop3HostName').val(),
			 userName: $('#userName').val(),
			 domainName: $('#domainName').val(),
			 password: $('#password').val(),
             maxPendingResponses: $('#maxPendingResponses').val(),
			 numRequestsProjected: $('#numRequestsProjected').val(),
			 timer:timer}


    $('#formDataDiv').fadeOut()
    $('#simulationListenerDiv').fadeIn()
     showSimulationProgress(simulationData)
	return false
});

function isValidForm() {

	return true
}

function showErrorMsg(errorMsg) {
	$("#mailProtocolSinulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#mailProtocolSinulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>
	<r:layoutResources />