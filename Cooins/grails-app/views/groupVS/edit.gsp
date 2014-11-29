<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webresource dir="vs-texteditor" file="vs-texteditor.html"/>
    <vs:webcomponent path="/groupVS/groupvs-editor"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="editGroupVSLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv" style="min-height: 1000px; padding:0px 30px 0px 30px;">
        <groupvs-editor groupvs='${groupvsMap as grails.converters.JSON}'></groupvs-editor>
    </div>
</body>
</html>
<asset:script>

</asset:script>