<!DOCTYPE html>
<html>
<head>
    <title><g:message code="claimProtocolSimulationCaption"/></title>
    <meta name="layout" content="main" />
    <r:require modules="textEditorPC"/>
</head>
<body style="overflow-y: scroll;">
<div class="row">
    <ol class="breadcrumbVS pull-left">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'simulation', action:'votingSystem', absolute:true)}">
            <g:message code="votingSystemOperationsLbl"/></a></li>
        <li class="active"><g:message code="initClaimProtocolSimulationButton"/></li>
    </ol>
</div>
<div id="claimProtocolSimulationDataDialog" title="<g:message code="initClaimProtocolSimulationButton"/>"
     style="padding:10px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
    <div class="errorMsgWrapper" style="display:none;"></div>
    <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
        <div id="pageTitle" class="pageHeader">
            <g:message code="initClaimProtocolSimulationMsg"/>
        </div>
        <div  id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
            <button id="testButton" type="button" class="btn btn-default" style="margin:0px 0px 0px 30px;">
                <g:message code="goToResultViewMsg"/>
            </button>
        </div>
    </div>
    <div id="formDataDiv">
        <form id="claimProtocolSimulationDataForm" onsubmit="return submitForm(this);">
            <input type="hidden" autofocus="autofocus" />
            <input id="resetClaimProtocolSimulationDataForm" type="reset" style="display:none;">

            <div style="display: block;">
                <label><g:message code="numRequestsProjectedLbl"/></label>
                <input type="number" id="numRequestsProjected" min="1" value="1" required
                       style="width:130px;margin:0px 20px 0px 3px;"
                       title="<g:message code="numRequestsProjectedLbl"/>"
                       placeholder="<g:message code="numRequestsProjectedLbl"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')">
                <label><g:message code="maxPendingResponsesLbl"/></label>
                <input type="number" id="maxPendingResponses" min="1" value="10" required
                       style="width:130px;margin:10px 20px 0px 3px;"
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
                <label>${message(code:'dateFinishLbl')}</label>
                <votingSystem:datePicker id="dateFinish" title="${message(code:'dateFinishLbl')}"
                                         oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
                                         onchange="this.setCustomValidity('')"></votingSystem:datePicker>
            </div>

            <div style="margin:10px 0px 10px 0px">
                <input type="text" name="subject" id="subject" style="width:350px"  required
                       title="<g:message code="subjectLbl"/>"
                       placeholder="<g:message code="subjectLbl"/>"/>
                <input type="url" id="accessControlURL" style="width:300px; margin:0px 0px 0px 20px;" required
                       value="http://sistemavotacion.org/AccessControl"
                       title="<g:message code="accessControlURLMsg"/>"
                       placeholder="<g:message code="accessControlURLMsg"/>"
                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                       onchange="this.setCustomValidity('')"/>
            </div>

            <votingSystem:textEditor id="claimEditorDiv" style="height:300px;"/>

            <div id="backupDiv" style="margin:10px 0px 10px 10px; overflow: hidden; height: 50px; display: table;">
                <div class="checkBox" style="display:table-cell;vertical-align: middle;">
                    <input type="checkbox" id="requestBackup"/><label for="requestBackup"><g:message code="requestBackupLbl"/></label>
                </div>
                <div id="emailDiv" style="display:table-cell;vertical-align: middle;"></div>
            </div>

            <div style="margin:0px 0px 0px 0px; overflow: hidden;width: 100%;">
                <button id="addClaimFieldButton" type="button" class="btn btn-default" style="margin:15px 20px 20px 0px; float:right;">
                    <g:message code="addClaimFieldLbl"/>
                </button>
            </div>

            <fieldset id="fieldsBox" class="fieldsBox" style="display:none;">
                <legend id="fieldsLegend"><g:message code="claimsFieldLegend"/></legend>
                <div id="fields"></div>
            </fieldset>
            <div style="position: relative; overflow:hidden; ">
                <button id="submitButton" type="submit" class="btn btn-default" style="margin:15px 20px 20px 0px; width:450px; float:right;">
                    <g:message code="initClaimProtocolSimulationButton"/>
                </button>
            </div>
        </form>
    </div>
