<!DOCTYPE html>
<html>
<head>
  	<title>testing</title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<body>
<div layout horizontal center center-justified id="progressDiv"
     style="width:100%;height:100%;display:{{isProcessing? 'block':'none'}}">
    <progress></progress>
</div>
</body>
</html>
<asset:script>

</asset:script>