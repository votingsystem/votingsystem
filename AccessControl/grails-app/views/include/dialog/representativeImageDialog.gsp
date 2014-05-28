<div id="imageDialog" title="" style="padding:20px 20px 20px 20px">
	<img id="dialogRepresentativeImg" style="width:100%; height: 100%;"></img>
</div> 
<asset:script>
   $("#imageDialog").dialog({
	   	  width: 500, autoOpen: false, modal: true,
	      buttons: [{id: "acceptButton",
	        		text:"<g:message code="acceptLbl"/>",
	               	icons: { primary: "ui-icon-check"},
	             	click:function() {
	             		$(this).dialog( "close" );   	   			   				
			        	}}],
	      show: {effect:"fade", duration: 300},
	      hide: {effect: "fade",duration: 300}
 });
</asset:script>