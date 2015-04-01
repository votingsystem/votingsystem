<html>
<head>
    <link href="${config.webURL}/eventVSElection/eventvs-election.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.pollLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <eventvs-election id="electionVS" eventvs='${eventMap}'></eventvs-election>
    </div>
</body>
</html>