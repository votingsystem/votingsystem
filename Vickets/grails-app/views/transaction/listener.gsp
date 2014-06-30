<!DOCTYPE html>
<html>
<head>
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <script type="text/javascript" src="${resource(dir: 'bower_components/dynatable', file: 'jquery.dynatable.js')}"></script>
    <asset:stylesheet src="jquery.dynatable.css"/>
    <meta name="layout" content="main" />
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
            <select id="transactionvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control">
                <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>
                <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
                <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
                <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
            </select>
        </div>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <div id="transaction_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
        <table class="table white_headers_table" id="transaction_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="type" style="width: 280px;"><g:message code="typeLbl"/></th>
                <th data-dynatable-column="amount" style="width:150px;"><g:message code="amountLbl"/></th>
                <th data-dynatable-column="dateCreated" style="width:170px;"><g:message code="dateLbl"/></th>
                <th data-dynatable-column="subject" style="min-width:300px;"><g:message code="subjectLbl"/></th>
                <!--<th data-dynatable-no-sort="true"><g:message code="voucherLbl"/></th>-->
            </tr>
            </thead>
        </table>
    </div>
</div>
</body>

</html>
<asset:script>
    var socketService = new SocketService()

    function connectToWs() {

        socketService.connect()

        socketService.socket.onmessage = function (message) {
            console.log('socket.onmessage - message.data: ' + message.data);
            var messageJSON = toJSON(message.data)
            addRecordToTable(messageJSON)
        }

        socketService.socket.onopen = function () {
            console.log('listener - WebSocket connection opened');
            socketService.sendMessage({operation:Operation.LISTEN_TRANSACTIONS, locale:navigator.language})
        };
    }

var dynatable

$(function() {
    connectToWs();
    $("#navBarSearchInput").css( "visibility", "visible" );
    $('#transaction_table').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            dataset: {
                ajax: true,
                ajaxUrl: "${createLink(controller: 'transaction', action: 'index')}",
                    ajaxOnLoad: true,
                    perPageDefault: 50,
                    records: []
                },
                writers: { _rowWriter: rowWriter1 }
        });
        dynatable = $('#transaction_table').data('dynatable');
        dynatable.settings.params.records = 'transactionRecords'
        dynatable.settings.params.queryRecordCount = 'queryRecordCount'
        dynatable.settings.params.totalRecordCount = 'numTotalTransactions'

        $('#transaction_table').bind('dynatable:afterUpdate',  function() {
            console.log("page loaded")
            $('#dynatable-record-count-transaction_table').css('visibility', 'visible');
        })

        //$("#transaction_table").stickyTableHeaders({fixedOffset: $('.navbar')});
        $("#transaction_table").stickyTableHeaders();

        $('#transactionvsTypeSelect').on('change', function (e) {
            var transactionvsType = $(this).val()
            console.log("transactionvs selected: " + transactionvsType)
            var targetURL = "${createLink(controller: 'transaction', action: 'index')}";
            if("" != transactionvsType) {
                history.pushState(null, null, targetURL);
                targetURL = targetURL + "?transactionvsType=" + transactionvsType
            }
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.paginationPage.set(1);
            dynatable.process();
        });

    })

    function addRecordToTable(record)  {
        //dynatable.settings.dataset.originalRecords.push(record);
        dynatable.settings.dataset.records.push(record);
        dynatable.process();
    }

    function rowWriter1(rowIndex, jsonTransactionData, columns, cellWriter) {
        var transactionType = getTransactionVSDescription(jsonTransactionData.type)
        var transactionURL = jsonTransactionData.id
        var amount = jsonTransactionData.amount + " " + jsonTransactionData.currency
        tr = '<tr><td title="' + transactionType + '" class="text-center">' +
            '<a href="#" onclick="openWindow(\'' + transactionURL + '\')">' + transactionType + '</a></td><td class="text-right" style="">' +
            amount + '</td><td class="text-center">' + jsonTransactionData.dateCreated +
        '</td><td title="' + jsonTransactionData.subject + '" class="text-center">' + jsonTransactionData.subject + '</td></tr>'
        return tr
    }

    function processUserSearch(textToSearch) {
        $("#pageInfoPanel").text("<g:message code="searchResultLbl"/> '" + textToSearch + "'")
        $('#pageInfoPanel').css("display", "block")
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'transaction', action: 'index')}?searchText=" + textToSearch
        dynatable.paginationPage.set(1);
        dynatable.process();
    }

    function processUserSearchJSON(jsonData) {
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'transaction', action: 'index')}"
        dynatable.settings.dataset.ajaxData = jsonData
        dynatable.paginationPage.set(1);
        dynatable.process();
    }


</asset:script>