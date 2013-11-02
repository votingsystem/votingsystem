<div id="editRepresentativeDialog" title="<g:message code="editRepresentativeLbl"/>" style="margin:20px auto 20px auto;">	
	<div id="editRepresentativeDialogFormDiv" style="margin:0px auto 0px 20px;">
		<form id="editRepresentativeForm">
	    	<label for="userNifText" style="margin:0px 0px 20px 0px"><g:message code="nifForEditRepresentativeLbl"/></label>
			<input type="text" id="representativeNifText" style="width:350px; margin:0px auto 0px auto;" required
				oninvalid="this.setCustomValidity('<g:message code="nifERRORMsg"/>')"
	   			onchange="this.setCustomValidity('')"/>
			<input id="submitNifCheck" type="submit" style="display:none;">
		</form>
	</div>
	<div id="editRepresentativeDialogProgressDiv" style="display:none;">
		<p style='text-align: center;'><g:message code="checkingDataLbl"/></p>
		<progress style='display:block;margin:0px auto 10px auto;'></progress>
	</div>
</div>
<script>

$("#editRepresentativeDialog").dialog({
   	  width: 450, autoOpen: false, modal: true,
      buttons: [{id: "acceptButton",
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		$("#submitNifCheck").click() 
             		//$(this).dialog( "close" );   	   			   				
		        	}}, {id: "cancelButton",
        		text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
		   					$(this).dialog( "close" );
		       	 		}	
           }],
      show: {effect:"fade", duration: 300},
      hide: {effect: "fade",duration: 300},
      open: function( event, ui ) {
    	  $("#userNifText").val("");
          $("#editRepresentativeDialogFormDiv").show()
          $("#editRepresentativeDialogProgressDiv").hide()
          $("#acceptButton").button("enable");
          $("#cancelButton").button("enable"); 
	  }
});

var nifValidation = function () {
    var nifInput = document.getElementById('representativeNifText')
    var validationResult = validateNIF(nifInput.value)
    console.log("validateNIF result: " + validationResult)
    if (!validationResult) {
            document.getElementById('userNifText').setCustomValidity("<g:message code='nifERRORMsg'/>");
    }
}

   $('#editRepresentativeForm').submit(function(event){	
		console.log("editRepresentativeForm")
		event.preventDefault();
		$("#acceptButton").button("disable");
		$("#cancelButton").button("disable");
		$("#editRepresentativeDialogFormDiv").hide()
		$("#editRepresentativeDialogProgressDiv").fadeIn(500)
		var urlRequest = "${createLink(controller:'representative')}/edit/" + $("#userNifText").val()
		console.log(" - editRepresentative - urlRequest: " + urlRequest)
		$.ajax({///user/$nif/representative
			contentType:'application/json',
			url: urlRequest
		}).done(function(resultMsg) {
			window.location.href = urlRequest
		}).error(function(resultMsg) {
			showResultDialog('<g:message code="errorLbl"/>',resultMsg.responseText) 							
		}).always(function(resultMsg) {
			$("#editRepresentativeDialog").dialog("close");
		});

	 })

function editRepresentativeCallback(appMessage) {
	console.log("editRepresentativeCallback - message from native client: " + appMessage);
	var appMessageJSON = toJSON(appMessage)
	if(appMessageJSON != null) {
		$("#workingWithAppletDialog" ).dialog("close");
		var caption = '<g:message code="operationERRORCaption"/>'
		var msg = appMessageJSON.mensaje
		if(StatusCode.SC_OK == appMessageJSON.codigoEstado) { 
			caption = "<g:message code='operationOKCaption'/>"
		} else if (StatusCode.SC_CANCELADO== appMessageJSON.codigoEstado) {
			caption = "<g:message code='operationCANCELLEDLbl'/>"
		}
		showResultDialog(caption, msg)
	}
}

document.getElementById('representativeNifText').addEventListener('change', nifValidation, false);
</script>