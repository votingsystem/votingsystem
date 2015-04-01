<!DOCTYPE html>
<html>
<head>

    <link href="${config.webURL}/certificateVS/cert-list.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.certsPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv">
    <cert-list id="certList" url="${config.webURL}/certificateVS/certs"></cert-list>
</div>
</body>
</html>
<script>
</script>