<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <r:require module="dynatableModule"/>
</head>
<body>

    <button id="loadHistoryButton" type="button" class="btn btn-primary" style="margin: 10px 0px 10px 0px;">
        Cargar histórico de transacciones <i  id="loadHistoryButtonIcon" class="fa fa-refresh fa-spin" style="display: none;"></i></button>
    <div class="tableContainer">
        <table class="table" id="transaction_table">
            <thead>
            <tr style="color: #ff0000;">
                <th  style="width:190px;"><g:message code="typeLbl"/></th>
                <th><g:message code="amountLbl"/></th>
                <th><g:message code="currencyLbl"/></th>
                <th style="width:170px;"><g:message code="datecreatedLbl"/></th>
                <th style="width:300px;"><g:message code="subjectLbl"/></th>
                <th><g:message code="voucherLbl"/></th>
            </tr>
            </thead>
        </table>
    </div>

</body>

</html>
<r:script>
 var dynatable
 var transaction_tableWidth

    $(function() {
        $('#appTitle').text("<g:message code="transactionPageTitle"/>")

        $('#transaction_table').dynatable({
                features: dynatableFeatures,
                inputs: dynatableInputs,
                params: dynatableParams,
                dataset: {
                    ajax: true,
                    ajaxUrl: '/Tickets/transaction/index',
                    ajaxOnLoad: true,
                    records: []
                },
                writers: {
                    _rowWriter: rowWriter
                }
        });
        dynatable = $('#transaction_table').data('dynatable');
        $('#transaction_table').bind('dynatable:ajax:success',  function() {
            transaction_tableWidth = $("#transaction_table").outerWidth() + "px"
        })
        $("#transaction_table").fixMe();

    })

    $("#loadHistoryButton").click(function() {
        var xmlHttp = new XMLHttpRequest();
        xmlHttp.open( "GET", "${createLink(controller: 'transaction', action: 'index')}", true );
        $('#loadHistoryButtonIcon').show()
        $("#loadHistoryButton").prop("disabled", true);

        xmlHttp.onreadystatechange=function() {
            if (xmlHttp.readyState==4 && xmlHttp.status == 200) {
                var jsonResult = JSON.parse(xmlHttp.responseText);

                dynatable.records.updateFromJson({Transacciones: jsonResult});
                dynatable.records.init();
                dynatable.process();
		        transaction_tableWidth = $("#transaction_table").outerWidth() + "px"

                $('#loadHistoryButtonIcon').hide()
                $("#loadHistoryButton").prop("disabled", false);
            }
        }
        xmlHttp.send();
    });

    function addRecordToTable(record)  {
        dynatable.settings.dataset.originalRecords.push(record);
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
        '</td><td>' + jsonTransactionData.subject + '</td><td>' +
        '<a href="#" onclick="openWindow(\'' + jsonTransactionData.messageSMIMEURL+ '\')"><g:message code="proofLbl"/></a></td></tr>'
                return tr
            }

    //Fixed table header to allow scrolling large tables
    (function($) {
        $.fn.fixMe = function() {
          return this.each(function() {
             var $this = $(this), $t_fixed;
             function init() {
                $this.wrap('<div class="tableContainer" />');
                $t_fixed = $this.clone();
                $t_fixed.find("tbody").remove().end().addClass("fixed").insertBefore($this);
                resizeFixed();
             }
             function resizeFixed() {
                $t_fixed.find("th").each(function(index) {
                   $(this).css("width",$this.find("th").eq(index).outerWidth() + "px");
                });
             }
             function scrollFixed() {
                var offset = $(this).scrollTop(),
                tableOffsetTop = $this.offset().top,
                tableOffsetBottom = tableOffsetTop + $this.height() - $this.find("thead").height();
                if(offset < tableOffsetTop || offset > tableOffsetBottom)
                   $t_fixed.hide();
                else if(offset >= tableOffsetTop && offset <= tableOffsetBottom && $t_fixed.is(":hidden"))
                   $t_fixed.show();
                $("#transaction_table").css("width", transaction_tableWidth);
             }
             $(window).resize(resizeFixed);
             $(window).scroll(scrollFixed);
             init();
          });
       };
    })(jQuery);
</r:script>