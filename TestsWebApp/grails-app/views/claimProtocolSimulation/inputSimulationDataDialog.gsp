	<head>
		<meta name="layout" content="main"/>
			<r:require modules="application"/>
		<title><g:message code="manifestProtocolSimulationCaption"/></title>
		<style type="text/css" media="screen"></style>
	</head>
<div id="claimProtocolSinulationDataDialog" title="<g:message code="initClaimProtocolSimulationButton"/>">
	<div class="errorMsgWrapper" style="display:none; margin:"></div>
	<p style="text-align: center; margin:10px 0px 10px 0px; font-weight: bold;">
		<g:message code="initClaimProtocolSimulationMsg"/>
  	</p>
  	<div>
   		<form id="claimProtocolSinulationDataForm">
			<input type="hidden" autofocus="autofocus" />
			<input id="resetClaimProtocolSinulationDataForm" type="reset" style="display:none;">
   			<input type="url" id="claimsAccessControlURL" style="width:500px; margin:10px auto 0px auto;" required
				title="<g:message code="accessControlURLMsg"/>"
				placeholder="<g:message code="accessControlURLMsg"/>"
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')"/>
  				<input id="submitClaimProtocolSinulationDataForm" type="submit" style="display:none;">
  				
 				<div>
 					<input type="number" id="maxPendingClaimResponses" min="1" value="10" required
 						style="width:300px;margin:10px 0px 0px 0px;"
 						title="<g:message code="maxPendingResponsesMsg"/>"
						placeholder="<g:message code="maxPendingResponses"/>"
	   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
	   					onchange="this.setCustomValidity('')">
	   					
					<input type="number" id="numClaimRequestsProjected" min="1" value="1" required
 						style="width:300px;margin:10px 0px 0px 10px;" 
 						title="<g:message code="numRequestsProjected"/>"
						placeholder="<g:message code="numRequestsProjected"/>"
	   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
	   					onchange="this.setCustomValidity('')">
 				</div>

				<div style="margin:10px auto 0px auto; width:100%; height:30px; position: relative; overflow: hidden;">		
					<select id="claimStateOnFinishSelect" style="rigth:15%; left:15%; width:60%;position: absolute;"
							title="<g:message code="setEventStateLbl"/>">
					  	<option value=""> - <g:message code="eventAsDateRangeLbl"/> - </option>
					  	<option value="CANCELADO" style="color:#cc1606;"> - <g:message code="eventCancelledLbl"/> - </option>
					  	<option value="DELETED" style="color:#cc1606;"> - <g:message code="eventDeletedLbl"/> - </option>
					</select>
				</div>
				
				<div style="margin:10px 0px 10px 0px">
		    		<input type="text" name="subject" id="claimSubject" style="width:350px"  required
						title="<g:message code="subjectLbl"/>"
						placeholder="<g:message code="subjectLbl"/>"/>
					<votingSystem:datePicker id="dateFinishClaim" title="${message(code:'dateFinishLbl')}"
						placeholder="${message(code:'dateFinishLbl')}"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
						

				</div>
				
				<votingSystem:textEditorPC id="claimEditorDiv" style="height:300px;"/>
				
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

var claimEditorDiv = $("#claimEditorDiv")
dateFinishClaim    = $("#dateFinishClaim")
allFields = $( [] ).add(dateFinishClaim).add(claimEditorDiv);

var callerCallback


$("#claimProtocolSinulationDataDialog").dialog({
   	  width: 700, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="initSimulationLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
 			   				$("#submitClaimProtocolSinulationDataForm").click() 	   	   			   				
 			        	}},
           {text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
	   				$(this).dialog( "close" );
	   				hideEditor_claimEditorDiv()
	   				if(callerCallback != null) callerCallback()
       	 	}}],
      show: {effect:"fade", duration: 700},
      hide: {effect: "fade",duration: 700},
      open: function( event, ui ) {
      	  console.log("opening claimProtocolSinulationDataDialog") 
      	  //$("#resetClaimProtocolSinulationDataForm").click() 	   	  
    	  showEditor_claimEditorDiv()
	  }
    });


function showClaimProtocolSinulationDataDialog(callback) {
	$("#claimProtocolSinulationDataDialog").dialog("open");
	callerCallback = callback	
}

$('#claimProtocolSinulationDataForm').submit(function(event){
	event.preventDefault();	  


 	allFields.removeClass("ui-state-error");   
 	$(".errorMsgWrapper").fadeOut() 
	hideEditor_claimEditorDiv() 
	if(!isValidClaimForm()) {
		showEditor_claimEditorDiv()
		return false
	}

	var dateBeginStr = new Date().format()
	var event = {asunto:$('#claimSubject').val(),
	        contenido:claimEditorDivContent.trim(),
	        fechaInicio:dateBeginStr,
	        fechaFin:dateFinishClaim.datepicker("getDate").format()}
	
	 var simulationData = {service:"claimSimulationService",
	 		 operation:Operation.CLAIM_PROTOCOL_SIMULATION,
	 		 accessControlURL:$('#claimsAccessControlURL').val(), 
			 maxPendingResponses: $('#maxPendingClaimResponses').val(), 
			 numRequestsProjected: $('#numClaimRequestsProjected').val(),
			 dateBeginDocument: dateBeginStr, 
			 dateFinishDocument: dateFinishClaim.datepicker("getDate").format(),
			 whenFinishChangeEventStateTo:$( "#claimStateOnFinishSelect option:selected").val(), 
			 backupRequestEmail:$('#emailRequestBackup').val(), 
			 event:event}
		
	showSimulationRunningDialog(simulationData)	
	
	//$("#claimProtocolSinulationDataDialog").dialog("close");
	
	return false
});

function isValidClaimForm() {
	if(!document.getElementById('claimsAccessControlURL').validity.valid) {
		$("#claimsAccessControlURL").addClass( "ui-state-error" );
		showResultDialog('<g:message code="dataFormERRORLbl"/>', 
			'<g:message code="emptyFieldMsg"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}	      
	var claimsAccessControlURL = $('#claimsAccessControlURL').val()
	var suffix = "/"
	if((claimsAccessControlURL.indexOf(suffix, claimsAccessControlURL.length - suffix.length) == -1)) {
		claimsAccessControlURL = claimsAccessControlURL + "/"
	}
	claimsAccessControlURL = claimsAccessControlURL + "infoServidor"
	if(claimsAccessControlURL.indexOf("http://") != 0) {
		claimsAccessControlURL = "http://" + claimsAccessControlURL
	}

	if(dateFinishClaim.datepicker("getDate") === null) {
		dateFinishClaim.addClass( "ui-state-error" );
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return false
	}

	if(dateFinishClaim.datepicker("getDate") < new Date()) {
		showErrorMsg('<g:message code="dateFinishBeforeTodayERRORMsg"/>') 
		dateFinishClaim.addClass("ui-state-error");
		return false
	}

	if('' == claimEditorDivContent.trim()) {
		showErrorMsg('<g:message code="eventContentEmptyERRORMsg"/>') 
		claimEditorDiv.addClass("ui-state-error");
		return false
	}
	return true
}

function showErrorMsg(errorMsg) {
	$("#claimProtocolSinulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#claimProtocolSinulationDataDialog .errorMsgWrapper").fadeIn()
}
	
</r:script>