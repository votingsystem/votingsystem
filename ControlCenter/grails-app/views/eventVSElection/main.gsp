<html>
<head>
    <meta name="layout" content="main" />
    <r:require module="dynatableModule"/>
</head>
<body>
<div class="mainPage">
    <div style="">
        <div style="position:relative; height: 30px;">
            <div style="position:absolute;width: 50%;  margin: auto; left: 0; right: 0;">
                <select id="eventsStateSelect" style="margin:0px 0px 0px 40px;color:black;">
                    <option value="" style="color:black;"> - <g:message code="selectPollsLbl"/> - </option>
                    <option value="ACTIVE" style="color:#6bad74;"> - <g:message code="selectOpenPollsLbl"/> - </option>
                    <option value="AWAITING" style="color:#fba131;"> - <g:message code="selectPendingPollsLbl"/> - </option>
                    <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedPollsLbl"/> - </option>
                </select>
            </div>
        </div>
        <g:render template="/template/eventsSearchInfo"/>
        <div class="">
            <ul id="mainPageEventList" style="display: block; width: 100%; position: relative;" class="row"></ul>
        </div>
    </div>

    <div id="eventTemplate" style="display:none;">
        <g:render template="/template/event" model="[isTemplate:'true']"/>
    </div>
</div>

</body>
</html>
<r:script>
    var dynatable

    $(function() {

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
        dynatable.settings.params.records = 'eventsVSElections'
        dynatable.settings.params.queryRecordCount = 'numEventsVSElection'
        dynatable.settings.params.totalRecordCount = 'numEventsVSElectionInSystem'

        $('#eventsStateSelect').on('change', function (e) {
            eventState = $(this).val()
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
            if("" != eventState) targetURL = targetURL + "?eventVSState=" + $(this).val()
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.process();
        });


    });

    $('#mainPageEventList').bind('dynatable:afterUpdate',  function() {
        $('.eventDiv').click(function() {
            window.location.href = $(this).attr('href')
        }
    )})

    var eventTemplate = $('#eventTemplate').html()

    function eventVSWriter(rowIndex, jsonAjaxData, columns, cellWriter) {
        var eventVS = new EventVS(jsonAjaxData, eventTemplate, "VOTES")
        return eventVS.getElement()
    }

    function getSearchResult(newSearchQuery) {
        newSearchQuery.eventState = eventState
        newSearchQuery.subsystem = "${selectedSubsystem}"
        searchQuery = newSearchQuery
        showEventsSearchInfoMsg(newSearchQuery)
        loadEvents("${createLink(controller:'search', action:'find')}?max=" +
                numMaxEventsForPage + "&offset=0", newSearchQuery)
    }
</r:script>