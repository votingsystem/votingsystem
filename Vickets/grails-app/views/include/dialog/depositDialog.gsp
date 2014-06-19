<div id='depositDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="depositDialogCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                </h4>
            </div>
            <div class="modal-body">
                <div class="row">
                    <div class="col-md-8">
                        <div class="form-inline" style="display: inline-block;">
                            <label style="display: inline;"><g:message code="amountLbl"/>: </label>
                            <input type="number" id="amount" min="1" value="0" class="form-control"
                                   style="width:120px;margin:0px 0px 0px 0px;display: inline;" required>
                            <label style="display: inline;">EUR</label>
                        </div>
                    </div>
                </div>
                <div class="form-group" style="margin:15px 0px 15px 0px;">
                    <label><g:message code="subjectLbl"/></label>
                    <textarea id="subject" class="form-control" rows="2" required=""></textarea>
                </div>

                <label id="selectReceptorlblDiv" style=""><g:message code="selectReceptorMsg"/></label>

                <div id="fieldsDiv" class="fieldsBox" style="display:none;">
                    <fieldset id="fieldsBox">
                        <legend id="fieldsLegend" style="border: none;"><g:message code="receptorLbl"/></legend>
                        <div id="fields" style=""></div>
                    </fieldset>
                </div>

                <div id="searchPanel" class="form-group form-inline text-center"
                     style="margin:15px auto 0px auto;display: inline-block; width: 100%;">
                    <input id="userSearchInput" type="text" class="form-control" style="width:220px; display: inline;">
                    <button type="button" onclick="processUserSearch()" class="btn btn-danger" style="display: inline;">
                        <g:message code="userSearchLbl" /></button>
                </div>

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
            <div class="modal-footer">
                <button type="button" class="btn btn-accept-vs" onclick="submitDeposit()">
                    <g:message code="acceptLbl"/></button>
                <button id="advancedSearchCancelButton" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                    <g:message code="closeLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<div id="newReceptorTemplate" style="display:none;">
    <div class="form-group form-inline text-center" style='margin: 0px 20px 5px 20px;display:inline-block;'>
        <button data-userid="{0}" onclick="removeReceptor(this)" type="button" class="btn btn-default"
                style="display:inline;margin:0px 20px 0px 0px;">
            <g:message code="deleteLbl"/> <i class="fa fa-times"></i>
        </button>
        <div class='newReceptorNameDiv' style='font-size:1.2em;padding:0px 0 0 10px;vertical-align:middle;display:inline;'>{1}</div>
    </div>
</div>
<asset:script>
    function showDepositDialog(depositType, groupName) {
        cleanCssError()
        var message = groupName
        var caption
        switch(depositType) {
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                caption = groupName + '<br/><div style="font-weight: normal;"><g:message code="vicketDepositFromGroupToMember"/></div>'
                break;
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                caption = groupName + '<br/><div style="font-weight: normal;"><g:message code="vicketDepositFromGroupToMemberGroup"/></div>'
                break;
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                caption = groupName + '<br/><div style="font-weight: normal;"><g:message code="vicketDepositFromGroupToAllMembers"/></div>'
                break;
            default:
                message = 'unknown'
                caption = 'unknown'
        }
        document.getElementById('depositDialogCaption').innerHTML = caption

        $('#depositDialog').modal('show');
    }

    $('#depositDialog').on('hidden.bs.modal', function (e) {
        document.getElementById('amount').value = 0
        document.getElementById('subject').value = ''

        var uservs_table = document.getElementById("uservs_table");
        var uservs_tableBody = uservs_table.getElementsByTagName('tbody');
        var numRows = uservs_tableBody.length;
        while(numRows) uservs_table.removeChild(uservs_tableBody[--numRows]);
        document.getElementById("uservs_tableDiv").style.visibility = 'hidden'

    })

    function submitDeposit() {
        cleanCssError()
    	if(!document.getElementById('amount').validity.valid) {
		    $("#amount").addClass("formFieldError");
		    showResultDialog('<g:message code="dataFormERRORLbl"/>', '<g:message code="numberFieldLbl"/>', function() {})
		    return false
        }
    }

    function cleanCssError() {
        $("#amount").removeClass("formFieldError");
        $("#subject").removeClass("formFieldError");
    }

    var userMap = {}

    function processUserSearch() {
        userMap = {}
        var textToSearch = document.getElementById("userSearchInput").value
        if(textToSearch.trim() == "") return
        dynatable.settings.dataset.ajax = true
        dynatable.settings.dataset.ajaxUrl= "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
        dynatable.paginationPage.set(1);
        dynatable.process();
    }

    $(function() {
        $("#userSearchInput").bind('keypress', function(e) {if (e.which == 13) { processUserSearch() } });

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
                _rowWriter: depositDialogUserRowWriter
            }
        });
        dynatable = $('#uservs_table').data('dynatable');
        dynatable.settings.params.records = 'userVSList'
        dynatable.settings.params.queryRecordCount = 'queryRecordCount'
        dynatable.settings.params.totalRecordCount = 'numTotalUsers'


        $('#uservs_table').bind('dynatable:afterUpdate',  function() {
            document.getElementById('uservs_table').style.visibility = 'visible'
        })

    })

    function depositDialogUserRowWriter(rowIndex, jsonUserData, columns, cellWriter) {
        var name = getUserVSName(jsonUserData)

        userMap[jsonUserData.id] = jsonUserData
        var tr = '<tr onclick="depositDialogAddReceptor(\'' + jsonUserData.id + '\')"><td title="" class="text-center">' +
            '<a href="#" onclick="">' + jsonUserData.nif + '</a></td><td class="text-center">' + name + '</td></tr>'
        return tr
    }

    var receptorMap = {}
    var receptorTemplate = document.getElementById('newReceptorTemplate').innerHTML

    function depositDialogAddReceptor (userId) {
        var receptorJSON = userMap[userId]
        if(receptorMap[userId] == null) {
            receptorMap[receptorJSON.id] = receptorJSON
            var name = getUserVSName(receptorJSON)
            var newFieldHTML = receptorTemplate.format(userId, name);
            $("#fieldsBox #fields").append($(newFieldHTML))
            if(Object.keys(receptorMap).length == 1){
                $("#selectReceptorlblDiv").fadeOut(400)
                $("#fieldsDiv").fadeIn(400)
            }
        }
    }

    function removeReceptor(removeButton) {
         var userId = $(removeButton).attr('data-userid')
         $(removeButton).parent().fadeOut(1000, function() { $(removeButton).parent().remove(); });
         delete receptorMap[userId];
         if(Object.keys(receptorMap).length == 0){
            $("#fieldsDiv").fadeOut(400)
            $("#selectReceptorlblDiv").fadeIn(400)
         }
    }

    function getUserVSName(jsonUserData) {
        var name = jsonUserData.name
        if(jsonUserData.firstName != null && "" != jsonUserData.firstName.trim()) name = jsonUserData.firstName + " " +
            jsonUserData.lastName
        return name
    }

</asset:script>