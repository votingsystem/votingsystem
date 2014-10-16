<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="pollLbl"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <eventvs-election id="electionVS" eventvs="${eventMap as grails.converters.JSON}"></eventvs-election>
    </div>
</body>
</html>