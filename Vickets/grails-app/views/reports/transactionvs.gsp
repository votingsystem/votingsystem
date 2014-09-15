<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-transactionvs-table']"/>">
</head>
<body>
<div class="pageContentDiv">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li class="active"><g:message code="transactionPageTitle"/></li>
    </ol>

    <div layout horizontal center center-justified>
        <select id="transactionvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                 onchange="transactionvsTypeSelect(this)">
            <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>

            <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
            <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
            <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
        </select>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <vicket-transactionvs-table id="recordList" url="${createLink(controller: 'reports', action: 'transactionvs')}"></vicket-transactionvs-table>

</div>
</body>

</html>
<asset:script>
    function transactionvsTypeSelect(selected) {
        var transactionvsType = selected.value
        console.log("transactionvsType: " + transactionvsType)
        if("" != transactionvsType) {
            targetURL = "${createLink(controller: 'reports', action: 'transactionvs')}";
            history.pushState(null, null, targetURL);
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
            document.querySelector("#recordList").url = targetURL
        }
    }

    function processSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#recordList").url = "${createLink(controller: 'transaction', action: 'index')}?searchText=" + textToSearch
    }

    function processSearchJSON(jsonData) {
        document.querySelector("#recordList").params = jsonData
        document.querySelector("#recordList").url = "${createLink(controller: 'transaction', action: 'index')}"
    }
</asset:script>
<asset:deferredScripts/>