<div id='resultDialog' title='' style="display:none;">
	<p id='resultMessage' style="text-align: center;"></p>
</div>
<r:script>

var clientCallback = null
$("#resultDialog").dialog({
	 width: 600, autoOpen: false, modal: true,
	 buttons: [{text:"<g:message code="acceptLbl"/>",
	         	icons: { primary: "ui-icon-check"},
		       	click:function() {
		       		$(this).dialog( "close" );
					if(clientCallback != null) clientCallback()
		       	}}],
	show: {effect: "fade",duration: 100},
	hide: { effect: "fade", duration: 100}
});

function showResultDialog(caption, message, callback) {
	console.log("showResultDialog - caption: " + caption + " - message: "+ message);
	$('#resultMessage').html(message);
	$('#resultDialog').dialog('option', 'title', caption);
	$("#resultDialog").dialog( "open" );
	clientCallback = callback
}
</r:script>