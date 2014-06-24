<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <asset:stylesheet src="jquery.dynatable.css"/>
    <asset:javascript src="jquery.dynatable.js"/>
    <asset:javascript src="jquery.stickytableheaders.js"/>
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="reportsPageTitle"/></li>
        </ol>
    </div>


    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <div id="record_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
        <table class="table white_headers_table" id="record_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="date" style="width: 220px;"><g:message code="dateLbl"/></th>
                <th data-dynatable-column="message" style="max-width:80px;"><g:message code="messageLbl"/></th>
                <!--<th data-dynatable-no-sort="true"><g:message code="voucherLbl"/></th>-->
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
        $('#record_table').dynatable({
                features: dynatableFeatures,
                inputs: dynatableInputs,
                params: dynatableParams,
                dataset: {
                    ajax: true,
                    ajaxUrl: "${createLink(controller: 'reports', action: 'index')}",
                    ajaxOnLoad: false,
                    records: []
                },
                writers: {
                    _rowWriter: rowWriter
                }
        });
        dynatable = $('#record_table').data('dynatable');
        dynatable.settings.params.records = 'records'
        dynatable.settings.params.queryRecordCount = 'numTotalRecords'


        $('#record_table').bind('dynatable:afterUpdate',  function() {
            console.log("page loaded")
            $('#dynatable-record-count-record_table').css('visibility', 'visible');
        })

        //$("#record_table").stickyTableHeaders({fixedOffset: $('.navbar')});
        $("#record_table").stickyTableHeaders();

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

    function addRecordToTable(record)  {
        //dynatable.settings.dataset.originalRecords.push(record);
        dynatable.settings.dataset.records.push(record);
        dynatable.process();
    }


    function rowWriter(rowIndex, reportsData, columns, cellWriter) {
        tr = '<tr><td class="text-center">' + reportsData.date + '</td><td class="text-center">' + reportsData.message + '</td></tr>'
        return tr
    }

</asset:script>
<asset:deferredScripts/>