<!DOCTYPE html>
<html>
<head>
  	<title>pruebaTemplate</title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<body>
    <div id="dialog" title="Basic dialog">
    </div>
</body>
</html>
<asset:script>

</asset:script>