	<head>
		<meta name="layout" content="main"/>
			<r:require modules="application"/>
		<title><g:message code="manifestProtocolSimulationCaption"/></title>
		<style type="text/css" media="screen"></style>
	</head>
<div id="manifestProtocolSinulationDataDialog" title="<g:message code="initManifestProtocolSimulationButton"/>">
	<div class="errorMsgWrapper" style="display:none; margin:"></div>
	<p style="text-align: center; margin:10px 0px 10px 0px; font-weight: bold;">
		<g:message code="initManifestProtocolSimulationMsg"/>
  	</p>
  	<div>
   		<form id="manifestProtocolSinulationDataForm">
			<input type="hidden" autofocus="autofocus" />
			<input id="resetManifestProtocolSinulationDataForm" type="reset" style="display:none;">
   			<input type="url" id="accessControlURL" style="width:500px; margin:10px auto 0px auto;" required
				title="<g:message code="accessControlURLMsg"/>"
				placeholder="<g:message code="accessControlURLMsg"/>"
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"/>
  				<input id="submitManifestProtocolSinulationDataForm" type="submit" style="display:none;">
  				
 				<div>
 					<input type="number" id="maxPendingResponses" min="1" value="10" required
 						style="width:300px;margin:10px 0px 0px 0px;"
 						title="<g:message code="maxPendingResponsesMsg"/>"
						placeholder="<g:message code="maxPendingResponses"/>"
	   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
	   					onchange="this.setCustomValidity('')">
	   					
					<input type="number" id="numRequestsProjected" min="1" value="1" required
 						style="width:300px;margin:10px 0px 0px 10px;" 
 						title="<g:message code="numRequestsProjected"/>"
						placeholder="<g:message code="numRequestsProjected"/>"
	   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
	   					onchange="this.setCustomValidity('')">
 				</div>

				<div style="margin:10px auto 0px auto; width:100%; height:30px; position: relative; overflow: hidden;">		
					<select id="eventStateOnFinishSelect" style="rigth:15%; left:15%; width:60%;position: absolute;"
							title="<g:message code="setEventStateLbl"/>">
					  	<option value=""> - <g:message code="eventAsDateRangeLbl"/> - </option>
					  	<option value="CANCELADO" style="color:#cc1606;"> - <g:message code="eventCancelledLbl"/> - </option>
					  	<option value="DELETED" style="color:#cc1606;"> - <g:message code="eventDeletedLbl"/> - </option>
					</select>
				</div>
				
				<div style="margin:10px 0px 10px 0px">
		    		<input type="text" name="subject" id="subject" style="width:350px"  required
						title="<g:message code="subjectLbl"/>"
						placeholder="<g:message code="subjectLbl"/>"/>
					<votingSystem:datePicker id="dateFinish" title="${message(code:'dateFinishLbl')}"
						placeholder="${message(code:'dateFinishLbl')}"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
						

				</div>
				
				<votingSystem:textEditorPC id="manifestEditorDiv"/>
				
				<div id="backupDiv" style="margin:10px 0px 10px 10px; overflow: hidden; height: 50px; display: table;">
					<div class="checkBox" style="display:table-cell;vertical-align: middle;">
						<input type="checkbox" id="requestBackup"/><label for="requestBackup"><g:message code="requestBackupLbl"/></label>
					</div>
					<div id="emailDiv" style="display:table-cell;vertical-align: middle;">

					</div>
				</div>
				
   		</form>

  	</div>
</div>
<script type="text/ng-template" id="emailTemplate">
	<input type="email" id="emailRequestBackup" required
	 		title="<g:message code="emailRequestBackupMsg"/>"
			placeholder="<g:message code="emailLbl"/>"
		   	oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   	onchange="this.setCustomValidity('')">
</script> 
<r:script>

$("#requestBackup").click(function () {
	if($("#requestBackup").is(':checked')) {
		$('#emailDiv').append($('#emailTemplate').html());
	} else {
		$('#emailDiv').html("");
	}
})

