<!DOCTYPE html>
<html>
<head>
    <title><g:message code="vicketUserBaseDataSimulationCaption"/></title>
    <r:external uri="/images/euro_16.png"/>
    <r:require modules="application"/>
    <r:require modules="textEditorPC"/>
    <r:layoutResources />
</head>
<body style="overflow-y: scroll;">
<div id="vicketUserBaseDataSimulationDataDialog"
     style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
    <div class="errorMsgWrapper" style="display:none;"></div>
    <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
        <div id="pageTitle" style="display:table-cell;font-weight: bold; font-size: 1.4em; color: #48802c;
        text-align:center;vertical-align: middle;width: 80%;">
            <g:message code="initVicketUserBaseDataSimulationMsg"/>
        </div>
        <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
            <votingSystem:simpleButton id="testButton" style="margin:0px 0px 0px 30px;">
                <g:message code="goToResultViewMsg"/></votingSystem:simpleButton>
        </div>
    </div>
    <div id="formDataDiv">
        <form id="vicketUserBaseDataSimulationDataForm">
            <input type="hidden" autofocus="autofocus" />
            <input id="resetvicketUserBaseDataSimulationDataForm" type="reset" style="display:none;">
            <fieldset id="userBaseData">
                <legend style="font-size: 1.2em"><g:message code="userBaseDataCaption"/></legend>
                <div style="display: block; margin: 0px 0px 5px 0px;">
                    <label><g:message code="firstUserIndexMsg"/></label>
                    <input type="number" id="firstUserIndex" min="1" value="1" readonly required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="firstUserIndexMsg"/>"
                           placeholder="<g:message code="firstUserIndexMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">

                </div>
                <div style="display: block; margin: 0px 0px 5px 0px;">
                    <label><g:message code="numUsersMsg"/></label>
                    <input type="number" id="numUsers" min="0" value="1" required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="numRepresentativesMsg"/>"
                           placeholder="<g:message code="numRepresentativesMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">

                </div>

                <input type="url" id="vicketServerURL" style="width:280px; margin:20px 20px 0 20px;" required
                       value="http://vickets:8083/Vickets/" title="<g:message code="vicketServerURLMsg"/>"
                       placeholder="<g:message code="vicketServerURLMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </fieldset>


            <div style="position: relative; overflow:hidden; ">
                <votingSystem:simpleButton id="submitButton" isSubmitButton='true' style="margin:15px 20px 20px 0px;
                        width:450px; float:right;">
                    <g:message code="initVicketuserBaseDataButton"/>
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

//$("#resetvicketUserBaseDataSimulationDataForm").click()
//This is for number validation in Firefox
var allNumberFields = document.getElementsByClassName('userBaseDataInputNumber');
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


$('#vicketUserBaseDataSimulationDataForm').submit(function(event){
	event.preventDefault();
 	$(".errorMsgWrapper").fadeOut()


     var userBaseData = {userIndex:$('#firstUserIndex').val(),
        numUsers: $('#numUsers').val() }

	 var simulationData = {service:'vicketUserBaseDataSimulationService', status:"INIT_SIMULATION",
	 		 serverURL:$('#vicketServerURL').val(), userBaseData:userBaseData}

     showListenerDiv(true)
     showSimulationProgress(simulationData)
	 return false
});

function isValidForm() {
	//numRepresentativesMsg"/></label>numRepresentativesWithVote numUsersWithRepresentativeMsg numUsersWithRepresentativeWithVote


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
        $('#pageTitle').text('<g:message code="listeningtVicketUserBaseDataSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initVicketUserBaseDataSimulationMsg"/>')
    }
}


$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});

function showErrorMsg(errorMsg) {
	$("#vicketUserBaseDataSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#vicketUserBaseDataSimulationDataDialog .errorMsgWrapper").fadeIn()
}

</r:script>
<r:layoutResources />