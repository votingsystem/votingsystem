<html>
<head>
    <link href="${config.webURL}/certificateVS/cert-request-form.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.certRequestLbl}"></vs-innerpage-signal>
    <div id="contentDiv" class="pageContentDiv" style="min-height: 1000px;">
        <cert-request-form></cert-request-form>
    </div>
</body>
</html>