<html>
<head>
    <link href="${config.resourceURL}/vs-advanced-search-dialog/vs-advanced-search-dialog.html" rel="import"/>
    <link href="${config.webURL}/element/search-info.vsp" rel="import"/>
    <link href="${config.webURL}/eventVSClaim/eventvs-claim-list.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.claimSystemLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <search-info id="searchInfo"></search-info>
        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
        <eventvs-claim-list id="eventvsList" url="${config.webURL}/eventVSElection?menu=${params.menu}&eventVSState=ACTIVE"
                      eventvstype="claim"></eventvs-claim-list>
    </div>
    <vs-advanced-search-dialog id="advancedSearchDialog"></vs-advanced-search-dialog>
</body>
</html>
<script>
    function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        var ajaxUrl= "${config.webURL}/search/eventVS?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=CLAIM"
    }

    function processSearchJSON(dataJSON) {
        var ajaxUrl= "${config.webURL}/search/eventVS";
    }
</script>