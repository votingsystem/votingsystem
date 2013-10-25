<div id="selectRepresentativeDialog" title="<g:message code="selectRepresentativeLbl"/>" style="padding:20px 20px 20px 20px;display:none;">
	<g:message code="selectRepresentativeMsg" args="${[representativeName]}"/>
	<p style="text-align:center;"><g:message code="clickAcceptToContinueLbl"/></p>
</div> 
<script>
   $("#selectRepresentativeDialog").dialog({
   	  width: 500, autoOpen: false, modal: true,
      buttons: [{id: "acceptButton",
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		selectRepresentative(); 
             		$(this).dialog( "close" );  	   			   				
		        	}}, {	
   			        	id: "cancelButton",
		        		text:"<g:message code="cancelLbl"/>",
		               	icons: { primary: "ui-icon-closethick"},
		             	click:function() {
   			   					$(this).dialog( "close" );
   			       	 		}}],
      show: {effect:"fade", duration: 300},
      hide: {effect: "fade",duration: 300}
 });
</script>