<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <r:require module="dynatableModule"/>
</head>
<body>
    <div style="width: 900px; margin: 0 auto 0 auto;">
        <button id="loadHistoryButton" type="button" class="btn btn-primary"
                style="margin: 10px 0px 10px 0px;display: none;">
            <g:message code="loadHistoryLbl"/>
            <i  id="loadHistoryButtonIcon" class="fa fa-refresh fa-spin" style="display: none;"></i>
        </button>

        <p id="pageInfoPanel" class="" style="margin: 20px 20px 20px 20px; font-size: 1.3em; background-color: #f9f9f9;"></p>

        <table class="table dynatable-vickets" id="transaction_table">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="type" style="width:210px;"><g:message code="typeLbl"/></th>
                <th data-dynatable-column="amount"><g:message code="amountLbl"/></th>
                <th data-dynatable-column="currency"><g:message code="currencyLbl"/></th>
                <th data-dynatable-column="dateCreated" style="width:170px;"><g:message code="datecreatedLbl"/></th>
                <th data-dynatable-column="subject" style="width:300px;"><g:message code="subjectLbl"/></th>
                <th><g:message code="voucherLbl"/></th>
            </tr>
            </thead>
        </table>
    </div>
</body>

</html>
<r:script>
    var dynatable

    $(function() {
        $('#appTitle').text("<g:message code="transactionPageTitle"/>")
        $('#transaction_table').dynatable({
                features: dynatableFeatures,
                inputs: dynatableInputs,
                params: dynatableParams,
                dataset: {
                    ajax: true,
                    ajaxUrl: "${createLink(controller: 'transaction', action: 'index')}",
                    ajaxOnLoad: false,
                    perPageDefault: 100,
                    records: []
                },
                writers: {
                    _rowWriter: rowWriter
                }
        });
        dynatable = $('#transaction_table').data('dynatable');
        dynatable.settings.params.records = '<g:message code="transactionRecordsLbl"/>'
        dynatable.settings.params.queryRecordCount = 'queryRecordCount'
        dynatable.settings.params.totalRecordCount = 'numTotalTransactions'

        $('#transaction_table').bind('dynatable:afterUpdate',  function() {
            console.log("page loaded")
            $('#dynatable-record-count-transaction_table').css('visibility', 'visible');
        })

        $("#transaction_table").stickyTableHeaders({fixedOffset: $('.navbar')});
    })

    $("#loadHistoryButton").click(function() {
    });

    function loadHTTPTransactions()  {
        var xmlHttp = new XMLHttpRequest();
        xmlHttp.open( "GET", "${createLink(controller: 'transaction', action: 'index')}", true );
        $('#loadHistoryButtonIcon').show()
        $("#loadHistoryButton").prop("disabled", true);

        xmlHttp.onreadystatechange=function() {
            if (xmlHttp.readyState==4 && xmlHttp.status == 200) {
                var jsonResult = JSON.parse(xmlHttp.responseText);
                dynatable.records.updateFromJson({Transacciones: jsonResult.Transacciones});
                dynatable.records.init();
                dynatable.process();
                $('#loadHistoryButtonIcon').hide()
                $("#loadHistoryButton").prop("disabled", false);
            }
        }
        xmlHttp.send();
    }

    function addRecordToTable(record)  {
        //dynatable.settings.dataset.originalRecords.push(record);
        dynatable.settings.dataset.records.push(record);
        dynatable.process();
    }

    socketService.socket.onmessage = function (message) {
        console.log('socket.onmessage - message.data: ' + message.data);
        var messageJSON = toJSON(message.data)
        addRecordToTable(messageJSON)
    }

    socketService.socket.onopen = function () {
        console.log('listener - WebSocket connection opened');
        socketService.sendMessage({operation:SocketOperation.LISTEN_TRANSACTIONS, locale:navigator.language})
    };

    function rowWriter(rowIndex, jsonTransactionData, columns, cellWriter) {
        var cssClass = "span4", tr;
        if (rowIndex % 3 === 0) { cssClass += ' first'; }
        tr = '<tr><td title="' + jsonTransactionData.type + '">' + jsonTransactionData.type + '</td><td>' +
        jsonTransactionData.amount + '</td>' +
        '<td>' + jsonTransactionData.currency + '</td><td>' + jsonTransactionData.dateCreated +
        '</td><td title="' + jsonTransactionData.subject + '">' + jsonTransactionData.subject + '</td><td>' + '<a href="#" onclick="openWindow(\'' +
        jsonTransactionData.messageSMIMEURL+ '\')"><g:message code="proofLbl"/></a></td></tr>'
        return tr
    }

    function processUserSearch(textToSearch) {
        $("#pageInfoPanel").text("<g:message code="searchResultLbl"/> '" + textToSearch + "'")
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'transaction', action: 'index')}?searchParam=" + textToSearch
        dynatable.process();
    }

</r:script>