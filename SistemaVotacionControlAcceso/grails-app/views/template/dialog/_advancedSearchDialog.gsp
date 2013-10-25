<div id="advancedSearchDialog" title="<g:message code="advancedSearchLabel"/>" style="display:none;">
	<p style="text-align: center;"><g:message code="advancedSearchMsg"/>.</p>
  		<form id="advancedSearchForm">
		<input type="hidden" autofocus="autofocus" />
		<div style="margin:0px auto 0px auto; width:50%">
  				<input type="text" id="searchText" style="" required
  					title="<g:message code="advancedSearchFieldLbl"/>"
  					placeholder="<g:message code="advancedSearchFieldLbl"/>"
   				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   				onchange="this.setCustomValidity('')" />
 				</div>

				<div style="display:block;margin:20px 0px 0px 0px;">
 				<div style="display:inline-block;margin:0px 0px 0px 20px;">
				<input type="text" id="dateBeginFrom" style="width:230px;" required readonly
					title="<g:message code="dateBeginFromLbl"/>"
					placeholder="<g:message code="dateBeginFromLbl"/>"
   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   					onchange="this.setCustomValidity('')"/>
 				</div>
 				<div style="display:inline-block;margin:0px 0px 0px 20px;">
				<input type="text" id="dateBeginTo" required readonly
					title="<g:message code="dateToLbl"/>"
					placeholder="<g:message code="dateToLbl"/>"
   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   					onchange="this.setCustomValidity('')"/>
  				</div>
 				</div>
 				
				<div style="display:block;margin:20px 0px 0px 0px;">
 				<div style="display:inline-block;margin:0px 0px 0px 20px;">
				<input type="text" id="dateFinishFrom" style="width:230px;" required readonly
					title="<g:message code="dateFinishFromLbl"/>"
					placeholder="<g:message code="dateFinishFromLbl"/>"
					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   					onchange="this.setCustomValidity('')"/>
 				</div>

 				<div style="display:inline-block;margin:0px 0px 0px 20px;">
				<input type="text" id="dateFinishTo" required readonly
					title="<g:message code="dateToLbl"/>"
					placeholder="<g:message code="dateToLbl"/>"
   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
   					onchange="this.setCustomValidity('')"/>
  				</div>
 				</div>   				
 				<input id="submitSearch" type="submit" style="display:none;">
  		</form>
  	</div> 
<script>
 $("#advancedSearchDialog").dialog({
		width: 'auto', autoOpen: false, modal: true,
	      buttons: [{text:"<g:message code="acceptLbl"/>",
			               	icons: { primary: "ui-icon-check"},
			             	click:function() {
	   	   			   				$("#submitSearch").click() 	   	   			   				
	   	   			        	} },{text:"<g:message code="cancelLbl"/>",
			               	icons: { primary: "ui-icon-closethick"},
			             	click:function() {
	   			   				$(this).dialog( "close" );
	   			       	 	}}],
	      show: {effect: "fade",duration: 1000},
	      hide: {effect: "fade",duration: 1000},
	      open: function( event, ui ) {
	 		$("#searchText").val("")
	 		$("#dateBeginFrom").val("")
	 		$("#dateBeginTo").val("")
	 		$("#dateFinishFrom").val("")
	 		$("#dateFinishTo").val("")
		  }
	    });

 $("#dateBeginFrom").datepicker(pickerOpts);
 $("#dateBeginTo").datepicker(pickerOpts);
 $("#dateFinishFrom").datepicker(pickerOpts);
 $("#dateFinishTo").datepicker(pickerOpts);
</script>