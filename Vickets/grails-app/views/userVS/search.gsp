<!DOCTYPE html>
<html>
<head>
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <asset:javascript src="jquery.dynatable.js"/>
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
                <option value="USER_ALLOCATION"> - <g:message code="selectUserAllocationLbl"/> - </option>
                <option value="USER_ALLOCATION_INPUT"> - <g:message code="selectUserAllocationInputLbl"/> - </option>
                <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
                <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
                <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
            </select>
        </div>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <div id="transaction_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
        <table class="table dynatable-vickets" id="transaction_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="type" style="width: 220px;"><g:message code="typeLbl"/></th>
                <th data-dynatable-column="nif" style="max-width:80px;"><g:message code="nifLbl"/></th>
                <th data-dynatable-column="IBAN" style="max-width:60px;"><g:message code="IBANLbl"/></th>
                <th data-dynatable-column="dateCreated" style="width:170px;"><g:message code="datecreatedLbl"/></th>
                <th data-dynatable-column="name" style="min-width:200px;"><g:message code="nameLbl"/></th>
                <th data-dynatable-column="firstName" style="min-width:200px;"><g:message code="firstNameLbl"/></th>
                <th data-dynatable-column="lastName" style="min-width:200px;"><g:message code="lastNameLbl"/></th>
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
    socketService.connect()

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

        //$("#transaction_table").stickyTableHeaders({fixedOffset: $('.navbar')});
        $("#transaction_table").stickyTableHeaders();

        $('#transactionvsTypeSelect').on('change', function (e) {
            var transactionvsType = $(this).val()
            var optionSelected = $("option:selected", this);
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

    function loadHTTPTransactions()  {
        var xmlHttp = new XMLHttpRequest();
        xmlHttp.open( "GET", "${createLink(controller: 'transaction', action: 'index')}", true );

        xmlHttp.onreadystatechange=function() {
            if (xmlHttp.readyState==4 && xmlHttp.status == 200) {
                var jsonResult = JSON.parse(xmlHttp.responseText);
                dynatable.records.updateFromJson({Transacciones: jsonResult.Transacciones});
                dynatable.records.init();
                dynatable.process();
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
        var transactionType
        switch(jsonTransactionData.type) {
            case 'VICKET_SEND':
                transactionType = '<g:message code="selectVicketSendLbl"/>'
                break;
            case 'USER_ALLOCATION':
                transactionType = '<g:message code="selectUserAllocationLbl"/>'
                break;
            case 'USER_ALLOCATION_INPUT':
                transactionType = '<g:message code="selectUserAllocationInputLbl"/>'
                break;
            case 'VICKET_REQUEST':
                transactionType = '<g:message code="selectVicketRequestLbl"/>'
                break;
            case 'VICKET_CANCELLATION':
                transactionType = '<g:message code="selectVicketCancellationLbl"/>'
                break;
            default:
                transactionType = jsonTransactionData.type
        }
        var transactionURL = jsonTransactionData.id

        var cssClass = "span4", tr;
        if (rowIndex % 3 === 0) { cssClass += ' first'; }
        tr = '<tr><td title="' + transactionType + '" class="text-center">' +
            '<a href="#" onclick="openWindow(\'' + transactionURL + '\')">' + transactionType + '</a></td><td class="text-center">' +
            jsonTransactionData.amount + '</td>' +
        '<td class="text-center">' + jsonTransactionData.currency + '</td><td class="text-center">' + jsonTransactionData.dateCreated +
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