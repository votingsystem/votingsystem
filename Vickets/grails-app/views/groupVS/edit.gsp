<html>
<head>
    <script type="text/javascript">

    </script>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-editor']"/>">

</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="editGroupVSLbl"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv" style="min-height: 1000px; padding:0px 30px 0px 30px;">
        <groupvs-editor groupvs='${groupvsMap as grails.converters.JSON}'></groupvs-editor>
    </div>
</body>
</html>
<asset:script>

</asset:script>