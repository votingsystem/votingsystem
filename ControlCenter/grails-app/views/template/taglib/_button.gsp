<g:if test="${'true'.equals(attrs.isSubmitButton)}">
	<button id='${attrs.id}' class='button_base' href='${attrs.href}' style='${attrs.style}'>
		<div class="buttonImage"><img src='${attrs.imgSrc}' style='margin:0px 0px 0px 5px;'></img></div>
		<span class='buttonText'>${attrs.message}</span>
	</button>
</g:if>
<g:else>
	<g:if test="${attrs.href}">
		<a id='${attrs.id}' class='button_base' href='${attrs.href}' style='${attrs.style}'>
			<img class='buttonImage' src='${attrs.imgSrc}'></img>
			<span class='buttonText'>${attrs.message}</span>
		</a>
	</g:if>
	<g:else>
		<div id='${attrs.id}' class='button_base' style='${attrs.style}'>
			<img class='buttonImage' src='${attrs.imgSrc}'></img>
			<span class='buttonText'>${attrs.message}</span>
		</div>
	</g:else>
</g:else>




