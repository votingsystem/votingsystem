<div id="requestRepresentativeVotingHistoryDialog" title="<g:message code="requestVotingHistoryLbl"/>" style="padding:20px 20px 20px 20px;display:none;">
	<g:message code="representativeHistoryRequestMsg"/>
	<label><g:message code="selectDateRangeMsg"/></label>
	<form id="reqVotingHistoryForm">
		<input type="hidden" autofocus="autofocus" />
		<div style="display:table;margin:20px 0px 0px 0px;">
			<div style="display:table-cell;margin:0px 0px 0px 20px;">
			<votingSystem:datePicker id="dateFrom" title="${message(code:'firstDaterangeLbl')}"
				 style="width:200px;"
				 placeholder="${message(code:'firstDaterangeLbl')}"
				 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
				 onchange="this.setCustomValidity('')"></votingSystem:datePicker>
			</div>
			<div style="display:table-cell;margin:0px 0px 0px 20px;">				
				<votingSystem:datePicker id="dateTo" title="${message(code:'dateToLbl')}"
					 style="width:200px;margin:0px 0px 0px 20px;"
					 placeholder="${message(code:'dateToLbl')}"
					 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
					 onchange="this.setCustomValidity('')"></votingSystem:datePicker>
			</div>
		</div>
		<div style="margin:15px 0px 20px 0px">
			<input type="email" id="userEmailText" style="width:350px; margin:0px auto 0px auto;" required
				title='<g:message code='enterEmailLbl'/>'
				placeholder='<g:message code='emailInputLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
				onchange="this.setCustomValidity('')"/>
		</div>
		<input id="submitVotingHistoryRequest" type="submit" style="display:none;">
	</form> 	   
</div> 
<r:script>

  $("#requestRepresentativeVotingHistoryDialog").dialog({
   	  width: 550, autoOpen: false, modal: true,
      buttons: [{id: "acceptButton",
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		$("#submitVotingHistoryRequest").click() 	   			   				
	        	}}, {id: "cancelButton",
		        		text:"<g:message code="cancelLbl"/>",
		               	icons: { primary: "ui-icon-closethick"},
		             	click:function() {
   			   					$(this).dialog( "close" );
   			       	 		}}],
      show: {effect:"fade", duration: 300},
      hide: {effect: "fade",duration: 300}
});
</r:script>