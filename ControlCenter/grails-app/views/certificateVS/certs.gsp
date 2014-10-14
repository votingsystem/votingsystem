<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/certificateVS/cert-list.gsp']"/>">
    <style type="text/css" media="screen"></style>
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="certsPageTitle"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv">
    <cert-list id="certList" url="<g:createLink controller="certificateVS" action="certs"/>"></cert-list>
</div>
</body>
</html>
<asset:script>
</asset:script>