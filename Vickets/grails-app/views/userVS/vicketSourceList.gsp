<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <script type="text/javascript" src="${resource(dir: 'bower_components/dynatable', file: 'jquery.dynatable.js')}"></script>
    <asset:stylesheet src="jquery.dynatable.css"/>
</head>
<body>
<div class="pageContenDiv" style="max-width: 1300px; margin: 0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <div class="row">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'userVS', action:'search')}"><g:message code="usersvsLbl"/></a></li>
                <li class="active"><g:message code="vicketSourceListPageLbl"/></li>
            </ol>
        </div>

        <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
            <div style="display:table-cell;margin: auto; vertical-align: top;">
                <select id="vicketSourceStateSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control">
                    <option value="ACTIVE"  style="color:#388746;"> - <g:message code="selectActiveVicketSourceLbl"/> - </option>
                </select>
            </div>
        </div>

        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
            background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

        <div id="vicketSourceList" class="row container"><ul></ul></div>
    </div>
</div>

<div id="vicketSourceTemplate" style="display:none;">
    <div>
        <li class='vicketSourceDiv linkvs'>
            <div class='vicketSourceSubjectDiv'><h2>{0}</h2></div>
            <div class='vicketSourceDescriptionDiv'><p>{1}</p></div>
            <div class='vicketSourceReasonDiv'>
                <p> {2}</p>
            </div>
        </li>
    </div>
</div>
</body>
</html>
<asset:script>
    var dynatable

    $(function() {
        $('#vicketSourceList').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            table: {
                bodyRowSelector: 'li'
            },
            dataset: {
                ajax: true,
                ajaxUrl: "<g:createLink controller="userVS" action="vicketSourceList"/>?menu=" + menuType,
                ajaxOnLoad: false,
                perPageDefault: 50,
                records: []
            },
            writers: {
                _rowWriter: vicketSourceWriter
            }
        });

        dynatable = $('#vicketSourceList').data('dynatable');

        dynatable.settings.params.records = 'vicketSourceList'
        dynatable.settings.params.queryRecordCount = 'numTotalVicketSources'


        $('#vicketSourceStateSelect').on('change', function (e) {
            var vicketSourceState = $(this).val()
            var optionSelected = $("option:selected", this);
            console.log("vicketSourceStateSelect - selected: " + vicketSourceState)
            var targetURL = "${createLink(controller: 'userVS', action:'vicketSourceList')}?menu=" + menuType;
            if("" != vicketSourceState) {
                history.pushState(null, null, targetURL);
                targetURL = targetURL + "&state=" + vicketSourceState
            }
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.paginationPage.set(1);
            dynatable.process();
        });

        $('#vicketSourceList').bind('dynatable:afterUpdate',  function() {
            updateMenuLinks()
            $('.vicketSourceDiv').click(function() {
                window.location.href = $(this).attr('data-href')
            }
        )})

    })


    function VicketSource(vicketSourceJSON, htmlTemplate) {
        if(vicketSourceJSON != null) {
            this.id = vicketSourceJSON.id
            this.dateCreated = vicketSourceJSON.dateCreated
            this.description = vicketSourceJSON.description
            this.state = vicketSourceJSON.state
            this.name = vicketSourceJSON.name
            this.reason = vicketSourceJSON.reason
            this.url = "${createLink( controller:'userVS')}/" + this.id
        }

        if(htmlTemplate != null) {
            this.groupHTML = htmlTemplate.format(this.name, this.description, this.reason);
        }
    }

    function UserVS() {}

    UserVS.State = {
        ACTIVE:"ACTIVE",
        PENDING:"PENDING",
        CLOSED:"CLOSED"
    }

    VicketSource.prototype.getElement = function() {
        var $newGroup = $(this.groupHTML)
        var $li = $newGroup.find("li");

        if(UserVS.State.ACTIVE == this.state) $li.addClass("vicketSourceActive");
        if(UserVS.State.PENDING == this.state) $li.addClass("vicketSourcePending");
        if(UserVS.State.CLOSED == this.state) {
            $li.addClass("vicketSourceFinished");
            $li.find(".cancelMessage").fadeIn(100)
        }

        $li.attr('id', this.id)
        $li.attr('data-href', this.url)
        return $newGroup.html();
    }

    var vicketSourceTemplate = $('#vicketSourceTemplate').html()

    function vicketSourceWriter(rowIndex, jsonAjaxData, columns, cellWriter) {
        var vicketSource = new VicketSource(jsonAjaxData, vicketSourceTemplate)
        return vicketSource.getElement()
    }

</asset:script>
