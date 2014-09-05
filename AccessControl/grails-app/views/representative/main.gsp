<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/search-info.gsp']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-advanced-search-dialog', file: 'votingsystem-advanced-search-dialog.html')}">
</head>
<body>
<div style="margin: 15px 0 0 0;">
    <search-info id="searchInfo"></search-info>

    <div class="" style="display: table; margin: auto; width: 100%;">
        <ul id="representativeList" style="display: block; width: 100%; position: relative;margin: auto;" ></ul>
    </div>
</div>

<template id="representativeTemplate" style="display:none;">
    <g:render template="/template/representative"/>
</template>
<votingsystem-advanced-search-dialog id="advancedSearchDialog"></votingsystem-advanced-search-dialog>
</body>
</html>
<asset:script>
       	
    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
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
        updateMenuLinks()
        $('.representativeDiv').click(function() {
            window.location.href = $(this).attr('data-href')
    })})

    function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        document.querySelector("#searchInfo").show(textToSearch, dateBeginFrom, dateBeginTo)
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'search', action: 'representative')}?searchText=" + textToSearch
        dynatable.process();
    }

</asset:script>