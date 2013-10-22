<g:if test="${'true'.equals(button.isButton)}">
	<button id='${button.id}' class='button_base' href='${button.href}' style='${button.style}'>
		<div class="buttonImage"><img src='${button.imgSrc}' style='margin:0px 0px 0px 5px;'></img></div>
		<div class='buttonText'>${button.message}</div>
	</button>
</g:if>
<g:else>
	<g:if test="${button.href}">
		<a id='${button.id}' class='button_base' href='${button.href}' style='${button.style}'>
			<img class='buttonImage' src='${button.imgSrc}'></img>
			<span class='buttonText'>${button.message}</span>
		</a>
	</g:if>
	<g:else>
		<div id='${button.id}' class='button_base' style='${button.style}'>
			<img class='buttonImage' src='${button.imgSrc}'></img>
			<span class='buttonText'>${button.message}</span>
		</div>
	</g:else>
</g:else>




