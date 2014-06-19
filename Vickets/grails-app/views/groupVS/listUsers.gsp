<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <asset:javascript src="jquery.dynatable.js"/>
    <asset:stylesheet src="jquery.dynatable.css"/>
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
                <li class="active"><g:message code="groupvsUserListLbl"/></li>
            </ol>
        </ol>
    </div>


    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <h3><div class="pageHeader text-center">
        <g:message code="groupvsUserListPageHeader"/> '${subscriptionMap?.groupName}'</div>
    </h3>

    <div id="uservs_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
        <table class="table white_headers_table" id="uservs_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="nif" style="width: 120px;"><g:message code="nifLbl"/></th>
                <th data-dynatable-column="name" style="max-width:80px;"><g:message code="nameLbl"/></th>
                <th data-dynatable-column="state" style="max-width:60px;"><g:message code="stateLbl"/></th>
                <th data-dynatable-column="lastUpdate" style="width:200px;"><g:message code="lastUpdateLbl"/></th>
            </tr>
            </thead>
        </table>
    </div>
</div>
</body>

</html>
<asset:script>

    var dynatable

    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
        $('#uservs_table').dynatable({
                features: dynatableFeatures,
                inputs: dynatableInputs,
                params: dynatableParams,
                dataset: {
                    ajax: true,
                    ajaxUrl: "${createLink(controller: 'groupVS', action: 'listUsers')}/${subscriptionMap?.id}",
                    ajaxOnLoad: false,
                    perPageDefault: 50,
                    records: []
                },
                writers: {
                    _rowWriter: rowWriter
                }
        });
        dynatable = $('#uservs_table').data('dynatable');
        dynatable.settings.params.records = '<g:message code="uservsRecordsLbl"/>'
        dynatable.settings.params.queryRecordCount = 'numTotalUsers'
        dynatable.settings.params.totalRecordCount = 'numTotalUsers'


        $('#uservs_table').bind('dynatable:afterUpdate',  function() {
            console.log("page loaded")
            $('#dynatable-record-count-uservs_table').css('visibility', 'visible');
            updateMenuLinks()
        })

        //$("#uservs_table").stickyTableHeaders({fixedOffset: $('.navbar')});
        $("#uservs_table").stickyTableHeaders();


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


    function rowWriter(rowIndex, jsonSubscriptionData, columns, cellWriter) {
        var transactionType
        var userURL = getMenuURL("user/" + jsonSubscriptionData.uservs.id + "?mode=details")

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

        var cssClass = "span4", tr;
        if (rowIndex % 3 === 0) { cssClass += ' first'; }
        tr = '<tr><td class="text-center"><a href="#" onclick="openWindow(\'' + userURL + '\')">' + jsonSubscriptionData.uservs.NIF + '</a></td>' +
            '<td class="text-center">' + jsonSubscriptionData.uservs.name + '</td>' +
            '<td class="text-center">' + userState + '</td>' +
            '<td class="text-center">' + jsonSubscriptionData.lastUpdated + '</td></tr>'
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