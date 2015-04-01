<html>
<head>
    <link href="${config.resourceURL}/vs-texteditor/vs-texteditor.html" rel="import"/>
    <link href="${config.webURL}/groupVS/groupvs-editor.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.editGroupVSLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv" style="min-height: 1000px; padding:0px 30px 0px 30px;">
        <groupvs-editor groupvs='${groupvsMap}'></groupvs-editor>
    </div>
</body>
</html>
<script>

</script>