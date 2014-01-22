<!DOCTYPE html>
<html>
<head>
    <title><g:message code="ticketDepositSimulationCaption"/></title>
    <r:external uri="/images/euro_16.png"/>
    <r:require modules="application"/>
    <r:require modules="textEditorPC"/>
    <r:layoutResources />
</head>
<body style="overflow-y: scroll;">
<div id="ticketDepositSimulationDataDialog"
     style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
    <div class="errorMsgWrapper" style="display:none;"></div>
    <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
        <div id="pageTitle" style="display:table-cell;font-weight: bold; font-size: 1.4em; color: #48802c;
        text-align:center;vertical-align: middle;width: 80%;">
            <g:message code="initTicketDepositSimulationMsg"/>
        </div>
        <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
            <votingSystem:simpleButton id="testButton" style="margin:0px 0px 0px 30px;">
                <g:message code="goToResultViewMsg"/></votingSystem:simpleButton>
        </div>
    </div>
    <div id="formDataDiv">
        <form id="ticketDepositSimulationDataForm">
            <input type="hidden" autofocus="autofocus" />
            <input id="resetticketDepositSimulationDataForm" type="reset" style="display:none;">
            <fieldset id="Deposit">
                <legend style="font-size: 1.2em"><g:message code="depositCaption"/></legend>
                <div style="display: block; margin: 0px 0px 5px 0px;">
                    <label><g:message code="depositAmount"/></label>
                    <input type="number" id="depositAmount" min="0" value="1" required
                           class="DepositInputNumber"
                           style="width:150px;margin:10px 20px 0px 7px;"
                           title="<g:message code="depositAmount"/>"
                           placeholder="<g:message code="depositAmount"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">
                    <input type="url" id="ticketServerURL" style="width:280px; margin:10px 20px 0 20px;" required
                           value="http://tickets:8080/Tickets/" title="<g:message code="ticketServerURLMsg"/>"
                           placeholder="<g:message code="ticketServerURLMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')"/>
                </div>
            </fieldset>


            <div style="position: relative; overflow:hidden; ">
                <votingSystem:simpleButton id="submitButton" isSubmitButton='true' style="margin:15px 20px 20px 0px;
                        width:450px; float:right;">
                    <g:message code="initTicketDepositButton"/>
                </votingSystem:simpleButton>
            </div>

        </form>

    </div>
</div>

<div id="simulationListenerDiv" style="display: none;">
    <g:include view="/include/listenSimulation.gsp"/>
</div>

</body>
</html>
<r:script>

//$("#resetticketDepositSimulationDataForm").click()
//This is for number validation in Firefox
var allNumberFields = document.getElementsByClassName('DepositInputNumber');
for (var inputElement in allNumberFields) {
    if(allNumberFields[inputElement] instanceof HTMLInputElement) {
        allNumberFields[inputElement].addEventListener('change', function(event) {
            if (isNaN(Number(event.target.value))) {
                event.target.message = "<g:message code='numberFieldLbl'/>"
                event.target.setCustomValidity("DummyInvalid");
            } else {
                event.target.message = null
                event.target.setCustomValidity("");
            }
        }, false);
        allNumberFields[inputElement].addEventListener('invalid', setInvalidMsg, false);
    }
}

function setInvalidMsg(event) {
    if( event.target.message != null) {
        event.target.setCustomValidity(event.target.message);
    }
}

var electionEditorDiv = $("#electionEditorDiv")


$('#ticketDepositSimulationDataForm').submit(function(event){
	event.preventDefault();
 	$(".errorMsgWrapper").fadeOut()

	 var simulationData = {service:"ticketDepositSimulationService", status:"INIT_SIMULATION",
	 		 serverURL:$('#ticketServerURL').val(),  depositAmount: $('#depositAmount').val()}

     showListenerDiv(true)
     showSimulationProgress(simulationData)
	 return false
});

function isValidForm() {


	if(!document.getElementById('ticketServerURL').validity.valid) {
		$("#ticketServerURL").addClass( "formFieldError" );
		showResultDialog('<g:message code="dataFormERRORLbl"/>',
			'<g:message code="emptyFieldLbl"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}
	var ticketServerURL = $('#ticketServerURL').val()
	var suffix = "/"
	if((ticketServerURL.indexOf(suffix, ticketServerURL.length - suffix.length) == -1)) {
		ticketServerURL = ticketServerURL + "/"
	}
	ticketServerURL = ticketServerURL + "serverInfo"
	if(ticketServerURL.indexOf("http://") != 0) {
		ticketServerURL = "http://" + ticketServerURL
	}
	return true
}

$("#testButtonDiv").hide()

function showListenerDiv(isListening) {
    $("#testButtonDiv").show()
    if(isListening) {
        $("#testButton").text("<g:message code="goToFormViewMsg"/>")
        $('#formDataDiv').fadeOut()
        $('#simulationListenerDiv').fadeIn()
        $('#pageTitle').text('<g:message code="listeningtTicketDepositSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initTicketDepositSimulationMsg"/>')
    }
}


$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});

function showErrorMsg(errorMsg) {
	$("#ticketDepositSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#ticketDepositSimulationDataDialog .errorMsgWrapper").fadeIn()
}

</r:script>
<r:layoutResources />