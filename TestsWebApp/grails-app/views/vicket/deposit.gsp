<!DOCTYPE html>
<html>
<head>
    <title><g:message code="vicketDepositSimulationCaption"/></title>
    <meta name="layout" content="main" />
</head>
<body style="">
<div class="pageContenDiv">
    <div style="padding: 0px 30px 0px 30px;">
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
                <h3>
                    <div id="pageTitle" class="pageHeader text-center">
                        <g:message code="initVicketDepositSimulationMsg"/>
                    </div>
                </h3>
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
                        <legend style="font-size: 1.2em; font-weight: bold;"><g:message code="depositCaption"/></legend>
                        <div style="margin: 0px 0px 5px 0px;">
                            <div class="form-group">
                                <label class="col-sm-3"><g:message code="depositAmount"/></label>
                                <input type="number" id="depositAmount" min="0" value="1" required
                                       class="DepositInputNumber form-control col-sm-10"
                                       style="width:150px;margin:0px 20px 0px 7px;"
                                       title="<g:message code="depositAmount"/>"
                                       placeholder="<g:message code="depositAmount"/>"
                                       oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                                       onchange="this.setCustomValidity('')">
                            </div>

                            <select id="currencySelect" style="margin:0px 20px 0px 0px; width:280px;"
                                    class="form-control" title="<g:message code="currencyLbl"/>">
                                <option value="<g:message code="euroLbl"/>"> - <g:message code="euroLbl"/> - </option>
                            </select>

                            <div class="form-group">
                                <input type="text" id="subject" style="width:280px; margin:10px 20px 0 20px;" required
                                       title="<g:message code="transactionSubjectMsg"/>" class="form-control col-sm-4"
                                       placeholder="<g:message code="transactionSubjectMsg"/>"
                                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                                       onchange="this.setCustomValidity('')"/>

                                <input type="url" id="vicketServerURL" style="width:280px; margin:10px 20px 0 20px;" required
                                       value="http://vickets/Vickets/" title="<g:message code="vicketServerURLMsg"/>"
                                       placeholder="<g:message code="vicketServerURLMsg"/>" class="form-control col-sm-4"
                                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                                       onchange="this.setCustomValidity('')"/>
                            </div>
                        </div>
                    </fieldset>

                    <div style="position: relative; overflow:hidden; ">
                        <button id="submitButton" type="submit" class="btn btn-warning" style="margin:25px 20px 20px 0px; float:right;">
                            <g:message code="initVicketDepositButton"/>
                        </button>
                    </div>

                </form>

            </div>
        </div>

        <div id="simulationListenerDiv" style="display: none;">
            <g:include view="/include/listenSimulation.gsp"/>
        </div>
    </div>
</div>
</body>
</html>
<asset:script>

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

</asset:script>