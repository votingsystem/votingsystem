<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/representative/representative-edit-form']"/>">
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="editRepresentativeLbl"/>"></vs-innerpage-signal>
    <div layout vertical class="pageContentDiv">
        <representative-edit-form id="representativeEditor"></representative-edit-form>
    </div>
</body>
</html>
<asset:script>
</asset:script>