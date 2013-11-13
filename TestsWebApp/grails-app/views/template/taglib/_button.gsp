<g:if test="${'true'.equals(attrs.isButton)}">
	<input id='${attrs.id}Hidden' type="submit" style="display:none;">
	<div id='${attrs.id}' class='button_base' href='${attrs.href}' style='${attrs.style}'>
		${attrs.message}
	</div>
	<r:script>
  		$("#${attrs.id}").click(function () {
  			$("#${attrs.id}Hidden").click();
		});
	</r:script>
</g:if>
<g:else>
	<g:if test="${attrs.href}">
		<a id='${attrs.id}' class='button_base' href='${attrs.href}' style='${attrs.style}'>
		${attrs.message}
		</a>
	</g:if>
	<g:else>
		<div id='${attrs.id}' class='button_base' style='${attrs.style}'>
			${attrs.message}
		</div>
	</g:else>
</g:else>





