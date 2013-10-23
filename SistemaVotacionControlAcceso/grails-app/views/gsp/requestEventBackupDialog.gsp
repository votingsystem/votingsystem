<div id='requestEventBackupDialog' title='<g:message code="backupRequestCaption"/>' style="display:none;">
	<div style='text-align:center;'>
		<g:message code="backupRequestMsg"/>
	</div>
	<form id="requestEventBackupForm">
		<div style="margin:15px 0px 20px 0px">
			<input type="email" id="eventBackupUserEmailText" style="width:360px; margin:0px auto 0px auto;" required
				title='<g:message code='enterEmailLbl'/>'
				placeholder='<g:message code='emailInputLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
				onchange="this.setCustomValidity('')"/>
		</div>
		<input id="submitBackupRequest" type="submit" style="display:none;">
	</form>
</div>
<script>

$("#requestEventBackupDialog").dialog({
 	  width: 400, autoOpen: false, modal: true,
    buttons: [{id: "acceptButton",
      		text:"<g:message code="acceptLbl"/>",
             	icons: { primary: "ui-icon-check"},
           	click:function() {
           		$("#submitBackupRequest").click()
           		$(this).dialog( "close" ); 	   			   				
	        	}}, {id: "cancelButton",
	        		text:"<g:message code="cancelLbl"/>",
	               	icons: { primary: "ui-icon-closethick"},
	             	click:function() {
	   					$(this).dialog( "close" );
	       	 		}}],
    show: {effect:"fade", duration: 300},
    hide: {effect: "fade",duration: 300}
});
</script>