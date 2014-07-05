<!DOCTYPE html>
<html>
<head>
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <script type="text/javascript" src="${resource(dir: 'bower_components/dynatable', file: 'jquery.dynatable.js')}"></script>
    <asset:stylesheet src="jquery.dynatable.css"/>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="userSearchPageTitle"/></li>
        </ol>
    </div>

    <div id="searchPanel" class="" style="background:#ba0011; padding:10px 10px 10px 10px; width: 300px; margin:0px auto 0px auto;">
        <input id="userSearchInput" type="text" class="form-control" placeholder="<g:message code="userSearchLbl" />"
               style="width:220px; border-color: #f9f9f9;display:inline; vertical-align: middle;">
        <i id="searchPanelCloseIcon" onclick="processUserSearch()" class="fa fa-search text-right navBar-vicket-icon"
           style="margin:0px 0px 0px 15px; display:inline;vertical-align: middle;"></i>
    </div>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>


    <template is="auto-binding">
        <core-ajax id="ajax" auto url="{{url}}" response="{{userListJSON}}" handleAs="json" method="get"
                   contentType="json"></core-ajax>
        <div layout vertical center style="max-width: 800px; overflow:auto;">
            <table class="table white_headers_table" id="uservs_table" style="">
                <thead>
                <tr style="color: #ff0000;">
                    <th data-dynatable-column="uservsNIF" style="width: 60px;"><g:message code="nifLbl"/></th>
                    <th data-dynatable-column="uservsName" style=""><g:message code="nameLbl"/></th>
                </tr>
                </thead>
                <tbody>
                <template repeat="{{userJSON in userListJSON.userVSList}}">
                    <tr>
                        <td class="text-center">{{userJSON.nif}}</td>
                        <td class="text-center">{{userJSON.name}}</td>
                    </tr>
                </template>
                </tbody>
            </table>
        </div>


    </template>


    <div id="uservs_tableDiv" style="margin: 20px auto 0px auto; max-width: 800px; overflow:auto; visibility: hidden;">
        <table class="table white_headers_table" id="uservs_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th data-dynatable-column="uservsNIF" style="width: 60px;"><g:message code="nifLbl"/></th>
                <th data-dynatable-column="uservsName" style=""><g:message code="nameLbl"/></th>
                <!--<th data-dynatable-no-sort="true"><g:message code="voucherLbl"/></th>-->
            </tr>
            </thead>
        </table>
    </div>

</div>
</body>

</html>
<asset:script>
    $(function() {


        $("#userSearchInput").bind('keypress', function(e) {
            if (e.which == 13) {
                processUserSearch()
            }
        });

        $('#uservs_table').dynatable({
            features: dynatableFeatures,
            inputs: dynatableInputs,
            params: dynatableParams,
            dataset: {
                ajax: false,
                ajaxOnLoad: false,
                perPageDefault: 50,
                records: []
            },
            writers: {
                _rowWriter: rowWriter
            }
        });
        dynatable = $('#uservs_table').data('dynatable');
        dynatable.settings.params.records = 'userVSList'
        dynatable.settings.params.queryRecordCount = 'queryRecordCount'
        dynatable.settings.params.totalRecordCount = 'numTotalUsers'


        $('#uservs_table').bind('dynatable:afterUpdate',  function() {
            console.log("page loaded")
            document.getElementById('uservs_table').style.visibility = 'visible'
        })

        //$("#uservs_table").stickyTableHeaders({fixedOffset: $('.navbar')});
        $("#uservs_table").stickyTableHeaders();

    })

    function rowWriter(rowIndex, jsonUserData, columns, cellWriter) {
        var name = jsonUserData.name
        if(jsonUserData.firstName != null && "" != jsonUserData.firstName.trim()) name = jsonUserData.firstName + " " +
            jsonUserData.lastName
        var tr = '<tr onclick="showUserData(\'' + jsonUserData.id + '\')"><td title="" class="text-center">' +
            '<a href="#" onclick="">' + jsonUserData.nif + '</a></td><td class="text-center">' + name + '</td></tr>'
        return tr
    }

    function showUserData(userId) {
        window.location.href = updateMenuLink("${createLink(controller: 'userVS')}/" + userId.toString())
    }

//{ "userVSList": [ { "id": 4, "nif": "07553172H", "firstName": "Name7553172H", "lastName": "SurName7553172H", "name": "Name7553172H", "IBAN": "ES8978788989450000000004", "state": "ACTIVE", "type": "USER" } ], "queryRecordCount": 1, "numTotalTransactions": 1 }
    function processUserSearch() {
        var textToSearch = document.getElementById("userSearchInput").value
        if(textToSearch.trim() == "") return
        dynatable.settings.dataset.ajax = true
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
        dynatable.paginationPage.set(1);
        dynatable.process();
    }

</asset:script>