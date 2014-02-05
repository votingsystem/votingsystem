<!DOCTYPE html>
<html>
<head>
    <title><g:message code="transactionListenerPageTitle"/></title>
    <r:external uri="/images/euro_16.png"/>
    <r:require module="application"/>
    <r:layoutResources />
</head>
<body>
    <div id="simulationRunningDialog" style="padding: 20px;">
        <table class="transaction_table" id="transaction_table">
            <tr style="width: 100%;">
                <th><g:message code="typeLbl"/></th>
                <th><g:message code="amountLbl"/></th>
                <th><g:message code="currencyLbl"/></th>
                <th style="width:200px;"><g:message code="datecreatedLbl"/></th>
                <th style="width:300px;"><g:message code="subjectLbl"/></th>
                <th style="width:200px;"><g:message code="voucherLbl"/></th>
            </tr>
        </table>
    </div>
    <table style="display:none;">
        <tbody id="transactionRowTemplate">
            <tr style="width: 100%;">
                <td>{0}</td>
                <td>{1}</td>
                <td>{2}</td>
                <td style="width:200px;">{3}</td>
                <td style="width:300px;">{4}</td>
                <td style="width:200px;"><a href="{5}">Justificante</a></td>
            </tr>
        </tbody>
    </table>
</body>

</html>
<r:script>

    socketService.socket.onmessage = function (message) {
        console.log('socket.onmessage - message.data: ' + message.data);
        var messageJSON = toJSON(message.data)
        addTransactionRow(messageJSON)
    }

    socketService.socket.onopen = function () {
        console.log('listener - WebSocket connection opened');
        socketService.sendMessage({operation:SocketOperation.LISTEN_TRANSACTIONS, locale:navigator.language})
    };

    function addTransactionRow (jsonTransactionData) {
        if(jsonTransactionData == null) return
        var newTransactionRowTemplate = $('#transactionRowTemplate').html()
        var fromUser = (jsonTransactionData.fromUserVS)? jsonTransactionData.fromUserVS.nif:""
        var newTransactionHTML = newTransactionRowTemplate.format(jsonTransactionData.type,
                jsonTransactionData.amount, jsonTransactionData.currency,
                jsonTransactionData.dateCreated, jsonTransactionData.subject, jsonTransactionData.messageSMIMEURL);
        $('#transaction_table tr:first').after(newTransactionHTML).fadeIn("slow");
    }

</r:script>
<r:layoutResources />