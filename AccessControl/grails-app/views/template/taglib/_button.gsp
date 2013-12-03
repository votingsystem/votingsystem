<g:if test="${'true'.equals(attrs.isButton)}">
	<button id='${attrs.id}' class='button_base' href='${attrs.href}' style='${attrs.style}'>
        <g:if test="${attrs.iconName != null}">
            <div class="buttonImage"><fatcow:icon iconName='${attrs.iconName}'/></div>
        </g:if>
        <g:else>
            <div class="buttonImage"><img src='${attrs.imgSrc}' style='margin:0px 0px 0px 5px;'></img></div>
        </g:else>
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




