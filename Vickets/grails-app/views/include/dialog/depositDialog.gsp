<link rel="import" href="${resource(dir: '/bower_components/votingsystem-input', file: 'votingsystem-input.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-user-box', file: 'votingsystem-user-box.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-select-tag-dialog', file: 'votingsystem-select-tag-dialog.html')}">


<div id='depositDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="depositDialogCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                </h4>
            </div>

            <form id="depositDialogForm"  method="post"  class="">
                <div layout vertical style="padding: 5px 20px 0px 20px;">
                    <votingsystem-input id="amount" floatinglabel label="<g:message code="amountLbl"/> (EUR)"
                                        validate="^[0-9]*$" error="<g:message code="onlyNumbersErrorLbl"/>" style="display: inline;" required>
                    </votingsystem-input>
                    <div class="form-group" style="margin:15px 0px 15px 0px;">
                        <label><g:message code="subjectLbl"/></label>
                        <textarea id="depositDialogSubject" class="form-control" rows="2" required="" name="subject"></textarea>
                    </div>
                    <div id="tagDataDiv" style="margin:15px 0px 15px 0px; border: 1px solid #ccc; font-size: 1.1em; display: none; padding: 5px;">
                        <div style="display:table-cell;font-weight: bold; margin:0px 10px 0px 0px; padding:5px;">
                            <div id="tagDataDivMsg"><g:message code="depositWithTagAdvertMsg"/></div>
                            <div id="selectedTagDivDepositDialog" style="display: none;"></div>
                        </div>
                        <div style="display: table-cell;vertical-align: middle;">
                            <button id="selectTagButton" type="button" onclick="document.querySelector('#tagDialog').toggle()" class="btn btn-danger">
                                <g:message code="addTagLbl" /></button>
                        </div>
                    </div>

                    <votingsystem-user-box id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></votingsystem-user-box>

                    <div id="receptorPanelDiv">
                        <div id="searchPanel" class="form-group form-inline text-center"
                             style="margin:15px auto 0px auto;display: inline-block; width: 100%;">
                            <input id="userSearchInput" type="text" class="form-control" style="width:220px; display: inline;"
                                   placeholder="<g:message code="enterReceptorDataMsg"/>">
                            <button type="button" onclick="processUserSearch()" class="btn btn-danger" style="display: inline;">
                                <g:message code="userSearchLbl" /></button>
                        </div>

                        <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
                        <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

                        <g:include view="/include/search-user.gsp"/>
                        <search-user id="dialogUserList"></search-user>
                    </div>
                </div>
            </form>
                <div class="modal-footer">
                    <button id="depositDialogSubmitButton" onclick="submitForm()" class="btn btn-accept-vs">
                        <g:message code="acceptLbl"/></button>
                    <button id="advancedSearchCancelButton" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                        <g:message code="closeLbl"/>
                    </button>
                </div>

        </div>
    </div>
</div>
<votingsystem-select-tag-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></votingsystem-select-tag-dialog>

<asset:script>
    var operation
    var fromUserName
    var fromUserIBAN
    var dateValidTo
    var groupId

    var dialogUserList = document.querySelector("#dialogUserList")
    var receptorBox = document.querySelector("#receptorBox")
    var tagDialog = document.querySelector("#tagDialog")
    var selectedTags = {}

    dialogUserList.addEventListener('user-clicked', function (e) {
        receptorBox.addUser(e.detail)
    })

    tagDialog.addEventListener('tag-selected', function (e) {
        refreshTags(e.detail)
    })

    function refreshTags(selectedTagsMap) {
        selectedTags = selectedTagsMap
        document.getElementById("selectedTagDivDepositDialog").innerHTML = ''

        Object.keys(selectedTags).forEach(function(entry) {
            var tagDiv = document.createElement("div");
            tagDiv.classList.add('form-group');
            tagDiv.setAttribute("style","margin: 10px 0px 5px 10px;display:inline;");
            tagDiv.innerHTML = tagTemplate.format(entry, tagMap[entry]);
            document.querySelector("#selectedTagDivDepositDialog").appendChild(tagDiv);
        });

        if(Object.keys(selectedTags).length == 0){
            document.getElementById("tagDataDivMsg").innerHTML = '<g:message code='depositWithTagAdvertMsg'/>'
            document.getElementById("selectTagButton").innerHTML = '<g:message code='addTagLbl'/>'

            document.getElementById("selectedTagDivDepositDialog").style.display= 'none'
        } else {
            document.getElementById("tagDataDivMsg").innerHTML = '<g:message code='depositWithTagSelectedAdvertMsg'/>'
            document.getElementById("selectTagButton").innerHTML = '<g:message code='changeTagLbl'/>'
            document.getElementById("selectedTagDivDepositDialog").style.display= 'block'
        }
    }

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
                document.getElementById('tagDataDiv').style.display = 'table'
                break;
        }
        document.getElementById('depositDialogCaption').innerHTML = caption
        refreshTags({})
        $('#depositDialog').modal('show');
    }

    $('#depositDialog').on('hidden.bs.modal', function (e) {
        receptorBox.removeUsers()
        if(document.getElementById("fields") != null) document.getElementById("fields").innerHTML= ''
        dialogUserList.emptyTable()
        tagDialog.reset()
        document.querySelector("#fieldsDiv").style.display = 'none'
        document.getElementById("depositDialogForm").reset();
    })

    function processUserSearch() {
        console.log(" ======= processUserSearch")
        var textToSearch = document.getElementById("userSearchInput").value
        if(textToSearch.trim() == "") return
        var targetURL
        if(groupId != null) targetURL = "${createLink(controller: 'userVS', action: 'searchGroup')}?searchText=" + textToSearch + "&groupId=" + groupId
        else targetURL = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
        document.querySelector('#dialogUserList').url = targetURL
    }

    document.querySelector("#userSearchInput").onkeypress = function(event){
        var chCode = ('charCode' in event) ? event.charCode : event.keyCode;
        if (chCode == 13) {
            processUserSearch()
        }
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



    function submitForm() {
        console.log(" ======= submitForm: " + document.querySelector("#amount").invalid)
        var receptorList = receptorBox.getUserList()
        console.log(" ======= receptorList.length: " + receptorList.length)
        if(document.querySelector("#amount").invalid) { return false;}


            document.querySelector("#depositDialogSubmitButton").disabled = false;
            switch(operation) {
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                    if(receptorList.length == 0){
                        showResultDialog('<g:message code="dataFormERRORLbl"/>',
                            '<g:message code="receptorMissingErrorLbl"/>', function() {})
                        return false
                    }
                    break;
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                    if(receptorList.length == 0){
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

            if(Object.keys(selectedTags).length > 0) {
                var tagList = []
                Object.keys(selectedTags).forEach(function(entry) {
                    tagList.push({id:entry, name:selectedTags[entry]})
                })
                webAppMessage.signedContent.tags = tagList
            }
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            webAppMessage.callerCallback = 'depositDialogCallback'
            console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

</asset:script>