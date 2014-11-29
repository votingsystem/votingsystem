<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/transactionVS/transactionvs-list"/>
    <vs:webcomponent path="/transactionVS/transactionvs-selector"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="transactionPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div layout horizontal center center-justified>
        <transactionvs-selector id="transactionSelector" transactionvsType="${params.transactionvsType}"></transactionvs-selector>
    </div>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <transactionvs-list id="cooinTransactionTable" transactionsMap='${transactionsMap}'></transactionvs-list>
</div>
</body>

</html>
<asset:script>

    if(typeof sendSocketVSMessage != 'undefined') {
        document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-new', function(e) {
            console.log("listener.gsp - core-signal-transactionvs-new")
            document.querySelector("#cooinTransactionTable").addTransaction(e.detail)
        });
        sendSocketVSMessage({operation:Operation.LISTEN_TRANSACTIONS})
    } else console.log("listener.gsp - no socket service available")

    document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-selector-selected', function(e) {
        var transactionvsType = e.detail
        console.log("index.gsp - transactionvsType: " + transactionvsType)
        targetURL = "${createLink(controller: 'transactionVS', action: 'index')}";
        if("" != transactionvsType) {
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#cooinTransactionTable").url = targetURL
    });

    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#cooinTransactionTable").url = "${createLink(controller: 'transactionVS', action: 'index')}?searchText=" + textToSearch
    }

    function processSearchJSON(jsonData) {
        document.querySelector("#cooinTransactionTable").params = jsonData
        document.querySelector("#cooinTransactionTable").url = "${createLink(controller: 'transactionVS', action: 'index')}"
    }
</asset:script>
<asset:deferredScripts/>