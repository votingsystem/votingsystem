<!DOCTYPE html>
<html>
<head>
  	<title><g:message code="manifestProtocolSimulationCaption"/></title>
    <r:external uri="/images/TestWebApp.ico"/>
   	<r:require modules="application"/>
	<r:layoutResources />
</head>
<div id="timeStampProtocolSimulationDataDialog" title="<g:message code="initTimeStampProtocolSimulationButton"/>"
	style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
	<div class="errorMsgWrapper" style="display:none;"></div>
    <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
        <div id="pageTitle" style="display:table-cell;font-weight: bold; font-size: 1.4em; color: #48802c;
                text-align:center; vertical-align: middle;">
            <g:message code="initTimeStampProtocolSimulationMsg"/>
        </div>
        <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
            <votingSystem:simpleButton id="testButton" style="margin:0px 0px 0px 30px;">
                <g:message code="goToResultViewMsg"/></votingSystem:simpleButton>
        </div>
    </div>
  	<div id="formDataDiv">
   		<form id="timeStampProtocolSimulationDataForm">
			<input type="hidden" autofocus="autofocus" />
			<input id="resetTimeStampProtocolSimulationDataForm" type="reset" style="display:none;">

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
                <input type="number" name="eventId" id="eventId" min="1" value="1" style="width:350px"  required
                    title="<g:message code="eventIdLbl"/>"
                    placeholder="<g:message code="eventIdLbl"/>"/>
                <input type="url" id="accessControlURL" style="width:300px; margin:0px 0px 0px 20px;" required
                       value="http://sistemavotacion.org/AccessControl"
                       title="<g:message code="accessControlURLMsg"/>"
                       placeholder="<g:message code="accessControlURLMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>

            <div style="position: relative; overflow:hidden; ">
                <votingSystem:simpleButton id="submitButton" isSubmitButton='true'
                       style="margin:15px 20px 20px 0px; width:400px; float:right;">
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

$("#testButtonDiv").hide()

function showListenerDiv(isListening) {
     $("#testButtonDiv").show()
    if(isListening) {
        $("#testButton").text("<g:message code="goToFormViewMsg"/>")
        $('#formDataDiv').fadeOut()
        $('#simulationListenerDiv').fadeIn()
        $('#pageTitle').text('<g:message code="listeningTimeStampProtocolSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initTimeStampProtocolSimulationMsg"/>')
    }
}

$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});


$('#timeStampProtocolSimulationDataForm').submit(function(event){
	 event.preventDefault();

	 var simulationData = {service:"timeStampSimulationService", status:"INIT_SIMULATION",
	         accessControlURL:$('#accessControlURL').val(),
			 maxPendingResponses: $('#maxPendingResponses').val(), 
			 numRequestsProjected: $('#numRequestsProjected').val(),
			 eventId:$('#eventId').val()}

     showListenerDiv(true)
     showSimulationProgress(simulationData)
	 return false
});

function isValidForm() {
	if(!document.getElementById('accessControlURL').validity.valid) {
		$("#accessControlURL").addClass("formFieldError");
		showResultDialog('<g:message code="dataFormERRORLbl"/>',  '<g:message code="emptyFieldLbl"/>', function() {
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
	$("#timeStampProtocolSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#timeStampProtocolSimulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>
<r:layoutResources />