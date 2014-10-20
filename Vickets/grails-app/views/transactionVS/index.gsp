<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-table']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-selector']"/>">
</head>
<body>
<vs-innerpage-signal title="<g:message code="transactionPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div layout horizontal center center-justified>
        <transactionvs-selector id="transactionSelector"></transactionvs-selector>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <transactionvs-table id="vicketTransactionTable" transactionsMap="${transactionsMap as grails.converters.JSON}"/>
</div>
</body>

</html>
<asset:script>

    if(typeof sendSocketVSMessage != 'undefined') {
        document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-new', function(e) {
            console.log("listener.gsp - core-signal-transactionvs-new")
            document.querySelector("#vicketTransactionTable").addTransaction(e.detail)
        });
        sendSocketVSMessage({operation:Operation.LISTEN_TRANSACTIONS})
    } else console.log("listener.gsp - no socket service available")

    document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-selector-selected', function(e) {
        var transactionvsType = e.detail
        console.log("transactionvsType: " + transactionvsType)
        targetURL = "${createLink(controller: 'transactionVS', action: 'index')}";
        if("" != transactionvsType) {
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#vicketTransactionTable").url = targetURL
    });

    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#vicketTransactionTable").url = "${createLink(controller: 'transactionVS', action: 'index')}?searchText=" + textToSearch
    }

    function processSearchJSON(jsonData) {
        document.querySelector("#vicketTransactionTable").params = jsonData
        document.querySelector("#vicketTransactionTable").url = "${createLink(controller: 'transactionVS', action: 'index')}"
    }
</asset:script>
<asset:deferredScripts/>