<!DOCTYPE html>
<html>
    <head>
        <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election-stats']"/>">
    </head>
    <body>
        <div horizontal layout center center-justified>
            <eventvs-election-stats eventVSId="${params.id}" style="margin: 0 auto;"></eventvs-election-stats>
        </div>
    </body>
</html>