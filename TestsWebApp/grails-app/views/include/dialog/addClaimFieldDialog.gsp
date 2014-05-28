<div id="addClaimFieldDialog" title="<g:message code="addClaimFieldLbl"/>" style="display:none;">
	<p style="text-align: center;">
		<g:message code="claimFieldDescriptionMsg"/>
	</p>
	<div style="margin:20px 0px 0px 0px;"><g:message code="addClaimFieldMsg"/></div>
	<form id="newFieldClaimForm">
			<input type="text" id="claimFieldText" style="width:400px" 
				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
				onchange="this.setCustomValidity('')"
				class="text ui-widget-content ui-corner-all" required/>
			<input id="submitClaimFieldText" type="submit" style="display:none;">
	</form>
</div> 
<asset:script>

var callerCallback

function showAddClaimFieldDialog(callback) {
	$("#addClaimFieldDialog").dialog("open");
	callerCallback = callback	
}

$('#newFieldClaimForm').submit(function(event){	
	event.preventDefault();
	$("#addClaimFieldDialog").dialog( "close" );
 		if(!document.getElementById('claimFieldText').validity.valid) {
 			$("#claimFieldText").addClass( "formFieldError" );
 			showResultDialog('<g:message code="dataFormERRORLbl"/>', 
 				'<g:message code="emptyFieldLbl"/>',function() {
 					$("#addClaimFieldDialog").dialog("open")
     			})
 		} else {
     		if(callerCallback != null) callerCallback($("#claimFieldText").val())
     		else console.log("addClaimFieldDialog - missing callerCallback")
 			$("#claimFieldText").removeClass( "formFieldError" );
 		}
})

$("#addClaimFieldDialog").dialog({
   	  width: 450, autoOpen: false, modal: true,
      buttons: [{text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		$("#submitClaimFieldText").click()    			   				
	        	}},{
        		text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
             		if(callerCallback != null) callerCallback(null)
             		else console.log("addClaimFieldDialog - missing callerCallback")
	   				$(this).dialog( "close" );
	       	 	}	
           }
       ],
      show: { effect: "fade", duration: 500 },
      hide: { effect: "fade", duration: 500 },
      open: function( event, ui ) {
	 		$("#claimFieldText").val("")
		  }
    });
</asset:script>