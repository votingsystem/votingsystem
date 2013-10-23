<div style="display: none;">
	<div id='browserWithoutJavaDialog' title='<g:message code='browserWithoutJavaCaption'/>' style='display:table;'>
		<div style='display:table-cell; vertical-align:middle;'>
			<img src='${resource(dir:'images',file:'advert_64x64.png')}' style='margin:3px 0 0 10px;'></img>
		</div>
		<div style='display:table-cell;width:15px;'></div>
		<div style='display:table-cell; vertical-align:middle;'><g:message code='browserWithoutJavaMsg'/>.</div>
	</div>
</div>
<script>
$("#browserWithoutJavaDialog").dialog({
	width: 600, autoOpen: false, modal: true,
	buttons: [{text:"<g:message code="acceptLbl"/>",
	          	icons: { primary: "ui-icon-check"},
	        	click:function() {$(this).dialog( "close" );}}],
	show: {effect:"fade", duration: 300},
	hide: {effect: "fade",duration: 300}
});
</script>