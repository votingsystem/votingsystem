<!DOCTYPE html>
<html>
    <head>
        <meta name="HandheldFriendly" content="true" />
        <meta name="viewport" content="width=device-width, height=device-height, user-scalable=no" />
        <meta name="mobile-web-app-capable" content="yes">
        <link href="${config.webURL}/css/votingSystem.css" media="all" rel="stylesheet" />
        <link href="${config.webURL}/eventVSElection/eventvs-election-stats.vsp" rel="import"/>
    </head>
    <body>
        <div style="margin: 0 auto;padding: 100px;">
            <eventvs-election-stats statsDataMap='${statsDataMap}'></eventvs-election-stats>
        </div>
    </body>
</html>