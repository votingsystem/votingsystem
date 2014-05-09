<!DOCTYPE html>
<html>
<head>
    <link href="${resource(dir: 'css', file: 'vicket_groupvs.css')}" type="text/css" rel="stylesheet"/>
    <meta name="layout" content="main" />
    <r:require module="dynatableModule"/>
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="groupvsLbl"/></li>
        </ol>
    </div>
    <button id="loadHistoryButton" type="button" class="btn btn-primary"
            style="margin: 10px 0px 10px 0px;display: none;">
        <g:message code="loadHistoryLbl"/>
        <i id="loadHistoryButtonIcon" class="fa fa-refresh fa-spin" style="display: none;"></i>
    </button>

    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="groupvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control">
                <option value="ACTIVE"  style="color:#388746;"> - <g:message code="selectActiveGroupvsLbl"/> - </option>
                <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingGroupvsLbl"/> - </option>
                <option value="CLOSED" style="color:#cc1606;"> - <g:message code="selectClosedGroupvsLbl"/> - </option>
            </select>
        </div>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
    background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <div id="groupvsList" class="row container"><ul></ul></div>

</div>

<div id="groupvsTemplate" style="display:none;">
    <g:render template="/template/groupvs" model="[isTemplate:'true']"/>
</div>
</body>
</html>
<r:script>
    var dynatable

    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
        $('#groupvsList').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            table: {
                bodyRowSelector: 'li'
            },
            dataset: {
                ajax: true,
                ajaxUrl: "<g:createLink controller="groupVS"/>?menu=" + menuType,
                ajaxOnLoad: false,
                perPageDefault: 50,
                records: []
            },
            writers: {
                _rowWriter: groupVSWriter
            }
        });

        dynatable = $('#groupvsList').data('dynatable');
        dynatable.settings.params.records = '<g:message code="groupvsRecordsLbl"/>'
        dynatable.settings.params.queryRecordCount = 'numTotalGroups'

        $('#groupvsTypeSelect').on('change', function (e) {
            var groupvsType = $(this).val()
            var optionSelected = $("option:selected", this);
            console.log("groupvsTypeSelect - selected: " + groupvsType)
            var targetURL = "${createLink(controller: 'groupVS')}?menu=" + menuType;
            if("" != groupvsType) targetURL = targetURL + "&groupvsType=" + groupvsType
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.paginationPage.set(1);
            dynatable.process();
        });

        $('#groupvsList').bind('dynatable:afterUpdate',  function() {
            $('.groupvsDiv').click(function() {
                window.location.href = $(this).attr('data-href')
            }
        )})

    })

    function GroupVS(groupJSON, htmlTemplate) {
        if(groupJSON != null) {
            this.id = groupJSON.id
            this.dateCreated = groupJSON.dateCreated
            this.description = groupJSON.description
            this.representative = groupJSON.representative
            this.numActiveUsers = groupJSON.numActiveUsers
            this.numPendingUsers = groupJSON.numPendingUsers
            this.state = groupJSON.state
            this.name = groupJSON.name
            this.url = "${createLink( controller:'groupVS')}/" + this.id
        }

        if(htmlTemplate != null) {
            this.groupHTML = htmlTemplate.format(this.name, this.description, this.numActiveUsers,
                this.representative.firstName + " " + this.representative.lastName);
        }
    }

    GroupVS.State = {
        ACTIVE:"ACTIVE",
        PENDING:"PENDING",
        CLOSED:"CLOSED"
    }

    GroupVS.prototype.getElement = function() {
        var $newGroup = $(this.groupHTML)
        var $li = $newGroup.find("li");

        if(GroupVS.State.ACTIVE == this.state) $li.addClass("groupvsActive");
        if(GroupVS.State.PENDING == this.state) $li.addClass("groupvsPending");
        if(GroupVS.State.CLOSED == this.state) {
            $li.addClass("groupvsFinished");
            $li.find(".cancelMessage").fadeIn(100)
        }

        $li.attr('id', this.id)
        $li.attr('data-href', this.url)
        return $newGroup.html();
    }

    var groupvsTemplate = $('#groupvsTemplate').html()

    function groupVSWriter(rowIndex, jsonAjaxData, columns, cellWriter) {
        var groupVS = new GroupVS(jsonAjaxData, groupvsTemplate)
        return groupVS.getElement()
    }

    function processUserSearch(textToSearch) {
        $("#pageInfoPanel").text("<g:message code="searchResultLbl"/> '" + textToSearch + "'")
        $('#pageInfoPanel').css("display", "block")
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'search', action: 'groupVS')}?searchText=" + textToSearch
        dynatable.paginationPage.set(1);
        dynatable.process();
    }

</r:script>
