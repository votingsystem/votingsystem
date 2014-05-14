<!DOCTYPE html>
<html>
<head>
    <title><g:message code="electionProtocolSimulationCaption"/></title>
    <meta name="layout" content="main" />
    <r:require modules="textEditorPC"/>
</head>
<body style="overflow-y: scroll;">
<div class="row">
    <ol class="breadcrumbVS pull-left">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'simulation', action:'votingSystem', absolute:true)}">
            <g:message code="votingSystemOperationsLbl"/></a></li>
        <li class="active"><g:message code="initElectionProtocolSimulationButton"/></li>
    </ol>
</div>
<div id="electionProtocolSimulationDataDialog"
     style="padding:0px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
    <div class="errorMsgWrapper" style="display:none;"></div>
    <div style="margin: 0px 0px 30px 0px;display: table; width: 100%;">
        <h3>
            <div id="pageTitle" class="pageHeader text-center">
                <g:message code="initElectionProtocolSimulationMsg"/>
            </div>
        </h3>
        <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
            <button id="testButton" type="button" class="btn btn-default" style="margin:0px 0px 0px 30px;">
                <g:message code="goToResultViewMsg"/> <i class="fa fa fa-check"></i>
            </button>
        </div>
    </div>
    <div id="formDataDiv">
        <form id="electionProtocolSimulationDataForm">
            <input type="hidden" autofocus="autofocus" />
            <input id="resetElectionProtocolSimulationDataForm" type="reset" style="display:none;">
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
                    <label><g:message code="numUsersWithoutRepresentativeMsg"/></label>
                    <input type="number" id="numUsersWithoutRepresentative" min="0" value="1" required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="numRepresentativesMsg"/>"
                           placeholder="<g:message code="numRepresentativesMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">


                    <label><g:message code="numUsersWithoutRepresentativeWithVoteMsg"/></label>
                    <input type="number" id="numUsersWithoutRepresentativeWithVote" min="0" value="1" required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="numRepresentativesWithVoteMsg"/>"
                           placeholder="<g:message code="numRepresentativesWithVoteMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">
                </div>
                <div style="display: block; margin: 0px 0px 5px 0px;">
                    <label><g:message code="numRepresentativesMsg"/></label>
                    <input type="number" id="numRepresentatives" min="0" value="1" required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="numRepresentativesMsg"/>"
                           placeholder="<g:message code="numRepresentativesMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">


                    <label><g:message code="numRepresentativesWithVoteMsg"/></label>
                    <input type="number" id="numRepresentativesWithVote" min="0" value="1" required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="numRepresentativesWithVoteMsg"/>"
                           placeholder="<g:message code="numRepresentativesWithVoteMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">
                </div>

                <div style="display: block; margin: 0px 0px 5px 0px;">
                    <label><g:message code="numUsersWithRepresentativeMsg"/></label>
                    <input type="number" id="numUsersWithRepresentative" min="0" value="1" required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="numUsersWithRepresentativeMsg"/>"
                           placeholder="<g:message code="numUsersWithRepresentativeMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">

                    <label><g:message code="numUsersWithRepresentativeWithVoteMsg"/></label>
                    <input type="number" id="numUsersWithRepresentativeWithVote" min="0" value="1" required
                           class="userBaseDataInputNumber"
                           style="width:120px;margin:10px 20px 0px 7px;"
                           title="<g:message code="numUsersWithRepresentativeWithVoteMsg"/>"
                           placeholder="<g:message code="numUsersWithRepresentativeWithVoteMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">
                </div>
            </fieldset>

            <fieldset style="margin:10px 0px 0px 0px;">
                <legend style="font-size: 1.2em"><g:message code="eventDataCaption"/></legend>
                <div>
                    <label><g:message code="maxPendingResponsesLbl"/></label>
                    <input type="number" id="maxPendingResponses" min="1" value="10" required
                           style="width:120px;margin:10px 20px 0px 3px;"
                           title="<g:message code="maxPendingResponsesLbl"/>"
                           placeholder="<g:message code="maxPendingResponsesLbl"/>"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')">
                </div>

                <div style="margin:10px auto 0px auto; width:100%; height:30px; position: relative; overflow: hidden;">
                    <label><g:message code="eventStateOnFinishLbl"/></label>
                    <select id="eventStateOnFinishSelect" style="margin:0px 20px 0px 0px;"
                            title="<g:message code="setEventStateLbl"/>">
                        <option value=""> - <g:message code="eventAsDateRangeLbl"/> - </option>
                        <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="eventCancelledLbl"/> - </option>
                        <option value="DELETED" style="color:#cc1606;"> - <g:message code="eventDeletedLbl"/> - </option>
                    </select>
                </div>

                <div>
                    <label>${message(code:'dateInitLbl')}</label>
                    <votingSystem:datePicker id="dateInit"  style="width:160px;"
                                             title="${message(code:'dateInitLbl')}"
                                             oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
                                             onchange="this.setCustomValidity('')"></votingSystem:datePicker>
                    <label>${message(code:'dateFinishLbl')}</label>
                    <votingSystem:datePicker style="width:160px; margin:0px 0px 0px 15px;" id="dateFinish"
                                             title="${message(code:'dateFinishLbl')}"
                                             oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
                                             onchange="this.setCustomValidity('')"></votingSystem:datePicker>
                </div>

                <div style="margin:10px 0px 10px 0px">
                    <input type="text" name="subject" id="subject" style="width:280px;"  required
                           title="<g:message code="subjectLbl"/>"
                           placeholder="<g:message code="subjectLbl"/>"/>
                    <input type="url" id="accessControlURL" style="width:280px; margin:0 20px 0 20px;" required
                           value="http://sistemavotacion.org/AccessControl"
                           title="<g:message code="accessControlURLMsg"/>"
                           placeholder="<g:message code="accessControlURLMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')"/>
                    <input type="url" id="controlCenterURL" style="width:280px;" required
                           value="http://sistemavotacion.org/ControlCenter"
                           title="<g:message code="controlCenterURLMsg"/>"
                           placeholder="<g:message code="controlCenterURLMsg"/>"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')"/>
                </div>

                <votingSystem:textEditor id="electionEditorDiv" style="height:250px;"/>

                <div id="backupDiv" style="margin:10px 0px 10px 10px; overflow: hidden; height: 50px; display: table;">
                    <div class="checkBox" style="display:table-cell;vertical-align: middle;">
                        <input type="checkbox" id="requestBackup"/><label for="requestBackup"><g:message code="requestBackupLbl"/></label>
                    </div>
                    <div id="emailDiv" style="display:table-cell;vertical-align: middle;"></div>
                </div>

                <div style="margin:0px 0px 0px 0px; overflow: hidden;width: 100%;">
                    <button id="addElectionFieldButton" type="button" class="btn btn-default" style="margin:15px 20px 20px 0px; float:right;">
                        <g:message code="addElectionFieldLbl"/>
                    </button>
                </div>

                <fieldset id="fieldsBox" class="fieldsBox" style="display:none;border: #f2f2f2 solid 1px;">
                    <legend id="fieldsLegend" style="font-size: 1.2em"><g:message code="electionsFieldLegend"/></legend>
                    <div id="fields"></div>
                </fieldset>
            </fieldset>

            <div style="position: relative; overflow:hidden; ">
                <button id="submitButton" type="submit" class="btn btn-default" style="margin:15px 20px 20px 0px;
                width:450px; float:right;">
                    <g:message code="initElectionProtocolSimulationButton"/>
                </button>
            </div>
        </form>
    </div>
