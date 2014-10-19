<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="${resource(dir: '/bower_components/vs-advanced-search-dialog', file: 'vs-advanced-search-dialog.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/search-info.gsp']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election-list']"/>">
</head>
<body>
<vs-innerpage-signal title="<g:message code="electionSystemLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <search-info id="searchInfo"></search-info>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
    background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <eventvs-election-list id="eventvsList" url="${createLink(controller: 'eventVSElection', action: 'index')}?menu=${params.menu}&eventVSState=ACTIVE"
                  eventvstype="election"></eventvs-election-list>
</div>
<vs-advanced-search-dialog id="advancedSearchDialog"></vs-advanced-search-dialog>
</body>
</html>
<asset:script>
    function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        var ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=ELECTION"
    }

    function processSearchJSON(jsonData) {
        var ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}";
    }
</asset:script>