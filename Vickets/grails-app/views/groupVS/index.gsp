<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <asset:javascript src="jquery.grid-a-licious.min.js"/>
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <script type="text/javascript" src="${resource(dir: 'bower_components/dynatable', file: 'jquery.dynatable.js')}"></script>
    <asset:stylesheet src="jquery.dynatable.css"/>
    <asset:stylesheet src="vicket_groupvs.css"/>
</head>
<body>
<div class="pageContenDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="groupvsLbl"/></li>
        </ol>
    </div>

    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="groupvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;" class="form-control">
                <option value="ACTIVE"  style="color:#59b;"> - <g:message code="selectActiveGroupvsLbl"/> - </option>
                <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingGroupvsLbl"/> - </option>
                <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="selectClosedGroupvsLbl"/> - </option>
            </select>
        </div>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <div id="groupvsList" class="" style=""></div>
</div>

<div id="groupvsTemplate" style="display:none;">
    <g:render template="/template/groupvs" model="[isTemplate:'true']"/>
</div>
</body>
</html>
<asset:script>
    var dynatable
    var groupvsArray;

    $(function() {
        initGridalicious()

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

        dynatable.settings.params.records = 'groupvsRecords'
        dynatable.settings.params.queryRecordCount = 'numTotalGroups'

        $('#groupvsTypeSelect').on('change', function (e) {
            var groupvsType = $(this).val()
            var optionSelected = $("option:selected", this);
            console.log("groupvsTypeSelect - selected: " + groupvsType)

            var targetURL = "${createLink(controller: 'groupVS')}?menu=" + menuType;
            if("" != groupvsType) {
                history.pushState(null, null, targetURL);
                targetURL = targetURL + "&state=" + groupvsType
            }
            dynatable.settings.dataset.ajaxUrl= targetURL
            dynatable.paginationPage.set(1);
            dynatable.process();
        });

        $('#groupvsList').bind('dynatable:afterUpdate',  function() {
            $("#groupvsList").gridalicious('append', groupvsArray);
            $('p').each(function(index, item) {
                if($.trim($(item).text()) === "") {
                    $(item).slideUp(); // $(item).remove();
                }
            });
            updateMenuLinks()
            $('.groupvsDiv').click(function() {
                window.location.href = $(this).attr('data-href')
            })
        })

        $('#groupvsList').bind('dynatable:afterProcess',  function() { })

        $('#groupvsList').bind('dynatable:beforeProcess',  function() {
            initGridalicious();
        })

    })

    function initGridalicious() {
        groupvsArray = new Array;
        document.getElementById("groupvsList").innerHTML = ''
        $("#groupvsList").gridalicious({width: 300, gutter: 20, selector: '.item', animate: true,
            animationOptions: {speed: 200, duration: 300, complete:onCompleted() }});
    }

    function onCompleted() {
        updateMenuLinks()
        $('.groupvsDiv').click(function() {
            window.location.href = $(this).attr('data-href')
        })
    }

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

    function UserVS() {}

    UserVS.State = {
        ACTIVE:"ACTIVE",
        PENDING:"PENDING",
        CANCELLED:"CANCELLED"
    }

    GroupVS.prototype.getElement = function() {
        var $newGroup = $(this.groupHTML)
        var $groupDiv = $newGroup.find(".groupvsDiv");
        if(UserVS.State.ACTIVE == this.state) $groupDiv.addClass("groupvsActive");
        if(UserVS.State.PENDING == this.state) $groupDiv.addClass("groupvsPending");
        if(UserVS.State.CANCELLED == this.state) {
            $groupDiv.addClass("groupvsFinished");
            $groupDiv.find(".groupvsMessageCancelled").text("<g:message code="groupvsCancelledLbl"/>")
        }
        $groupDiv.attr('id', this.id)
        $groupDiv.attr('data-href', this.url)
        return $newGroup.html();
    }

    var groupvsTemplate = $('#groupvsTemplate').html()

    function groupVSWriter(rowIndex, jsonAjaxData, columns, cellWriter) {
        var groupVS = new GroupVS(jsonAjaxData, groupvsTemplate)
        groupvsArray.push(groupVS.getElement());
        return ''
    }

    function processUserSearch(textToSearch) {
        $("#pageInfoPanel").text("<g:message code="searchResultLbl"/> '" + textToSearch + "'")
        $('#pageInfoPanel').css("display", "block")
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'search', action: 'groupVS')}?searchText=" + textToSearch
        dynatable.paginationPage.set(1);
        dynatable.process();
    }

</asset:script>