</div>

<div id="simulationListenerDiv" style="display: none;">
    <g:include view="/include/listenSimulation.gsp"/>
</div>

<g:include view="/include/dialog/addClaimFieldDialog.gsp"/>
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

$("#requestBackup").click(function () {
	if($("#requestBackup").is(':checked')) {
		$('#emailDiv').append($('#emailTemplate').html());
	} else {
		$('#emailDiv').html("");
	}
})

var claimEditorDiv = $("#claimEditorDiv")
allFields = $( [] ).add(claimEditorDiv);

function submitForm(form) {
    if(!isValidForm()) return false

	var dateBeginStr = new Date().format()
	var event = {subject:$('#subject').val(),
	        content:getEditor_claimEditorDivData(),
	        dateBegin:dateBeginStr,
	        dateFinish:document.getElementById("dateFinish").getValidatedDate().format()}

    var claimFields = new Array();
    $("#fields").children().each(function(){
        var claimField = $(this).find('div.newFieldValueDiv');
        var claimFieldTxt = claimField.text();
        if(claimFieldTxt.length > 0) {
            var claimField = {content:claimFieldTxt}
            claimFields.push(claimField)
        }
    });
     event.fieldsEventVS = claimFields
	 var simulationData = {service:"claimSimulationService", status:Status.INIT_SIMULATION,
	 		 accessControlURL:$('#accessControlURL').val(),
			 maxPendingResponses: $('#maxPendingResponses').val(),
			 numRequestsProjected: $('#numRequestsProjected').val(),
			 dateBeginDocument: dateBeginStr,
			 dateFinishDocument: document.getElementById("dateFinish").getValidatedDate().format(),
			 whenFinishChangeEventStateTo:$( "#eventStateOnFinishSelect option:selected").val(),
			 backupRequestEmail:$('#emailRequestBackup').val(),
			 event:event}

     showListenerDiv(true)
     showSimulationProgress(simulationData)
     return false
}


function isValidForm() {
 	allFields.removeClass("formFieldError");
 	$(".errorMsgWrapper").fadeOut()

	if(!document.getElementById('accessControlURL').validity.valid) {
		$("#accessControlURL").addClass("formFieldError");
		showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="emptyFieldLbl"/>', function() {
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

	if(document.getElementById("dateFinish").getValidatedDate() == null) {
		showErrorMsg('<g:message code="emptyFieldLbl"/>')
		return false
	}

	if(document.getElementById("dateFinish").getValidatedDate() < new Date()) {
		showErrorMsg('<g:message code="dateFinishBeforeTodayERRORMsg"/>')
		return false
	}

	if('' == getEditor_claimEditorDivData()) {
		showErrorMsg('<g:message code="eventContentEmptyERRORMsg"/>') 
		claimEditorDiv.addClass("formFieldError");
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
        $('#pageTitle').text('<g:message code="listeningClaimProtocolSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initClaimProtocolSimulationMsg"/>')
    }
}


$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});


$("#addClaimFieldButton").click(function () {
    getEditor_claimEditorDivData()
    showAddClaimFieldDialog(addClaimField)
});

var numClaimFields = 0

function addClaimField (claimFieldText) {
    if(claimFieldText == null) return
    var newFieldTemplate = $('#newFieldTemplate').html()
    var newFieldHTML = newFieldTemplate.format(claimFieldText);
    var $newField = $(newFieldHTML)
    $newField.find('#deleteFieldButton').click(function() {
            $(this).parent().fadeOut(1000,
            function() { $(this).parent().remove(); });
            numClaimFields--
            if(numClaimFields == 0) {
                $("#fieldsBox").fadeOut(1000)
            }
        }
    )
    $("#fieldsBox #fields").append($newField)
    if(numClaimFields == 0) {
        $("#fieldsBox").fadeIn(1000)
    }
    self.scrollbars = true;
    numClaimFields++
    $("#claimFieldText").val("");
}

function showErrorMsg(errorMsg) {
	$("#claimProtocolSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#claimProtocolSimulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>