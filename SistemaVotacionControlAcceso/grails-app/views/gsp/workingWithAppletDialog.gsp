<div id='workingWithAppletDialog' title='<g:message code='appletRunningMsg'/>' style="display: none;">
	<p style='text-align: center;'><g:message code='workingWithAppletMsg'/>.</p>
</div>
<script>
$("#workingWithAppletDialog").dialog({
   	  width: 330, autoOpen: false, modal: true,
      show: {effect: "fade",duration: 1000},
      hide: {effect: "fade", duration: 1000}
});
</script>