<g:if test="${exception?.message}">
	${exception?.message}
</g:if>
<g:else>
	ERROR 500
</g:else>