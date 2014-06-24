<style>
    .receptorsBox {
        margin: 10px auto 30px auto;
        padding: 5px 10px 5px 10px;;
        border: 1px solid #ccc;
    }

    .receptorsBox legend {
        font-size: 1.2em;
        font-weight: bold;
        color:#6c0404;
    }
</style>
<div id='depositDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="depositDialogCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                </h4>
            </div>
            <form id="depositDialogForm"  method="post"  class="">
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-8">
                            <div class="form-group" style="display: inline-block;">
                                <label style="display: inline;"><g:message code="amountLbl"/>: </label>
                                <input type="number" id="amount" min="1" value="0" class="form-control"
                                       style="width:120px;margin:0px 0px 0px 0px;display: inline;" name="amount">
                                <label style="display: inline;">EUR</label>
                            </div>
                        </div>
                    </div>
                    <div class="form-group" style="margin:15px 0px 15px 0px;">
                        <label><g:message code="subjectLbl"/></label>
                        <textarea id="depositDialogSubject" class="form-control" rows="2" required="" name="subject"></textarea>
                    </div>

                    <label id="selectReceptorlblDiv" style=""></label>
                    <div id="receptorPanelDiv">
                        <div id="fieldsDiv" class="receptorsBox" style="display:none;">
                            <fieldset id="fieldsBox">
                                <legend id="fieldsLegend" style="border: none;"><g:message code="receptorLbl"/></legend>
                                <div id="fields" style=""></div>
                            </fieldset>
                        </div>

                        <div id="searchPanel" class="form-group form-inline text-center"
                             style="margin:15px auto 0px auto;display: inline-block; width: 100%;">
                            <input id="userSearchInput" type="text" class="form-control" style="width:220px; display: inline;"
                                   placeholder="<g:message code="enterReceptorDataMsg"/>">
                            <button type="button" onclick="processUserSearch()" class="btn btn-danger" style="display: inline;">
                                <g:message code="userSearchLbl" /></button>
                        </div>

                        <div id="deposit_dialog_uservs_tableDiv" style="margin: 20px auto 0px auto; max-width: 800px; overflow:auto; visibility: hidden;">
                            <table class="table white_headers_table" id="deposit_dialog_uservs_table" style="">
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


                </div>
                <div class="modal-footer">
                    <button id="depositDialogSubmitButton" type="submit" class="btn btn-accept-vs">
                        <g:message code="acceptLbl"/></button>
                    <button id="advancedSearchCancelButton" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                        <g:message code="closeLbl"/>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
<div id="newReceptorTemplate" style="display:none;">
    <div class="form-group" style='margin: 0px 0px 5px 10px;display:inline-block; width:100%;'>
        <button data-userid="{0}" onclick="removeReceptor(this)" type="button" class="btn btn-default"
                style="display:inline;margin:0px 20px 0px 0px;">
            <g:message code="deleteLbl"/> <i class="fa fa-times"></i>
        </button>
        <div class='newReceptorNameDiv' style='font-size:1.2em;padding:0px 0 0 10px;vertical-align:middle;display:inline; width:100%;'>{1}</div>
    </div>