</div>

<div id="simulationListenerDiv" style="display: none;">
    <g:include view="/include/listenSimulation.gsp"/>
</div>

<g:include view="/include/dialog/addVoteOptionDialog.gsp"/>
<template id="emailTemplate" style="display:none;">
    <input type="email" id="emailRequestBackup" style="width:300px;" required
           title="<g:message code="emailRequestBackupMsg"/>"
           placeholder="<g:message code="emailLbl"/>"
           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
           onchange="this.setCustomValidity('')">
</template>
<template id="newFieldTemplate" style="display:none;">
    <g:render template="/template/newField"/>
</template>

</body>
</html>
<r:script>

$(function() {$('#backupDiv').hide()})

$('#eventStateOnFinishSelect').on('change', function (e) {
    var eventState = $(this).val()
    if(document.getElementById('eventStateOnFinishSelect').selectedIndex != 0) {
        $('#backupDiv').show()
    } else $('#backupDiv').hide()
});


//$("#resetElectionProtocolSimulationDataForm").click()
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

document.getElementById('numRepresentativesWithVote').addEventListener('change', checkRangeRepresentatives, false);
document.getElementById('numRepresentatives').addEventListener('change', checkRangeRepresentatives, false);

document.getElementById('numUsersWithRepresentative').addEventListener('change', checkRangeUsersWithRepresentative, false);
document.getElementById('numUsersWithRepresentativeWithVote').addEventListener('change', checkRangeUsersWithRepresentative, false);


