<!DOCTYPE html>
<html>
<head>
  	<title><g:message code="manifestProtocolSimulationCaption"/></title>
   	<r:require modules="application"/>
	<r:layoutResources />
</head>
<div id="timeStampProtocolSinulationDataDialog" title="<g:message code="initTimeStampProtocolSimulationButton"/>"
	style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
	<div class="errorMsgWrapper" style="display:none;"></div>
    <div style="display:block;overflow: hidden; position: relative; margin:0px 0px 1px 0px; padding: 0 0 10px 0; ">
        <div style="margin:10px 0px 0px 0px; display: inline; position: absolute; right: 0px; left: 0px;">
            <p style="text-align: center;font-weight: bold; font-size: 1.4em; color: #48802c;">
                <g:message code="initTimeStampProtocolSimulationMsg"/>
            </p>
        </div>
        <votingSystem:simpleButton id="testButton"
            style="margin:0px 20px 0px 0px; padding: 0px 10px 0px 10px; float:right; display: inline;">
            Test
        </votingSystem:simpleButton>
    </div>
  	<div id="formDataDiv">
   		<form id="timeStampProtocolSinulationDataForm">
			<input type="hidden" autofocus="autofocus" />
			<input id="resetTimeStampProtocolSinulationDataForm" type="reset" style="display:none;">

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
                <input type="text" name="eventId" id="eventId" style="width:350px"  required
                    title="<g:message code="eventIdLbl"/>"
                    placeholder="<g:message code="eventIdLbl"/>"/>
                <input type="url" id="accessControlURL" style="width:300px; margin:0px 0px 0px 20px;" required
                       value="http://192.168.1.20:8080/AccessControl"
                       title="<g:message code="accessControlURLMsg"/>"
                       placeholder="<g:message code="accessControlURLMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>


            <div style="position: relative; overflow:hidden; ">
                <votingSystem:simpleButton id="submitButton" isButton='true'
                    style="margin:15px 20px 20px 0px;padding:2px 5px 2px 0px; height:30px; width:400px; float:right;">
                    <g:message code="initTimeStampProtocolSimulationButton"/>
                </votingSystem:simpleButton>
            </div>

   		</form>

  	</div>
</div>

<div id="simulationListenerDiv" style="display: none;">
    <g:include view="/include/listenSimulation.gsp"/>
</div>

</html> 
<r:script>

var isFormView = true

$("#testButton").click(function () {
    if(isFormView) {
        $('#formDataDiv').fadeOut()
        $('#simulationListenerDiv').fadeIn()
        $('#pageTitle').text('<g:message code="listeningTimeStampProtocolSimulationMsg"/>' + ": '" + $('#subject').val() + "'")
        isFormView = false;
    } else {
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initTimeStampProtocolSimulationMsg"/>')
        isFormView = true;
    }

});

$('#timeStampProtocolSinulationDataForm').submit(function(event){
	 event.preventDefault();

	
	 var simulationData = {service:"timeStampSimulationService", status:"INIT_SIMULATION",
	         accessControlURL:$('#accessControlURL').val(),
			 maxPendingResponses: $('#maxPendingResponses').val(), 
			 numRequestsProjected: $('#numRequestsProjected').val(),
			 eventId:$('#eventId').val()}



    $('#formDataDiv').fadeOut()
    $('#simulationListenerDiv').fadeIn()
     showSimulationProgress(simulationData)
	return false
});

function isValidForm() {
	if(!document.getElementById('accessControlURL').validity.valid) {
		$("#accessControlURL").addClass("formFieldError");
		showResultDialog('<g:message code="dataFormERRORLbl"/>', 
			'<g:message code="emptyFieldLbl"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}	      
	var accessControlURL = $('#accessControlURL').val()
	var suffix = "/"
	if((accessControlURL.indexOf(suffix, accessControlURL.length - suffix.length) == -1)) {
		accessControlURL = accessControlURL + "/"
	}
	accessControlURL = accessControlURL + "serverInfo"
	if(accessControlURL.indexOf("http://") != 0) {
		accessControlURL = "http://" + accessControlURL
	}
	return true
}

function showErrorMsg(errorMsg) {
	$("#timeStampProtocolSinulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#timeStampProtocolSinulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>
	<r:layoutResources />