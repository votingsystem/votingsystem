<html>
<head>

    <link href="${config.resourceURL}/vs-advanced-search-dialog/vs-advanced-search-dialog.html" rel="import"/>
    <link href="${config.webURL}/element/search-info.vsp" rel="import"/>
    <link href="${config.webURL}/eventVSElection/eventvs-election-list.vsp" rel="import"/>
</head>
<body>
    <div class="pageContentDiv">
        <search-info id="searchInfo"></search-info>
        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
            background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
        <eventvs-election-list id="eventvsList" eventsVSMap='${eventsVSMap}'
                      eventvstype="election" eventVSState="${params.eventVSState}"></eventvs-election-list>
    </div>
    <vs-advanced-search-dialog id="advancedSearchDialog"></vs-advanced-search-dialog>

    =========== contextURL: ${contextURL}

</body>
</html>
<script>
    function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        var ajaxUrl= "${config.webURL}/search/eventVS?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=ELECTION"
    }

    function processSearchJSON(dataJSON) {
        var ajaxUrl= "${config.webURL}/search/eventVS";
    }
</script>