<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/representative/representative-editor"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="newRepresentativeLbl"/>"></vs-innerpage-signal>
    <div layout vertical class="pageContentDiv">
        <representative-editor id="representativeEditor"></representative-editor>
    </div>
</body>
</html>
<asset:script>
</asset:script>