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

    <transactionvs-table id="recordList" url="${createLink(controller: 'reports', action: 'transactionvs')}"></transactionvs-table>

</div>
</body>

</html>
<asset:script>
    document.querySelector("#coreSignals").addEventListener('core-signal-transactionvs-selector-selected', function(e) {
        var transactionvsType = e.detail
        console.log("transactionvsType: " + transactionvsType)
        targetURL = "${createLink(controller: 'reports', action: 'transactionvs')}";
        if("" != transactionvsType) {
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#recordList").url = targetURL
    });

    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#recordList").url = "${createLink(controller: 'transactionVS', action: 'index')}?searchText=" + textToSearch
    }

    function processSearchJSON(jsonData) {
        document.querySelector("#recordList").params = jsonData
        document.querySelector("#recordList").url = "${createLink(controller: 'transactionVS', action: 'index')}"
    }
</asset:script>
<asset:deferredScripts/>