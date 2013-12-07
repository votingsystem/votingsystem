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
<g:else>
    <div style='margin:auto; display:table;'>${attrs.message}</div></g:else>
</button>
<r:script>
    <g:if test="${attrs.href}">
        $("#${attrs.id}").click(function () {
            window.location.href = "${attrs.href}"
        });
    </g:if>
</r:script>

