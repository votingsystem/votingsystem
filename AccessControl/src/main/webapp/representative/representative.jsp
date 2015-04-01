<html>
<head>
    <link href="${config.webURL}/representative/representative-info.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.representativeLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <representative-info id="representative" representative='${representativeMap}'></representative-info>
    </div>
</body>
</html>
<script>
</script>