var manifestEditorDiv = $("#manifestEditorDiv")
dateFinish    = $("#dateFinish")
allFields = $( [] ).add(dateFinish).add(manifestEditorDiv);

var callerCallback


$("#manifestProtocolSinulationDataDialog").dialog({
   	  width: 700, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="initSimulationLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
 			   				$("#submitManifestProtocolSinulationDataForm").click() 	   	   			   				
 			        	}},
           {text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
	   				$(this).dialog( "close" );
	   				hideEditor_manifestEditorDiv()
	   				if(callerCallback != null) callerCallback()
       	 	}}],
      show: {effect:"fade", duration: 700},
      hide: {effect: "fade",duration: 700},
      open: function( event, ui ) {
      	  console.log("opening manifestProtocolSinulationDataDialog") 
      	  //$("#resetManifestProtocolSinulationDataForm").click() 	   	  
    	  showEditor_manifestEditorDiv()
	  }
    });


function showManifestProtocolSinulationDataDialog(callback) {
	$("#manifestProtocolSinulationDataDialog").dialog("open");
	callerCallback = callback	
}

$('#manifestProtocolSinulationDataForm').submit(function(event){
	event.preventDefault();	  


 	allFields.removeClass("ui-state-error");   
 	$(".errorMsgWrapper").fadeOut() 
	hideEditor_manifestEditorDiv() 
	if(!isValidForm()) {
		showEditor_manifestEditorDiv()
		return false
	}

	var dateBeginStr = new Date().format()
	var event = {asunto:$('#subject').val(),
	        contenido:manifestEditorDivContent.trim(),
	        fechaInicio:dateBeginStr,
	        fechaFin:dateFinish.datepicker("getDate").format()}
	
	 var simulationData = {service:"manifestSimulationService",
	 		 operation:Operation.MANIFEST_PROTOCOL_SIMULATION,
	 		 accessControlURL:$('#accessControlURL').val(), 
			 maxPendingResponses: $('#maxPendingResponses').val(), 
			 numRequestsProjected: $('#numRequestsProjected').val(),
			 dateBeginDocument: dateBeginStr, 
			 dateFinishDocument: dateFinish.datepicker("getDate").format(),
			 whenFinishChangeEventStateTo:$( "#eventStateOnFinishSelect option:selected").val(), 
			 backupRequestEmail:$('#emailRequestBackup').val(), 
			 event:event}
		
	showSimulationRunningDialog(simulationData)	
	
	//$("#manifestProtocolSinulationDataDialog").dialog("close");
	
	return false
});

function isValidForm() {
	if(!document.getElementById('accessControlURL').validity.valid) {
		$("#accessControlURL").addClass( "ui-state-error" );
		showResultDialog('<g:message code="dataFormERRORLbl"/>', 
			'<g:message code="emptyFieldMsg"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}	      
	var accessControlURL = $('#accessControlURL').val()
	var suffix = "/"
	if((accessControlURL.indexOf(suffix, accessControlURL.length - suffix.length) == -1)) {
		accessControlURL = accessControlURL + "/"
	}
	accessControlURL = accessControlURL + "infoServidor"
	if(accessControlURL.indexOf("http://") != 0) {
		accessControlURL = "http://" + accessControlURL
	}

	if(dateFinish.datepicker("getDate") === null) {
		dateFinish.addClass( "ui-state-error" );
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return false
	}

	if(dateFinish.datepicker("getDate") < new Date()) {
		showErrorMsg('<g:message code="dateFinishBeforeTodayERRORMsg"/>') 
		dateFinish.addClass("ui-state-error");
		return false
	}

	if('' == manifestEditorDivContent.trim()) {
		showErrorMsg('<g:message code="eventContentEmptyERRORMsg"/>') 
		manifestEditorDiv.addClass("ui-state-error");
		return false
	}
	return true
}

function showErrorMsg(errorMsg) {
	$("#manifestProtocolSinulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#manifestProtocolSinulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>