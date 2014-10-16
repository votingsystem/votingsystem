<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/representative/representative-editor']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="newRepresentativeLbl"/>"></votingsystem-innerpage-signal>
    <div layout vertical class="pageContentDiv">
        <representative-editor id="representativeEditor"></representative-editor>
    </div>
</body>
</html>
<asset:script>
</asset:script>