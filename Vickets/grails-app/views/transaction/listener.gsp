<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-transaction-table', file: 'votingsystem-transaction-table.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-socket', file: 'votingsystem-socket.html')}">
</head>
<body>
<div class="pageContenDiv">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li class="active"><g:message code="transactionPageTitle"/></li>
    </ol>

    <div layout horizontal center center-justified>
        <select id="transactionvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                class="form-control" onchange="transactionvsTypeSelect(this)">
            <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>
            <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
            <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
            <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
        </select>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <votingsystem-transaction-table id="recordList" url="${createLink(controller: 'transaction', action: 'index', absolute: true)}"></votingsystem-transaction-table>

    <votingsystem-socket id="wssocket" url="${grailsApplication.config.webSocketURL}"></votingsystem-socket>
</div>
</body>

</html>
<asset:script>

    document.addEventListener('polymer-ready', function() {
        document.querySelector("#wssocket").addEventListener('on-message', function (e) {
            document.querySelector("#recordList").newRecord = e.detail
        })
        document.querySelector("#wssocket").sendMessage(JSON.stringify({operation:Operation.LISTEN_TRANSACTIONS, locale:navigator.language}))
    });

function transactionvsTypeSelect(selected) {
    var transactionvsType = selected.value
    console.log("transactionvsType: " + transactionvsType)
    targetURL = "${createLink(controller: 'transaction', action: 'index')}";
        if("" != transactionvsType) {
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
        }
        history.pushState(null, null, targetURL);
        document.querySelector("#recordList").url = targetURL
    }

    function processUserSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#recordList").url = "${createLink(controller: 'transaction', action: 'index')}?searchText=" + textToSearch
    }

    function processUserSearchJSON(jsonData) {
        document.querySelector("#recordList").params = jsonData
        document.querySelector("#recordList").url = "${createLink(controller: 'transaction', action: 'index')}"
    }
</asset:script>