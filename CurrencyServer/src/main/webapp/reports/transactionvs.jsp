<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${config.webURL}/transactionVS/transactionvs-table.vsp" rel="import"/>
    <link href="${config.webURL}/transactionVS/transactionvs-selector.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.transactionPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div layout horizontal center center-justified>
        <transactionvs-selector id="transactionSelector"></transactionvs-selector>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <transactionvs-table id="recordList" url="${config.restURL}/reports/transactionvs"></transactionvs-table>

</div>
</body>

</html>
<script>
    document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-selector-selected', function(e) {
        var transactionvsType = e.detail
        console.log("transactionvsType: " + transactionvsType)
        targetURL = "${config.restURL}/reports/transactionvs";
        if("" != transactionvsType) {
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#recordList").url = targetURL
    });

    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "${msg.searchResultLbl} '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#recordList").url = "${config.restURL}/transactionVS?searchText=" + textToSearch
    }

    function processSearchJSON(dataJSON) {
        document.querySelector("#recordList").params = dataJSON
        document.querySelector("#recordList").url = "${config.restURL}/transactionVS"
    }
</script>
