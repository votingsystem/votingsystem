<div id="confirmOptionDialog" title="<g:message code="confirmOptionDialogCaption"/>" style="display:none;">
	<p style="text-align: center;">
		<g:message code="confirmOptionDialogMsg"/>:<br>
		<b><span id="optionSelectedDialogMsg"></span></b>
	</p>
</div>
<r:script>
$("#confirmOptionDialog").dialog({width: 500, autoOpen: false, modal: true,
    buttons: [{text:"<g:message code="acceptLbl"/>",
             	icons: { primary: "ui-icon-check"},
           	click:function() {
           		sendVote()
           		$(this).dialog( "close" );}},
         	{text:"<g:message code="cancelLbl"/>",
           	icons: { primary: "ui-icon-closethick"},
           	click:function() {
  					$(this).dialog( "close" );
      	 		}}],
    show: {effect:"fade", duration: 300},
    hide: {effect: "fade",duration: 300}
});
</r:script>