<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <r:require module="dynatableModule"/>
</head>
<body>
<div class="pageContenDiv">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="transactionPageTitle"/></li>
        </ol>
    </div>
    <button id="loadHistoryButton" type="button" class="btn btn-primary"
            style="margin: 10px 0px 10px 0px;display: none;">
        <g:message code="loadHistoryLbl"/>
        <i  id="loadHistoryButtonIcon" class="fa fa-refresh fa-spin" style="display: none;"></i>
    </button>

    <div style="display: table;width:100%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="transactionvsTypeSelect" style="margin:0px auto 0px auto;color:black; width: 400px;" class="form-control">
                <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>
                <option value="USER_ALLOCATION"> - <g:message code="selectUserAllocationLbl"/> - </option>
                <option value="USER_ALLOCATION_INPUT"> - <g:message code="selectUserAllocationInputLbl"/> - </option>
                <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
                <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
                <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
            </select>
        </div>
    </div>

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
        $("#navBarSearchInput").css( "visibility", "visible" );
        $('#transaction_table').dynatable({
                features: dynatableFeatures,
                inputs: dynatableInputs,
                params: dynatableParams,
                dataset: {
                    ajax: true,
                    ajaxUrl: "${createLink(controller: 'transaction', action: 'index')}",
                    ajaxOnLoad: false,
                    perPageDefault: 50,
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


        $('#transactionvsTypeSelect').on('change', function (e) {
            var transactionvsType = $(this).val()
            var optionSelected = $("option:selected", this);
            console.log("transactionvs selected: " + transactionvsType)
            var targetURL = "${createLink(controller: 'transaction', action: 'index')}";
            if("" != transactionvsType) targetURL = targetURL + "?transactionvsType=" + transactionvsType
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.paginationPage.set(1);
            dynatable.process();
        });

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

</r:script>