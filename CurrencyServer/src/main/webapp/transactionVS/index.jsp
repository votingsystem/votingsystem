<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${elementURL}/transactionVS/transactionvs-list.vsp" rel="import"/>
    <link href="${elementURL}/transactionVS/transactionvs-selector.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.transactionPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div layout horizontal center center-justified>
        <transactionvs-selector id="transactionSelector" transactionvsType="${params.transactionvsType}"></transactionvs-selector>
    </div>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <transactionvs-list id="currencyTransactionTable" transactionsMap='${transactionsMap}'></transactionvs-list>
</div>
</body>

</html>
<script>

    if(typeof sendSocketVSMessage != 'undefined') {
        document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-new', function(e) {
            console.log("listener.gsp - core-signal-transactionvs-new")
            document.querySelector("#currencyTransactionTable").addTransaction(e.detail)
        });
        sendSocketVSMessage({operation:Operation.LISTEN_TRANSACTIONS})
    } else console.log("listener.gsp - no socket service available")

    document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-selector-selected', function(e) {
        var transactionvsType = e.detail
        console.log("index.gsp - transactionvsType: " + transactionvsType)
        targetURL = "${restURL}/transactionVS";
        if("" != transactionvsType) {
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#currencyTransactionTable").url = targetURL
    });

    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "${msg.searchResultLbl} '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#currencyTransactionTable").url = "${restURL}/transactionVS?searchText=" + textToSearch
    }

    function processSearchJSON(dataJSON) {
        document.querySelector("#currencyTransactionTable").params = dataJSON
        document.querySelector("#currencyTransactionTable").url = "${restURL}/transactionVS"
    }
</script>
