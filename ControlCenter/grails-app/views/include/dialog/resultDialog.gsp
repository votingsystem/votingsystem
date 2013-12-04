<div id='resultDialog' title='' style="display:none;">
	<p id='resultMessage' style='text-align: center;'>
</p>
</div>
<r:script>
$("#resultDialog").dialog({
	 width: 600, autoOpen: false, modal: true,
	 buttons: [{text:"<g:message code="acceptLbl"/>",
	         	icons: { primary: "ui-icon-check"},
		       	click:function() {
		       		$(this).dialog( "close" );}}],
	show: {effect: "fade",duration: 1000},
	hide: { effect: "fade", duration: 1000}
});

function showResultDialog(caption, message) {
	console.log("showResultDialog - caption: " + caption + " - message: "+ message);
	$('#resultMessage').html(message);
	$('#resultDialog').dialog('option', 'title', caption);
	$("#resultDialog").dialog( "open" );
}
</r:script>