function checkRangeRepresentatives() {
    var representativeWithVote = Number(document.getElementById('numRepresentativesWithVote').value)
    var representatives = Number(document.getElementById('numRepresentatives').value)
    if(isNaN(Number(document.getElementById('numRepresentativesWithVote').value))) {
       document.getElementById('numRepresentativesWithVote').setCustomValidity("DummyInvalid");
       return
    }
    if (representativeWithVote > representatives) {
        document.getElementById('numRepresentativesWithVote').message = "<g:message code='representativeRangeErrorMsg'/>"
        document.getElementById('numRepresentativesWithVote').setCustomValidity("DummyInvalid");
    } else {
        document.getElementById('numRepresentativesWithVote').setCustomValidity("");
    }
}

function checkRangeUsersWithRepresentative () {
    var usersWithRepresentative = Number(document.getElementById('numUsersWithRepresentative').value)
    var usersWithRepresentativeWithVote = Number(document.getElementById('numUsersWithRepresentativeWithVote').value)
    if(isNaN(Number(document.getElementById('numUsersWithRepresentativeWithVote').value))) {
       document.getElementById('numUsersWithRepresentativeWithVote').setCustomValidity("DummyInvalid");
       return
    }
    if (usersWithRepresentativeWithVote > usersWithRepresentative) {
        document.getElementById('numUsersWithRepresentativeWithVote').message = "<g:message code='usersWithRepresentativeRangeErrorMsg'/>"
        document.getElementById('numUsersWithRepresentativeWithVote').setCustomValidity("DummyInvalid");
    } else {
        document.getElementById('numUsersWithRepresentativeWithVote').setCustomValidity("");
    }
}

function setInvalidMsg(event) {
    if( event.target.message != null) {
        event.target.setCustomValidity(event.target.message);
    }
}

function setInvalidMsg(event) {
    console.log(" --- setInvalidMsg --- ")
    if( event.target.message != null) {
        event.target.setCustomValidity(event.target.message);
    }
}

$("#requestBackup").click(function () {
	if($("#requestBackup").is(':checked')) {
		$('#emailDiv').append($('#emailTemplate').html());
	} else {
		$('#emailDiv').html("");
	}
})

var electionEditorDiv = $("#electionEditorDiv")
dateFinish    = $("#dateFinish")
dateInit    = $("#dateInit")
electionEditorDivButton = $("#addElectionFieldButton");
allFields = $( [] ).add(electionEditorDiv).add(electionEditorDivButton);


