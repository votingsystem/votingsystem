<html>
<head>
    <link href="${resourceURL}/vs-advanced-search-dialog/vs-advanced-search-dialog.html" rel="import"/>
    <link href="${elementURL}/element/search-info.vsp" rel="import"/>
    <link href="${elementURL}/representative/representative-list.vsp" rel="import"/>
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
        var ajaxUrl= "${restURL}/search/eventVS?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=ELECTION"
    }

    function processSearchJSON(dataJSON) {
        var ajaxUrl= "${restURL}/search/eventVS";
    }
</script>