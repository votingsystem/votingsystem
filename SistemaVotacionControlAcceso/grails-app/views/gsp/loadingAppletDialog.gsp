<div id='loadingVotingSystemAppletDialog' title='<g:message code="appletLoadingCaption"/>' style="display: none;">
	<p style='text-align: center;'><g:message code="appletLoadingMsg"/>.</p>
 	<progress style='display:block;margin:0px auto 10px auto;'></progress>
</div>
<script>
$("#loadingVotingSystemAppletDialog").dialog({
	width: 330, autoOpen: false, modal: true,
    show: {effect: "fade", duration: 1000},
    hide: {effect: "fade", duration: 1000}});
</script>