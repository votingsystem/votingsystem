<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-advanced-search-dialog', file: 'votingsystem-advanced-search-dialog.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/search-info.gsp']"/>">
</head>
<body>
<div class="">
    <div style="display: table;width:100%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="eventsStateSelect" style="margin:0px auto 0px auto;color:black; width: 300px;" class="form-control">
                <option value="" style="color:black;"> - <g:message code="selectPollsLbl"/> - </option>
                <option value="ACTIVE" style="color:#388746;"> - <g:message code="selectOpenPollsLbl"/> - </option>
                <option value="AWAITING" style="color:#fba131;"> - <g:message code="selectPendingPollsLbl"/> - </option>
                <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedPollsLbl"/> - </option>
            </select>
        </div>
    </div>


    <div id="mainPageEventList" class="pageContentDiv row"><ul></ul></div>

</div>
<votingsystem-advanced-search-dialog id="advancedSearchDialog"></votingsystem-advanced-search-dialog>
</body>
</html>
<asset:script>
    var dynatable

    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
        $("#advancedSearchButton").css("visibility", "visible")
        $('#mainPageEventList').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            table: {
                bodyRowSelector: 'li'
            },
            dataset: {
                ajax: true,
                ajaxUrl: "${createLink(controller: 'eventVSElection', action: 'index')}",
                ajaxOnLoad: false,
                perPageDefault: 100,
                records: []
            },
            writers: {
                _rowWriter: eventVSWriter
            }
        });
        dynatable = $('#mainPageEventList').data('dynatable');
        dynatable.settings.params.records = 'eventsVSElection'
        dynatable.settings.params.queryRecordCount = 'numEventsVSElection'
        dynatable.settings.params.totalRecordCount = 'numEventsVSElectionInSystem'

        $('#eventsStateSelect').on('change', function (e) {
            var eventState = $(this).val()
            var optionSelected = $("option:selected", this);
            console.log(" - eventState: " + eventState)
            if(!isFirefox()) {
                if($('#eventsStateSelect')[0].selectedIndex == 0) {
                    $('#eventsStateSelect').css({'color': '#434343',
                                                 'border-color': '#cccccc'})
                } else {
                    $('#eventsStateSelect').css({'color': $( "#eventsStateSelect option:selected" ).css('color'),
                         'border-color': $( "#eventsStateSelect option:selected" ).css('color')})
                }
            }
            var targetURL = "${createLink( controller:'eventVSElection')}";
            if("" != eventState) {
                history.pushState(null, null, targetURL);
                targetURL = targetURL + "?eventVSState=" + $(this).val()
            }
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.paginationPage.set(1);
            dynatable.process();
        });


    });

    $('#mainPageEventList').bind('dynatable:afterUpdate',  function() {
        $('.eventDiv').click(function() {
            window.location.href = $(this).attr('data-href')
        }
    )})

    var eventTemplate = $('#eventTemplate').html()

    function eventVSWriter(rowIndex, jsonAjaxData, columns, cellWriter) {
        var eventVS = new EventVS(jsonAjaxData, eventTemplate, "VOTES")
        return eventVS.getElement()
    }

    function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        document.querySelector("#searchInfo").show(textToSearch, dateBeginFrom, dateBeginTo)
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'search', action: 'eventVSElection')}?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo
        dynatable.process();
    }

</asset:script>