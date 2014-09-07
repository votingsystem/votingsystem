<!DOCTYPE html>
<html>
<head>
    <title><g:message code="initMultiSignProtocolSimulationButton"/></title>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'simulation', action:'votingSystem', absolute:true)}">
                <g:message code="votingSystemOperationsLbl"/></a></li>
            <li class="active"><g:message code="initMultiSignProtocolSimulationButton"/></li>
        </ol>
    </div>
    <div id="multiSignProtocolSimulationDataDialog" title="<g:message code="initMultiSignProtocolSimulationButton"/>"
         style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
        <div class="errorMsgWrapper" style="display:none;"></div>
        <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
            <h3>
                <div id="pageTitle" class="pageHeader text-center">
                    <g:message code="initMultiSignProtocolSimulationMsg"/>
                </div>
            </h3>
            <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
                <button id="testButton" type="button" class="btn btn-default" style="margin:0px 0px 0px 30px;">
                    <g:message code="goToResultViewMsg"/>
                </button>
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
                        <input type="checkbox" id="isWithTimer"/><label><g:message code="simulationTimerDataMsg"/></label>
                    </div>
                    <div id="timerInputDiv" style="display:table-cell;vertical-align: middle;">

                    </div>
                </div>
                <button id="submitButton" type="submit" class="btn btn-warning"
                        style="margin:15px 20px 20px 0px; float:right;">
                    <g:message code="initMultiSignProtocolSimulationButton"/>
                </button>
            </form>

        </div>
    </div>

    <template id="timerTemplate" style="display:none;">
        <input type="time" id="timerData" style="" required
               title="<g:message code="simulationTimerDataMsg"/>"
               placeholder="<g:message code="simulationTimerDataMsg"/>"
               oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
               onchange="this.setCustomValidity('')">
    </template>
</div>
</body>
</html> 
<asset:script>

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
	
</asset:script>