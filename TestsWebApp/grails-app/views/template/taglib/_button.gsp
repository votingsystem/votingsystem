<g:if test="${'true'.equals(attrs.isSubmitButton)}">
    <button id='${attrs.id}' class='button_base' href='${attrs.href}' style='${attrs.style}'>
</g:if>
<g:else>
    <button id='${attrs.id}' class='button_base' style='${attrs.style}' onclick="return false;">
</g:else>
<g:if test="${attrs.imgSrc}">
    <div class="buttonImage"><img src='${attrs.imgSrc}' style='margin:0px 0px 0px 5px;'/></div>
    <span class='buttonText'>${attrs.message}</span>
</g:if>
<g:else><span style='margin:0 7px 0 7px;'>${attrs.message}</span></g:else>
</button>
<r:script>
    <g:if test="${attrs.href}">
        $("#${attrs.id}").click(function () {
            window.location.href = "${attrs.href}"
        });
    </g:if>
</r:script>