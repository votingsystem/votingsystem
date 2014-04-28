<!DOCTYPE html>
<html>
<head>
    <title><g:message code="vicketDepositSimulationCaption"/></title>
    <r:external uri="/images/euro_16.png"/>
    <meta name="layout" content="main" />
</head>
<body style="">
<div class="row" style="">
    <ol class="breadcrumbVS pull-left">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'simulation', action:'vickets', absolute:true)}">
            <g:message code="vicketsOperationsLbl"/></a></li>
        <li class="active"><g:message code="makeDepositButton"/></li>
    </ol>
</div>
<div id="vicketDepositSimulationDataDialog"
     style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
    <div class="errorMsgWrapper" style="display:none;"></div>
    <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
        <div id="pageTitle" class="operationPageTitle">
            <g:message code="initVicketDepositSimulationMsg"/>
        </div>
        <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
            <button id="testButton" type="button" class="btn btn-default" style="margin:0px 0px 0px 30px;">
                <g:message code="goToResultViewMsg"/>
            </button>
        </div>
    </div>
    <div id="formDataDiv">
        <form id="vicketDepositSimulationDataForm">
            <input type="hidden" autofocus="autofocus" />
            <input id="resetvicketDepositSimulationDataForm" type="reset" style="display:none;">
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

                    <select id="currencySelect" style="margin:0px 20px 0px 0px;" title="<g:message code="currencyLbl"/>">
                        <option value="<g:message code="euroLbl"/>"> - <g:message code="euroLbl"/> - </option>
                    </select>

                    <input type="text" id="subject" style="width:280px; margin:10px 20px 0 20px;" required
                           title="<g:message code="transactionSubjectMsg"/>"
                           placeholder="<g:message code="transactionSubjectMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')"/>

                    <input type="url" id="vicketServerURL" style="width:280px; margin:10px 20px 0 20px;" required
                           value="http://vickets:8083/Vickets/" title="<g:message code="vicketServerURLMsg"/>"
                           placeholder="<g:message code="vicketServerURLMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')"/>
                </div>
            </fieldset>


            <div style="position: relative; overflow:hidden; ">
                <button id="submitButton" type="submit" class="btn btn-default" style="margin:15px 20px 20px 0px; width:450px; float:right;">
                    <g:message code="initVicketDepositButton"/>
                </button>
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

//$("#resetvicketDepositSimulationDataForm").click()
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


$('#vicketDepositSimulationDataForm').submit(function(event){
	event.preventDefault();
 	$(".errorMsgWrapper").fadeOut()

	 var simulationData = {service:'vicketDepositSimulationService', status:"INIT_SIMULATION",
	 		 serverURL:$('#vicketServerURL').val(),  depositAmount: $('#depositAmount').val(),
	 		 subject:$('#subject').val(), currency:$( "#currencySelect option:selected").val()}

     showListenerDiv(true)
     showSimulationProgress(simulationData)
	 return false
});

function isValidForm() {


	if(!document.getElementById('vicketServerURL').validity.valid) {
		$("#vicketServerURL").addClass( "formFieldError" );
		showResultDialog('<g:message code="dataFormERRORLbl"/>',
			'<g:message code="emptyFieldLbl"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}
	var vicketServerURL = $('#vicketServerURL').val()
	var suffix = "/"
	if((vicketServerURL.indexOf(suffix, vicketServerURL.length - suffix.length) == -1)) {
		vicketServerURL = vicketServerURL + "/"
	}
	vicketServerURL = vicketServerURL + "serverInfo"
	if(vicketServerURL.indexOf("http://") != 0) {
		vicketServerURL = "http://" + vicketServerURL
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
        $('#pageTitle').text('<g:message code="listeningtVicketDepositSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initVicketDepositSimulationMsg"/>')
    }
}


$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});

function showErrorMsg(errorMsg) {
	$("#vicketDepositSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#vicketDepositSimulationDataDialog .errorMsgWrapper").fadeIn()
}

</r:script>