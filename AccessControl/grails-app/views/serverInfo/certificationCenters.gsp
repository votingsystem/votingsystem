<html>
    <head>
        <title>${message(code: 'serverNameLbl', null)}</title>
        <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
        <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
        <g:else><meta name="layout" content="main" /></g:else>
    </head>
    <body>
	   <p><b>Comisaría Local de la población</b><br/>
	   	<div>Dirección: C/ Avda de la constitución</div>
	   </p>
	<HR>
	</body>
</html>
