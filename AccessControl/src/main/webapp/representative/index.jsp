<html>
<head>
    <link href="${config.resourceURL}/vs-advanced-search-dialog/vs-advanced-search-dialog.html" rel="import"/>
    <link href="${config.webURL}/element/search-info.vsp" rel="import"/>
    <link href="${config.webURL}/representative/representative-list.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.representativesPageLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <search-info id="searchInfo"></search-info>
        <representative-list id="representativeList" representativeData='${representativeData}'></representative-list>
    </div>
    <vs-advanced-search-dialog id="advancedSearchDialog"></vs-advanced-search-dialog>
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