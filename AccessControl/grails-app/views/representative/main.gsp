<html>
<head>
    <meta name="layout" content="main" />
    <r:require module="dynatableModule"/>
</head>
<body>
<div class="mainPage" style="margin:0 0 0 0;">
    <ul id="representativeList" style="display: block; width: 100%; position: relative;" class="row"></ul>
</div>

<template id="representativeTemplate" style="display:none;">
    <g:render template="/template/representative"/>
</template>
</body>
</html>
<r:script>
       	
    $(function() {
        dynatableInputs.processingText = '<span class="dynatableLoading" style="position: relative; top:100px;">' +
         '<g:message code="updatingLbl"/> <i class="fa fa-refresh fa-spin"></i></span>'
        $('#representativeList').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            table: {
                bodyRowSelector: 'li'
            },
            dataset: {
                ajax: true,
                ajaxUrl: "${createLink(controller: 'representative', action: 'index')}",
                ajaxOnLoad: false,
                perPageDefault: 50,
                records: []
            },
            writers: {
                _rowWriter: representativeWriter
            }
        });

        dynatable = $('#representativeList').data('dynatable');
        dynatable.settings.params.records = 'representatives'
        dynatable.settings.params.queryRecordCount = 'numRepresentatives'
        dynatable.settings.params.totalRecordCount = 'numTotalRepresentatives'
     });

    function representativeWriter(rowIndex, jsonAjaxData, columns, cellWriter) {
        //var dataStr = JSON.stringify(jsonAjaxData);
        //console.log("representative: " + dataStr);
        var targetURL = "${createLink( controller:'representative')}/" + jsonAjaxData.id
        var newRepresentativeTemplate = $('#representativeTemplate').html()
        var endTime = Date.parse(jsonAjaxData.dateFinish)

        var newRepresentativeHTML = newRepresentativeTemplate.format(targetURL, jsonAjaxData.imageURL,
                jsonAjaxData.firstName + " " + jsonAjaxData.lastName, jsonAjaxData.numRepresentations);
        return newRepresentativeHTML
    }

    $('#representativeList').bind('dynatable:afterUpdate',  function() {
        $('.representativeDiv').click(function() {
            window.location.href = $(this).attr('href')
    })})

</r:script>