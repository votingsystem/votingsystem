<html>
<head>
    <meta name="layout" content="main" />
    <r:require module="dynatableModule"/>
</head>
<body>
<div class="">
    <div style="display: table;width:100%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="eventsStateSelect" style="margin:0px auto 0px auto;color:black; width: 300px;" class="form-control">
                <option value="" style="color:black;"> - <g:message code="selectClaimsLbl"/> - </option>
                <option value="ACTIVE" style="color:#388746;"> - <g:message code="selectOpenClaimsLbl"/> - </option>
                <option value="AWAITING" style="color:#fba131;"> - <g:message code="selectPendingClaimsLbl"/> - </option>
                <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedClaimsLbl"/> - </option>
            </select>
        </div>
    </div>

    <g:render template="/template/eventsSearchInfo"/>

    <div id="mainPageEventList" class="pageContentDiv"><ul></ul></div>

    <div id="eventTemplate" style="display:none;">
        <g:render template="/template/event" model="[isTemplate:'true']"/>
    </div>
    <g:include view="/include/dialog/advancedSearchDialog.gsp"/>
</div>
</body>
</html>
<r:script>
    var dynatable

    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
        $('#mainPageEventList').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            table: {
                bodyRowSelector: 'li'
            },
            dataset: {
                ajax: true,
                ajaxUrl: "${createLink(controller: 'eventVSClaim', action: 'index')}",
                ajaxOnLoad: false,
                perPageDefault: 100,
                records: []
            },
            writers: {
                _rowWriter: eventVSWriter
            }
        });

        dynatable = $('#mainPageEventList').data('dynatable');
        dynatable.settings.params.records = 'eventVS'
        dynatable.settings.params.queryRecordCount = 'totalEventVS'

        $('#eventsStateSelect').on('change', function (e) {
            var eventState = $(this).val()
            var optionSelected = $("option:selected", this);
            if(!isFirefox()) {
                if($('#eventsStateSelect')[0].selectedIndex == 0) {
                    $('#eventsStateSelect').css({'color': '#434343',
                                                 'border-color': '#cccccc'})
                } else {
                    $('#eventsStateSelect').css({'color': $( "#eventsStateSelect option:selected" ).css('color'),
                         'border-color': $( "#eventsStateSelect option:selected" ).css('color')})
                }
            }
            var targetURL = "${createLink( controller:'eventVSClaim')}";
            if("" != eventState) targetURL = targetURL + "?eventVSState=" + eventState
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.paginationPage.set(1);
            dynatable.process();
        });
     });

    var eventTemplate = $('#eventTemplate').html()

    function eventVSWriter(rowIndex, jsonAjaxData, columns, cellWriter) {
        var eventVS = new EventVS(jsonAjaxData, eventTemplate, "CLAIMS")
        return eventVS.getElement()
    }

    $('#mainPageEventList').bind('dynatable:afterUpdate',  function() {
        updateMenuLinks()
        $('.eventDiv').click(function() {
            window.location.href = $(this).attr('data-href')
        }
    )})

    function processUserSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        showEventsSearchInfoMsg(textToSearch, dateBeginFrom, dateBeginTo)
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=CLAIM"
        dynatable.process();
    }

    function processUserSearchJSON(jsonData) {
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}"
        dynatable.settings.dataset.ajaxData = jsonData
        dynatable.paginationPage.set(1);
        dynatable.process();
    }
</r:script>