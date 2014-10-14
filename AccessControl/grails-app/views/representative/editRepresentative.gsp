<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/representative/representative-edit-form']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="editRepresentativeLbl"/>"></votingsystem-innerpage-signal>
    <div layout vertical class="pageContentDiv">
        <representative-edit-form id="representativeEditor"></representative-edit-form>
    </div>
</body>
</html>
<asset:script>
</asset:script>