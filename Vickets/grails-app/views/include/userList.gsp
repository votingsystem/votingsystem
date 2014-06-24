<div id="uservs_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
    <table class="table white_headers_table" id="uservs_table" style="">
        <thead>
        <tr style="color: #ff0000;">
            <th data-dynatable-column="nif" style="width: 120px;"><g:message code="nifLbl"/></th>
            <th data-dynatable-column="name" style="max-width:80px;">IBAN</th>
            <th data-dynatable-column="name" style="max-width:80px;"><g:message code="nameLbl"/></th>
            <th data-dynatable-column="state" style="max-width:60px;"><g:message code="stateLbl"/></th>
            <th data-dynatable-column="lastUpdate" style="width:200px;"><g:message code="lastUpdateLbl"/></th>
        </tr>
        </thead>
    </table>
</div>
<asset:script>
    var userListDynatable

    $(function() {
        $('#uservs_table').dynatable({
                features: dynatableFeatures,
                inputs: dynatableInputs,
                params: dynatableParams,
                dataset: {
                    ajax: true,
                    ajaxUrl: userListURL,
                    ajaxOnLoad: false,
                    perPageDefault: 50,
                    records: []
                },
                writers: { _rowWriter: userListRowWriter }
        });
        userListDynatable = $('#uservs_table').data('dynatable');
        userListDynatable.settings.params.records = 'userVSList'
        userListDynatable.settings.params.queryRecordCount = 'numTotalUsers'
        userListDynatable.settings.params.totalRecordCount = 'numTotalUsers'

        $('#uservs_table').bind('dynatable:afterUpdate',  function() {
            console.log("page loaded")
            $('#dynatable-record-count-uservs_table').css('visibility', 'visible');
            updateMenuLinks()
        })
        $("#uservs_table").stickyTableHeaders();
    })

    function userListRowWriter(rowIndex, jsonSubscriptionData, columns, cellWriter) {
        var userURL = userBaseURL + "/" + jsonSubscriptionData.uservs.id + "?mode=details&menu=" + getParameterByName('menu')

        var userState
        switch(jsonSubscriptionData.state) {
            case 'ACTIVE':
                userState = '<g:message code="activeUserLbl"/>'
                break;
            case 'PENDING':
                userState = '<g:message code="pendingUserLbl"/>'
                break;
            case 'CANCELLED':
                userState = '<g:message code="cancelledUserLbl"/>'
                break;
            default:
                userState = jsonSubscriptionData.state
        }
        tr = '<tr><td class="text-center"><a href="#" onclick="openWindow(\'' + userURL + '\')">' + jsonSubscriptionData.uservs.NIF + '</a></td>' +
            '<td class="text-center">' + jsonSubscriptionData.uservs.IBAN + '</td>' +
            '<td class="text-center">' + jsonSubscriptionData.uservs.name + '</td>' +
            '<td class="text-center">' + userState + '</td>' +
            '<td class="text-center">' + jsonSubscriptionData.lastUpdated + '</td></tr>'
        return tr
    }
</asset:script>