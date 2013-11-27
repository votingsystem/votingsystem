<div id="checkRepresentativeDialog" title="<g:message code="checkRepresentativeLbl"/>" style="display:none;">	
	<div id="checkRepresentativeDialogFormDiv" >
		<p style="text-align: center;"><g:message code="checkRepresentativeMsg"/></p>
		<div style="width: 80%; margin: 0 auto;">
			<form id="checkRepresentativeForm">
		    	<label for="userNifText" style="margin:0px 0px 20px 0px"><g:message code="enterNIFMsg"/></label>
				<input type="text" id="userNifText" style="width:350px; margin:0px auto 0px auto;" required
					oninvalid="this.setCustomValidity('<g:message code="nifERRORMsg"/>')"
		   			onchange="this.setCustomValidity('')"/>
				<input id="submitNifCheck" type="submit" style="display:none;">
			</form>
		</div>
	</div>  	
	<div id="checkRepresentativeDialogProgressDiv" style="display:none;">
		<p style='text-align: center;'><g:message code="checkingDataLbl"/></p>
		<progress style='display:block;margin:0px auto 10px auto;'></progress>
	</div>
	<div id="checkRepresentativeDialogResultDiv" style="display:none;">
		<div style='display:table; width:100%;'>
			<div style='display:table-cell; vertical-align:middle;'><img id="checkRepresentativeDialogResultImage" src='' style='margin:3px 0 0 10px;'></img></div>
			<div style='display:table-cell;width:15px;'></div>
			<div style='display:table-cell; vertical-align:middle;'>
				<p id="checkRepresentativeDialogResultMsg" style="margin: 0px 0px 0px 0px; text-align:center;"></p>
			</div>
		</div>
	</div>
</div>
<r:script>
   $('#checkRepresentativeForm').submit(function(event){	
		console.log("checkRepresentativeForm")
		event.preventDefault();
		$("#acceptButton").button("disable");
		$("#cancelButton").button("disable");
		checkRepresentative();
 })

 	var nifValidation = function () {
		var nifInput = document.getElementById('userNifText')
		var validationResult = validateNIF(nifInput.value)
		console.log("validateNIF result: " + validationResult)
		if (!validationResult) {
			document.getElementById('userNifText').setCustomValidity("<g:message code='nifERRORMsg'/>");
		}
	}
	
$("#checkRepresentativeDialog").dialog({
	   	  width: 500, autoOpen: false, modal: true,
		      buttons: [{id: "acceptButton",
		        		text:"<g:message code="acceptLbl"/>",
		               	icons: { primary: "ui-icon-check"},
		             	click:function() {
		             		$("#submitNifCheck").click() 
		             		//$(this).dialog( "close" );   	   			   				
  			        	}}, {	
 			        	id: "cancelButton",
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
				 		$("#checkRepresentativeDialogFormDiv").show()
						$("#checkRepresentativeDialogProgressDiv").hide()
						$("#checkRepresentativeDialogResultDiv").hide()
						$("#acceptButton").show();
				 		$("#acceptButton").button("enable");
				 		$("#cancelButton").button("enable");
			 			$("#cancelButton").find(".ui-button-text").text("<g:message code="cancelLbl"/>")
				  }
		    });

	function checkRepresentative () { 
 		$("#checkRepresentativeDialogFormDiv").hide()
 		$("#checkRepresentativeDialogProgressDiv").fadeIn(500)
 		var urlRequest = "${createLink(controller:'userVS')}/" + $("#userNifText").val() + "/representative"
 		console.log(" - checkRepresentative - urlRequest: " + urlRequest)
		$.ajax({///user/$nif/representative
			url: urlRequest
			//data: data,
		}).done(function(resultMsg) {
			var resultMsgStr = JSON.stringify(resultMsg);  
			console.log(" - ajax call done - resultMsgStr: " + resultMsgStr);
			var msgTemplate = "<g:message code="representativeAssociatedCheckedMsg"/>"
			var msg = msgTemplate.format(resultMsg.representativeName, $("#userNifText").val())
			showCheckRepresentativeResult (ResponseVS.SC_OK, msg)
		}).error(function(resultMsg) {
			showCheckRepresentativeResult (ResponseVS.SC_ERROR, resultMsg.responseText)
		});
	}

 	function showCheckRepresentativeResult (statusCode, msg) { 
 		console.log("showCheckRepresentativeResult - statusCode: " + statusCode + " - msg: " + msg)
 		$("#checkRepresentativeDialogProgressDiv").hide()
 		$("#checkRepresentativeDialogResultDiv").fadeIn(500)
 		$("#acceptButton").fadeOut();
 		$("#cancelButton").find(".ui-button-text").text("<g:message code="acceptLbl"/>")
 		$("#cancelButton").button("enable");
 		if(ResponseVS.SC_OK == statusCode) {
 			$("#checkRepresentativeDialogResultImage").attr('src',"${resource(dir:'images', file:'accept_48x48.png')}");
	 	} else {
	 		$("#checkRepresentativeDialogResultImage").attr('src',"${resource(dir:'images', file:'advert_64x64.png')}")
		}
 		$("#checkRepresentativeDialogResultMsg").text(msg)
	}

 	document.getElementById('userNifText').addEventListener('change', nifValidation, false);

</r:script>