</div>
<asset:script>

    var operation
    var fromUserName
    var fromUserIBAN
    var dateValidTo
    var receptorMap = {}
    var userMap = {}
    var groupId
    var receptorTemplate = document.getElementById('newReceptorTemplate').innerHTML

    function showDepositDialog(depositType, fromUser, fromIBAN, validTo, targetGroupId) {
        operation = depositType
        fromUserName = fromUser
        fromUserIBAN = fromIBAN
        dateValidTo = validTo
        groupId = targetGroupId
        var caption
        var selectReceptorMsg
        switch(depositType) {
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                caption = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToMember'/></div>"
                selectReceptorMsg = '<g:message code="selectReceptorMsg"/>'
                document.getElementById('receptorPanelDiv').style.display = 'block'
                break;
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                caption = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToMemberGroup'/></div>"
                selectReceptorMsg = '<g:message code="selectReceptorsMsg"/>'
                document.getElementById('receptorPanelDiv').style.display = 'block'
                break;
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                caption = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToAllMembers'/></div>"
                selectReceptorMsg = '<g:message code="depositToAllGroupMembersMsg"/>'
                 document.getElementById('receptorPanelDiv').style.display = 'none'
                break;
        }
        document.getElementById('depositDialogCaption').innerHTML = caption
        document.getElementById('selectReceptorlblDiv').innerHTML = selectReceptorMsg
        $('#depositDialog').modal('show');
    }

    $('#depositDialog').on('hidden.bs.modal', function (e) {
        receptorMap = {}
        userMap = {}
        if(document.getElementById("fields") != null) document.getElementById("fields").innerHTML= ''

        var deposit_dialog_uservs_table = document.getElementById("deposit_dialog_uservs_table");
        var deposit_dialog_uservs_tableBody = deposit_dialog_uservs_table.getElementsByTagName('tbody');
        var numRows = deposit_dialog_uservs_tableBody.length;
        while(numRows) deposit_dialog_uservs_table.removeChild(deposit_dialog_uservs_tableBody[--numRows]);
        document.getElementById("deposit_dialog_uservs_table").style.visibility = 'hidden'

        $("#fieldsDiv").hide()
        $("#selectReceptorlblDiv").show()

        $("#depositDialogForm").data('bootstrapValidator').resetForm();
        document.getElementById("depositDialogForm").reset();
    })

    function processUserSearch() {
        userMap = {}
        var textToSearch = document.getElementById("userSearchInput").value
        if(textToSearch.trim() == "") return
        dynatable.settings.dataset.ajax = true
        var targetURL
        if(groupId != null) targetURL = "${createLink(controller: 'userVS', action: 'searchGroup')}?searchText=" + textToSearch + "&groupId=" + groupId
        else targetURL = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
        dynatable.settings.dataset.ajaxUrl = targetURL
        dynatable.paginationPage.set(1);
        dynatable.process();
    }

    $(function() {
        $("#userSearchInput").bind('keypress', function(e) {if (e.which == 13) {
            processUserSearch();
            e.preventDefault();
         } });


        $('#deposit_dialog_uservs_table').dynatable({
            features: {
                paginate: false,
                search: false,
                recordCount: false,
                perPageSelect: false
            },
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
        dynatable = $('#deposit_dialog_uservs_table').data('dynatable');
        dynatable.settings.params.records = 'userVSList'
        dynatable.settings.params.queryRecordCount = 'queryRecordCount'
        dynatable.settings.params.totalRecordCount = 'numTotalUsers'


        $('#deposit_dialog_uservs_table').bind('dynatable:afterUpdate',  function() {
            document.getElementById('deposit_dialog_uservs_table').style.visibility = 'visible'
        })

        $('#depositDialogForm').bootstrapValidator({
                excluded: [':disabled'],
                feedbackIcons: {
                    valid: 'glyphicon glyphicon-ok',
                    invalid: 'glyphicon glyphicon-remove',
                    validating: 'glyphicon glyphicon-refresh'
                },
                message: '<g:message code="fieldTypeErrorMsg"/>',
                submitHandler: function(validator, form, submitButton) {

                    document.getElementById("depositDialogSubmitButton").disabled = false;
                    switch(operation) {
                        case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                            if(Object.keys(receptorMap).length == 0){
                                showResultDialog('<g:message code="dataFormERRORLbl"/>',
                                    '<g:message code="receptorMissingErrorLbl"/>', function() {})
                                return false
                            }
                            break;
                        case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                            if(Object.keys(receptorMap).length == 0){
                                showResultDialog('<g:message code="dataFormERRORLbl"/>',
                                    '<g:message code="receptorsMissingErrorLbl"/>', function() {})
                                return false
                            }
                            break;
                        case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                            break;
                    }

                    var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, operation)
                    webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                    webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                    webAppMessage.serviceURL = "${createLink( controller:'transaction', action:"deposit", absolute:true)}"
                    webAppMessage.signedMessageSubject = "<g:message code='depositFromGroupMsgSubject'/>"
                    webAppMessage.signedContent = {operation:operation, subject:$("#depositDialogSubject").val(),
                        toUserIBAN:getToUserIBAN(), amount: $("#amount").val(), currency:"EUR", fromUser:fromUserName,
                        fromUserIBAN:fromUserIBAN, toUserIBAN:getToUserIBAN() , validTo:dateValidTo }

                    webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                    webAppMessage.callerCallback = 'depositDialogCallback'
                    console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
                    VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                },
                fields: {
                    amount: { validators: {notEmpty: { message: '<g:message code="emptyFieldErrorMsg"/>' }} },
                    subject: { validators: {notEmpty: { message: '<g:message code="emptyFieldErrorMsg"/>'} } }
                }
        });

    })

    function getToUserIBAN() {
        var result = []
        Object.keys(receptorMap).forEach(function(entry) {
            result.push(receptorMap[entry].IBAN);
        });
        return result
    }

    function depositDialogCallback(appMessage) {
        console.log("depositDialogCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="depositERRORLbl"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='depositOKLbl'/>"
                $('#depositDialog').modal('hide');
            }
            var msg = appMessageJSON.message
            showResultDialog(caption, msg)
        }
    }

    function depositDialogUserRowWriter(rowIndex, jsonUserData, columns, cellWriter) {
        var name = getUserVSName(jsonUserData)

        userMap[jsonUserData.id] = jsonUserData
        var tr = '<tr onclick="depositDialogAddReceptor(\'' + jsonUserData.id + '\')"><td title="" class="text-center">' +
            '<a href="#" onclick="">' + jsonUserData.nif + '</a></td><td class="text-center">' + name + '</td></tr>'
        return tr
    }

    function depositDialogAddReceptor (userId) {
        var receptorJSON = userMap[userId]
        switch(operation) {
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                document.getElementById("fields").innerHTML= ''
                receptorMap[receptorJSON.id] = receptorJSON
                var name = getUserVSName(receptorJSON)
                var newFieldHTML = receptorTemplate.format(userId, name);
                $("#fieldsBox #fields").append($(newFieldHTML))
                if(Object.keys(receptorMap).length == 1){
                    $("#selectReceptorlblDiv").hide()
                    $("#fieldsDiv").show()
                }
                break;
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                if(receptorMap[userId] == null) {
                    receptorMap[receptorJSON.id] = receptorJSON
                    var name = getUserVSName(receptorJSON)
                    var newFieldHTML = receptorTemplate.format(userId, name);
                    $("#fieldsBox #fields").append($(newFieldHTML))
                    if(Object.keys(receptorMap).length == 1){
                        $("#selectReceptorlblDiv").hide()
                        $("#fieldsDiv").show()
                    }
                }
                break;
            case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                break;
        }
    }

    function removeReceptor(removeButton) {
         var userId = $(removeButton).attr('data-userid')
         $(removeButton).parent().fadeOut(400, function() { $(removeButton).parent().remove(); });
         delete receptorMap[userId];
         if(Object.keys(receptorMap).length == 0){
            $("#fieldsDiv").hide()
            $("#selectReceptorlblDiv").show()
         }
    }

    function getUserVSName(jsonUserData) {
        var name = jsonUserData.name
        if(jsonUserData.firstName != null && "" != jsonUserData.firstName.trim()) name = jsonUserData.firstName + " " +
            jsonUserData.lastName
        return name
    }

</asset:script>