<div id="advancedSearchDialog" title="<g:message code="advancedSearchLbl"/>" style="display:none;">
	<div class="errorMsgWrapper" style="display:none;"></div>
	<p style="text-align: center;"><g:message code="advancedSearchMsg"/>.</p>
  		<form id="advancedSearchForm">
		<input type="hidden" autofocus="autofocus" />
		<div style="margin:0px auto 0px auto; width:50%">
  				<input type="text" id="advancedSearchText" style="" required
  					title="<g:message code="advancedSearchFieldLbl"/>"
  					placeholder="<g:message code="advancedSearchFieldLbl"/>"
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')" />
 				</div>

				<div style="display:block;margin:20px 0px 0px 0px;">
 				<div style="display:inline-block;margin:0px 0px 0px 20px;">
					<votingSystem:datePicker id="dateBeginFrom" title="${message(code:'dateBeginFromLbl')}"
						style="width:230px;"
						placeholder="${message(code:'dateBeginFromLbl')}"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
 				</div>
 				<div style="display:inline-block;margin:0px 0px 0px 20px;">			
					<votingSystem:datePicker id="dateBeginTo" title="${message(code:'dateToLbl')}"
						placeholder="${message(code:'dateToLbl')}"
	   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
	   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
  				</div>
 				</div>
 				
				<div style="display:block;margin:20px 0px 0px 0px;">
 				<div style="display:inline-block;margin:0px 0px 0px 20px;">
				<votingSystem:datePicker id="dateFinishFrom" title="${message(code:'dateFinishFromLbl')}"
				 	style="width:230px;"
					placeholder="${message(code:'dateFinishFromLbl')}"
   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
 				</div>

 				<div style="display:inline-block;margin:0px 0px 0px 20px;">
				<votingSystem:datePicker id="dateFinishTo" title="${message(code:'dateToLbl')}"
					placeholder="${message(code:'dateToLbl')}"
   					oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
   					onchange="this.setCustomValidity('')"></votingSystem:datePicker>
  				</div>
 				</div>   				
 				<input id="submitSearch" type="submit" style="display:none;">
  		</form>
  	</div> 
<r:script>

var dateBeginFrom  = $("#dateBeginFrom"),
	dateBeginTo    = $("#dateBeginTo"),
	dateFinishFrom = $("#dateFinishFrom"),
	dateFinishTo   = $("#dateFinishTo"),
	allFields = $( [] ).add(dateBeginFrom).add(dateBeginTo).add(dateFinishFrom).add(dateFinishTo);

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
	      open: function( event, ui ) {
	 			$("#advancedSearchText").val("");
	 			$("#dateBeginFrom").val("");
	 			$("#dateBeginTo").val("");
	 			$("#dateFinishFrom").val("");
	 			$("#dateFinishTo").val("");
		  }
	    });

 $('#advancedSearchForm').submit(function(event){
 	console.log("advancedSearchForm")
 	event.preventDefault();
 	allFields.removeClass("formFieldError");
 	$(".errorMsgWrapper").fadeOut()
	if(dateBeginFrom.datepicker("getDate") === null) {
		dateBeginFrom.addClass( "formFieldError" );
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return
	}
	
	if(dateBeginTo.datepicker("getDate") === null) {
		dateBeginTo.addClass( "formFieldError" );
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return
	}

	if(dateBeginFrom.datepicker("getDate") > 
		dateBeginTo.datepicker("getDate")) {
		showErrorMsg('<g:message code="dateRangeERRORMsg"/>') 
		dateBeginFrom.addClass("formFieldError");
		dateBeginTo.addClass("formFieldError");
		return
	}

	if(dateFinishFrom.datepicker("getDate") === null) {
		dateFinishFrom.addClass( "formFieldError" );
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return
	}

	if(dateFinishTo.datepicker("getDate") === null) {
		dateFinishTo.addClass( "formFieldError" );
		showErrorMsg('<g:message code="emptyFieldMsg"/>')
		return
	}

	if(dateFinishFrom.datepicker("getDate") > 
		dateFinishTo.datepicker("getDate")) {
		showErrorMsg('<g:message code="dateRangeERRORMsg"/>') 
		dateFinishFrom.addClass("formFieldError");
		dateFinishTo.addClass("formFieldError");
		return
	}
 	getSearchResult(getAdvancedSearchQuery())
 	$("#advancedSearchDialog").dialog("close");
 });


function getAdvancedSearchQuery() {
	var searchQuery = {textQuery:$("#advancedSearchText").val(),
			dateBeginFrom: dateBeginFrom.datepicker("getDate").format(),
			dateBeginTo:dateBeginTo.datepicker("getDate").format(),
			dateFinishFrom:dateFinishFrom.datepicker("getDate").format(),
			dateFinishTo:dateFinishTo.datepicker("getDate").format()}
	return searchQuery
}

function showErrorMsg(errorMsg) {
	$("#advancedSearchDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#advancedSearchDialog .errorMsgWrapper").fadeIn()
}
</r:script>