$('#electionProtocolSimulationDataForm').submit(function(event){
	event.preventDefault();
 	allFields.removeClass("formFieldError");
 	$(".errorMsgWrapper").fadeOut()
    getEditor_electionEditorDivData()
	if(!isValidForm()) {
		return false
	}

	var dateBeginStr = new Date().format()
	var event = {subject:$('#subject').val(),
	        content:getEditor_electionEditorDivData(),
	        dateBegin:document.getElementById("dateInit").getValidatedDate().format(),
	        dateFinish:document.getElementById("dateFinish").getValidatedDate().format()}

    var electionFields = new Array();
    $("#fields").children().each(function(){
        var electionField = $(this).find('div.newFieldValueDiv');
        var electionFieldTxt = electionField.text();
        if(electionFieldTxt.length > 0) {
            var electionField = {content:electionFieldTxt}
            electionFields.push(electionField)
        }
    });
     event.fieldsEventVS = electionFields

     var userBaseData = {userIndex:$('#firstUserIndex').val(),
        numUsersWithoutRepresentative: $('#numUsersWithoutRepresentative').val(),
        numUsersWithoutRepresentativeWithVote: $('#numUsersWithoutRepresentativeWithVote').val(),
        numRepresentatives: $('#numRepresentatives').val(),
        numRepresentativesWithVote: $('#numRepresentativesWithVote').val(),
        numUsersWithRepresentative: $('#numUsersWithRepresentative').val(),
        numUsersWithRepresentativeWithVote: $('#numUsersWithRepresentativeWithVote').val()
     }

	 var simulationData = {service:"electionSimulationService", status:"INIT_SIMULATION",
	 		 accessControlURL:$('#accessControlURL').val(),
	 		 controlCenterURL:$('#controlCenterURL').val(),
			 maxPendingResponses: $('#maxPendingResponses').val(),
			 dateBeginDocument: document.getElementById("dateInit").getValidatedDate().format(),
			 dateFinishDocument: document.getElementById("dateFinish").getValidatedDate().format(),
			 whenFinishChangeEventStateTo:$( "#eventStateOnFinishSelect option:selected").val(),
			 backupRequestEmail:$('#emailRequestBackup').val(),
			 event:event}

	 simulationData.userBaseData = userBaseData

     showListenerDiv(true)
     showSimulationProgress(simulationData)
	 return false
});

	document.getElementById('numRepresentativesWithVote').addEventListener('change', representativeRangeValidator(), false);

    function representativeRangeValidator () {
		var representativeWithVote = document.getElementById('numRepresentativesWithVote')
		var representatives = document.getElementById('numRepresentatives')
		if (representativeWithVote > representatives) {
			document.getElementById('numRepresentativesWithVote').setCustomValidity("<g:message code='representativeRangeErrorMsg'/>");
		}
	}

function isValidForm() {
	//numRepresentativesMsg"/></label>numRepresentativesWithVote numUsersWithRepresentativeMsg numUsersWithRepresentativeWithVote
    var dateInit = document.getElementById("dateInit").getValidatedDate()
    var dateFinish = document.getElementById("dateFinish").getValidatedDate()

	if(!document.getElementById('accessControlURL').validity.valid) {
		$("#accessControlURL").addClass( "formFieldError" );
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

	if(dateInit == null) {
		showErrorMsg('<g:message code="emptyFieldLbl"/>')
		return false
	}

    if(dateFinish == null) {
		showErrorMsg('<g:message code="emptyFieldLbl"/>')
		return false
	}

	if(dateFinish < dateInit) {
        showErrorMsg('<g:message code="dateFinishBeforeTodayERRORMsg"/>')
		return false
	}


	if('' == getEditor_electionEditorDivData()) {
		showErrorMsg('<g:message code="eventContentEmptyERRORMsg"/>')
		electionEditorDiv.addClass("formFieldError");
		return false
	}

	var numOptions = 0;
    $("#fields").children().each(function(){
        var electionField = $(this).find('div.newFieldValueDiv');
        var electionFieldTxt = electionField.text();
        if(electionFieldTxt.length > 0) {
            numOptions++
        }
    });
	if(!(numOptions > 1)) {
        showErrorMsg('<g:message code="missingOptionsERRORMsg"/>')
        electionEditorDivButton.addClass( "formFieldError" );
		return false
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
        $('#pageTitle').text('<g:message code="listeningElectionProtocolSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initElectionProtocolSimulationMsg"/>')
    }
}


$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});



$("#addElectionFieldButton").click(function () {
    getEditor_electionEditorDivData()
    showAddVoteOptionDialog(addElectionField)
});

var numElectionFields = 0

function addElectionField (electionFieldText) {
    if(electionFieldText == null) return
    var newFieldTemplate = $('#newFieldTemplate').html()
    var newFieldHTML = newFieldTemplate.format(electionFieldText);
    var $newField = $(newFieldHTML)
    $newField.find('#deleteFieldButton').click(function() {
            $(this).parent().fadeOut(1000,
            function() { $(this).parent().remove(); });
            numElectionFields--
            if(numElectionFields == 0) {
                $("#fieldsBox").fadeOut(1000)
            }
        }
    )
    $("#fieldsBox #fields").append($newField)
    if(numElectionFields == 0) {
        $("#fieldsBox").fadeIn(1000)
    }
    self.scrollbars = true;
    numElectionFields++
    $("#electionFieldText").val("");
}

function showErrorMsg(errorMsg) {
	$("#electionProtocolSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#electionProtocolSimulationDataDialog .errorMsgWrapper").fadeIn()
}

</r:script>