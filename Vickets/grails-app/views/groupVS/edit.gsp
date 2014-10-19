<html>
<head>
    <script type="text/javascript">

    </script>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="${resource(dir: '/bower_components/vs-texteditor', file: 'vs-texteditor.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-editor']"/>">

</head>
<body>
    <vs-innerpage-signal title="<g:message code="editGroupVSLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv" style="min-height: 1000px; padding:0px 30px 0px 30px;">
        <groupvs-editor groupvs='${groupvsMap as grails.converters.JSON}'></groupvs-editor>
    </div>
</body>
</html>
<asset:script>

</asset:script>