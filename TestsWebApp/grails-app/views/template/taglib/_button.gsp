<g:if test="${'true'.equals(attrs.isButton)}">
	<input id='${attrs.id}Hidden' type="submit" style="display:none;">
	<div id='${attrs.id}' class='button_base' href='${attrs.href}' style='padding:2px 5px 2px 5px;height:30px;${attrs.style}'>
        <g:if test="${attrs.imgSrc}"><img class='buttonImage' src='${attrs.imgSrc}'/></g:if>
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
        <g:if test="${attrs.imgSrc}"><img class='buttonImage' src='${attrs.imgSrc}'/></g:if>
		<a id='${attrs.id}' class='button_base' href='${attrs.href}' style='padding:2px 5px 2px 5px;height:30px;${attrs.style}'>
		${attrs.message}
		</a>
	</g:if>
	<g:else>
		<div id='${attrs.id}' class='button_base' style='padding:2px 5px 2px 5px;height:30px;${attrs.style}'>
            <g:if test="${attrs.imgSrc}"><img class='buttonImage' src='${attrs.imgSrc}'/></g:if>
            ${attrs.message}
		</div>
	</g:else>
</g:else>





