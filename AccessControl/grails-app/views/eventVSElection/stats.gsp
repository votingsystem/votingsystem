<!DOCTYPE html>
<html>
    <head>
        <meta name="HandheldFriendly" content="true" />
        <meta name="viewport" content="width=device-width, height=device-height, user-scalable=no" />
        <meta name="mobile-web-app-capable" content="yes">
        <asset:stylesheet src="votingSystem.css"/>
        <vs:webcomponent path="/eventVSElection/eventvs-election-stats"/>
    </head>
    <body>
        <div style="margin: 0 auto;padding: 100px;">
            <eventvs-election-stats eventVSId="${params.id}"></eventvs-election-stats>
        </div>
    </body>
</html>