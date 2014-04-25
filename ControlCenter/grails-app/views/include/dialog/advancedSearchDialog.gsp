<div id="advancedSearchDialog" title="<g:message code="advancedSearchLbl"/>" style="display:none;" class="container">
	<div id="advancedSearchDialogErrorPanel" class="bg-danger text-center"
         style="display:none; padding: 10px; color:#870000;"></div>
	<p style="text-align: center; margin:10px 0 0 0;"><g:message code="advancedSearchMsg"/>.</p>
  		<form id="advancedSearchForm">
        <input id="resetAdvancedSearchForm" type="reset" style="display:none;">
		<input type="hidden" autofocus="autofocus" />
		<div style="margin:20px auto 0px auto;" class="text-center">
  				<input type="text" id="advancedSearchText" style="" required
  					title="<g:message code="advancedSearchFieldLbl"/>"
  					placeholder="<g:message code="advancedSearchFieldLbl"/>"
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')" />
 				</div>

				<div style="display:block;margin:20px 0px 0px 0px;">
 				<div id="dateBeginFromDiv_ASD" style="display:inline-block;margin:0px 0px 0px 20px;">
                    <label>${message(code:'dateBeginFromLbl')}</label>
					<votingSystem:datePicker id="dateBeginFrom_ASD" title="${message(code:'dateBeginFromLbl')}" required="true"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
 				</div>
 				<div id="dateBeginToDiv_ASD" style="display:inline-block;margin:0px 0px 0px 20px;">
                    <label>${message(code:'dateToLbl')}</label>
					<votingSystem:datePicker id="dateBeginTo_ASD" title="${message(code:'dateToLbl')}" required="true"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
  				</div>
 				</div>

 				<input id="submitSearch" type="submit" style="display:none;">
  		</form>
  	</div> 
<r:script>

 $("#advancedSearchDialog").dialog({
		width: 'auto', autoOpen: false, modal: true,
	      buttons: [{text:"<g:message code="acceptLbl"/>",
			               	icons: { primary: "ui-icon-check"},
			             	click:function() {
	   	   			   				$("#submitSearch").click() 	   	   			   				
	   	   			        	} },{text:"<g:message code="cancelLbl"/>",
			               	icons: { primary: "ui-icon-closethick"},
			             	click:function() {
	   			   				$(this).dialog("close");
	   			       	 	}}],
	      show: {effect: "fade",duration: 300},
	      hide: {effect: "fade",duration: 300},
	      open: function( event, ui ) { resetAdvancedSearchDialogForm() }
	    });

function resetAdvancedSearchDialogForm() {
     $("#resetAdvancedSearchForm").click()
     $("#dateBeginFromDiv_ASD").removeClass("has-error");
     $("#dateBeginToDiv_ASD").removeClass("has-error");
}

 $('#advancedSearchForm').submit(function(event){
 	console.log("advancedSearchForm")
 	event.preventDefault();
 	$(".errorMsgWrapper").fadeOut()
 	var dateBeginFrom = document.getElementById("dateBeginFrom_ASD").getValidatedDate()
 	var dateBeginTo = document.getElementById("dateBeginTo_ASD").getValidatedDate()

	if(dateBeginFrom == null) {
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return
	}
	
	if(dateBeginTo == null) {
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return
	}

	if(dateBeginFrom > dateBeginTo) {
		showErrorMsg('<g:message code="dateRangeERRORMsg"/>')
		 $("#dateBeginFromDiv_ASD").addClass("has-error");
		 $("#dateBeginToDiv_ASD").addClass("has-error");
		return
	}

	processUserSearch($("#advancedSearchText").val(), dateBeginFrom.format(), dateBeginTo.format())
 	$("#advancedSearchDialog").dialog("close");
 });


function showErrorMsg(errorMsg) {
	$("#advancedSearchDialogErrorPanel").html('<p>' + errorMsg + '<p>')
	$("#advancedSearchDialogErrorPanel").fadeIn()
}
</r:script>