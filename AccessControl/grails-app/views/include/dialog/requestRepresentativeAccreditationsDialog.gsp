<div id="requestRepresentativeAccreditationsDialog" title="<g:message code="requestRepresentativeAcreditationsLbl"/>" style="padding:20px 20px 20px 20px">
	<g:message code="accreditationRequestMsg"/>
	<form id="accreditationRequestForm">
		<input type="hidden" autofocus="autofocus" />
		<div style="display:table-cell;margin:0px 0px 0px 20px;">
            <label>${message(code:'dateRequestLbl')}</label>
			<votingSystem:datePicker id="accreditationDateSelected" title="${message(code:'dateRequestLbl')}"
				 style="width:200px;"
				 placeholder="${message(code:'dateRequestLbl')}"
				 oninvalid="this.setCustomValidity('${message(code:'emptyFieldLbl')}')"
				 onchange="this.setCustomValidity('')"></votingSystem:datePicker>
		</div>
		<div style="margin:15px 0px 20px 0px">
			<input type="email" id="accreditationReqUserEmailText" style="width:350px; margin:0px auto 0px auto;" required
				title='<g:message code='enterEmailLbl'/>'
				placeholder='<g:message code='emailInputLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
				onchange="this.setCustomValidity('')"/>
		</div>			
		<input id="submitAccreditationRequest" type="submit" style="display:none;">
	</form>
</div> 	
<r:script>
   $("#requestRepresentativeAccreditationsDialog").dialog({
	   	  width: 500, autoOpen: false, modal: true,
	      buttons: [{id: "acceptButton",
	        		text:"<g:message code="acceptLbl"/>",
	               	icons: { primary: "ui-icon-check"},
	             	click:function() {
	             		$("#submitAccreditationRequest").click()  	   			   				
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