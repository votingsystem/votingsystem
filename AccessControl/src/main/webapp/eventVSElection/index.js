function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
    var ajaxUrl= "${elementURL}/search/eventVS?searchText=" +
        textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=ELECTION"
}

function processSearchJSON(dataJSON) {
    var ajaxUrl= "${elementURL}/search/eventVS";
}