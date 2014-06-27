<div id="balance_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
    <table class="table white_headers_table" id="balance_table" style="">
        <thead>
        <tr style="color: #ff0000;">
            <th style="max-width:80px;"><g:message code="tagLbl"/></th>
            <th style="max-width:80px;"><g:message code="amountLbl"/></th>
            <th style="width: 120px;"><g:message code="currencyLbl"/></th>
            <th style="width:200px;"><g:message code="lastUpdateLbl"/></th>
        </tr>
        </thead>
        <tbody></tbody>
    </table>
</div>
<asset:script>

    function loadHTTPBalanceList (balanceListURL) {
        console.log("getHTTPRequest - tagetURL: " + balanceListURL)
        var request = new XMLHttpRequest();
        request.open('GET', balanceListURL, true);
        request.onload = function (e) {
            if (request.readyState === 4) {
                if (request.status === 200) {// Check if the get was successful.
                    var responseJSON = toJSON(request.responseText)
                    var tableRef = document.getElementById('balance_table').getElementsByTagName('tbody')[0];
                    responseJSON.accounts.forEach(function (it) {
                        var newRow   = tableRef.insertRow(tableRef.rows.length);
                        var newCellTag  = newRow.insertCell(0);
                        newCellTag.classList.add('text-center');
                        if(it.tag != null) newCellTag.innerHTML = it.tag.name
                        var newCellAmount  = newRow.insertCell(1);
                        newCellAmount.classList.add('text-center');
                        newCellAmount.innerHTML = it.amount.toFixed(2)
                        var newCellCurrency  = newRow.insertCell(2);
                        newCellCurrency.classList.add('text-center');
                        newCellCurrency.innerHTML = it.currency
                        var newCellLastUpdated  = newRow.insertCell(3);
                        newCellLastUpdated.classList.add('text-center');
                        newCellLastUpdated.innerHTML = it.lastUpdated
                    });
                } else console.error(request.statusText);
            } else console.error(request.statusText);
        };
        request.send(null);
    }

    loadHTTPBalanceList(balanceListURL)
</asset:script>