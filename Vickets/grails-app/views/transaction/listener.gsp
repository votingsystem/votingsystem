<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <link rel="import" href="${resource(dir: '/bower_components/vicket-transaction-table', file: 'vicket-transaction-table.html')}">
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="transactionPageTitle"/></li>
        </ol>
    </div>

    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="transactionvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                    class="form-control" onchange="transactionvsTypeSelect(this)">
                <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>
                <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
                <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
                <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
            </select>
        </div>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <vicket-transaction-table id="recordList" url="${createLink(controller: 'transaction', action: 'index')}"></vicket-transaction-table>

</div>
</body>

</html>
<asset:script>
    var socketService = new SocketService()
    socketService.connect()
    socketService.socket.onmessage = function (message) {
        console.log('socket.onmessage - message.data: ' + message.data);
        var messageJSON = toJSON(message.data)
        document.querySelector("#recordList").newRecord = messageJSON
    }

    socketService.socket.onopen = function () {
        console.log('listener - WebSocket connection opened');
        socketService.sendMessage({operation:Operation.LISTEN_TRANSACTIONS, locale:navigator.language})
    };

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