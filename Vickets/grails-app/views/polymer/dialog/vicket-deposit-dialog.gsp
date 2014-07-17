<link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-user-box', file: 'votingsystem-user-box.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-select-tag-dialog', file: 'votingsystem-select-tag-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/search-user.gsp']"/>">


<polymer-element name="vicket-deposit-dialog" attributes="caption opened serviceURL">
<template>
        <style no-shim>
        .view { :host {position: relative;} }
        .card {
            position: relative;
            display: inline-block;
            vertical-align: top;
            height: auto;
            background-color: #fefefe;
            box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
            border: 1px solid #ccc;
        }
        paper-button.button {
            background-color: #f9f9f9;
            color: #6c0404;
            border: 1px solid #ccc;
            margin:10px;
            vertical-align: middle;
            line-height: 24px;
            height: 35px;
        }
        paper-button:hover {
            background: #ba0011;
            color: #f9f9f9;
        }

        paper-button::shadow #ripple {
            color: white;
        }
        </style>

        <div id="container" class="card" style="width:600px; padding:0px 0px 15px 0px;">
            <div layout horizontal style="padding: 0px 10px 0px 20px;" >
                <h3 id="caption" flex style="color: #6c0404; font-weight: bold;"></h3>
                <core-icon-button icon="close" style="fill:#6c0404;" on-tap="{{close}}"></core-icon-button>
            </div>

            <div class="center card" style="font-weight: bold;color: {{status == 200?'#388746':'#ba0011'}};
            margin:10px auto 10px auto; border: 1px solid #ccc; background: #f9f9f9;padding:10px 20px 10px 20px; display:{{messageToUser == null?'none':'block'}};">
                <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                    <div id="messageToUser">{{messageToUser}}</div>
                    <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{status == 200?'#388746':'#ba0011'}};"></core-icon></div>
            </div>
            <div layout vertical style="padding: 5px 20px 0px 20px;">
                <paper-input id="amount" floatinglabel label="<g:message code="amountLbl"/> (EUR)"
                            validate="^[0-9]*$" error="<g:message code="onlyNumbersErrorLbl"/>" style="" required>
                </paper-input>
                <paper-input id="depositSubject" floatinglabel error="<g:message code="requiredLbl"/>"
                                    label="<g:message code="subjectLbl"/>" required></paper-input>


                <div  layout horizontal id="tagDataDiv" style="width:100%;margin:15px 0px 15px 0px; border: 1px solid #ccc;
                        font-size: 1.1em; display: none; padding: 5px; display:{{isDepositToAll ? 'block':'none'}}">
                    <div style="margin:0px 10px 0px 0px; padding:5px;">
                        <div id="tagDataDivMsg" style="font-size: 0.9em;display: {{selectedTags.length == 0? 'block':'none'}};">
                            <g:message code="depositWithTagAdvertMsg"/>
                        </div>
                        <div id="tagDataDivMsg" style="font-weight:bold;display: {{selectedTags.length == 0? 'none':'block'}};">
                            <g:message code="selectedTagsLbl"/>
                        </div>
                        <div id="selectedTagDivDepositDialog" style="display: {{selectedTags.length == 0? 'none':'block'}};">
                            <template repeat="{{tag in selectedTags}}">
                                <button data-tagId='{{tag.id}}' on-click="{{removeTag}}" type="button" class="btn btn-default"
                                        style="margin:7px 10px 0px 0px;">{{tag.name}}  <i class="fa fa-minus-circle"></i>
                                </button>
                            </template>
                        </div>
                    </div>
                    <votingsystem-button on-click="{{toggleTagDialog}}" style="margin:10px 0px 0px 10px;display:{{(isPending || isCancelled ) ? 'none':'block'}} ">
                        <g:message code="addTagLbl"/>
                    </votingsystem-button>
                </div>
                <div style="display:{{isDepositToAll ? 'none':'block'}}">
                    <div class="center" style="padding: 10px;">{{selectReceptorMsg}}</div>
                    <votingsystem-user-box flex id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></votingsystem-user-box>

                    <div id="receptorPanelDiv">
                        <div layout horizontal center center-justified id="searchPanel" style="margin:15px auto 0px auto;width: 100%;">
                            <input id="userSearchInput" type="text" class="form-control" style="width:200px;"
                                   placeholder="<g:message code="enterReceptorDataMsg"/>">
                            <paper-button raisedButton class="button" label="<g:message code="userSearchLbl"/>"
                                          on-click="{{searchUser}}" style="width:180px;"></paper-button>
                        </div>
                        <search-user id="userSearchList"></search-user>
                    </div>
                </div>
                <div layout horizontal style="margin:10px 20px 0px 0px;">
                    <div flex></div>
                    <paper-button raisedButton class="button" label="<g:message code="acceptLbl"/>"
                                  on-click="{{submitForm}}" style=""></paper-button>
                </div>
            </div>
        </div>

    <div style="position: absolute; width: 100%; top:0px;left:0px;">
        <div layout horizontal center center-justified style="padding:100px 0px 0px 0px;margin:0px auto 0px auto;">
            <votingsystem-select-tag-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></votingsystem-select-tag-dialog>
        </div>
    </div>

</template>
<script>
    Polymer('vicket-deposit-dialog', {
        operation:null,
        maxNumberTags:3,
        fromUserName:null,
        fromUserIBAN:null,
        dateValidTo:null,
        groupId:null,
        selectedTags: [],
        opened:false,

        ready: function() {
            console.log(this.tagName + " - " + this.id)
            this.style.display = 'none'
            this.isDepositToAll = false
            var depositDialog = this
            this.$.userSearchList.addEventListener('user-clicked', function (e) {
                depositDialog.$.receptorBox.addUser(e.detail)
            })

            this.$.tagDialog.addEventListener('tag-selected', function (e) {
                console.log("==== tag-selected: " + JSON.stringify(e.detail))
                depositDialog.selectedTags = e.detail
            })

            this.$.userSearchInput.onkeypress = function(event){
                var chCode = ('charCode' in event) ? event.charCode : event.keyCode;
                if (chCode == 13) {
                    depositDialog.searchUser()
                }
            }
            if(this.opened) show()
        },

        toggleTagDialog: function() {
            this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
        },

        removeTag: function(e) {
            var tagToDelete = e.target.templateInstance.model.tag
            for(tagIdx in this.selectedTags) {
                if(tagToDelete.id == this.selectedTags[tagIdx].id) {
                    this.selectedTags.splice(tagIdx, 1)
                }
            }
        },

        close: function() {
            console.log(this.id + " - close")
            this.isDepositToAll = false
            this.$.userSearchInput.value = ""
            this.$.amount.value = ""
            this.$.depositSubject.value = ""
            this.$.userSearchList.url = ""
            this.setMessage(200, null)
            this.$.receptorBox.removeUsers()
            this.$.userSearchList.reset()
            this.$.tagDialog.reset()
            this.$.tagDataDiv.style.display = 'none'
            this.style.display = 'none'
        },

        submitForm: function () {
            switch(this.operation) {
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                    if(this.$.receptorBox.getUserList().length == 0){
                        this.setMessage(500, "<g:message code='receptorMissingErrorLbl'/>")
                        return false
                    }
                    break;
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                    if(this.$.receptorBox.getUserList().length == 0){
                        this.setMessage(500, "<g:message code='receptorMissingErrorLbl'/>")
                        return false
                    }
                    break;
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                    break;
            }
            if(this.$.amount.invalid) {
                this.setMessage(500, "<g:message code='emptyFieldMsg'/>")
                return
            }
            if(this.$.depositSubject.invalid) {
                this.setMessage(500, "<g:message code='emptyFieldMsg'/>")
                return
            }
            this.setMessage(200, null)
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, this.operation)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.serviceURL = "${createLink( controller:'transaction', action:"deposit", absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code='depositFromGroupMsgSubject'/>"
            webAppMessage.signedContent = {operation:this.operation, subject:this.$.depositSubject.value,
                toUserIBAN:this.toUserIBAN(), amount: this.$.amount.value, currency:"EUR", fromUser:this.fromUserName,
                fromUserIBAN:this.fromUserIBAN, validTo:this.dateValidTo }

            if(this.selectedTags.length > 0) {
                var tagList = []
                for(tagIdx in this.selectedTags) {
                    tagList.push({id:this.selectedTags[tagIdx].id, name:this.selectedTags[tagIdx].name});
                }
                webAppMessage.signedContent.tags = tagList
            }
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            var objectId = Math.random().toString(36).substring(7)
            var vicketDepositDialog = this
            window[objectId] = {setClientToolMessage: function(appMessage) {
                var appMessageJSON = JSON.parse(appMessage)
                if(appMessageJSON != null) {
                    var caption
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='depositOKLbl'/>"
                        vicketDepositDialog.close()
                    } else caption = '<g:message code="depositERRORLbl"/>'
                    showMessageVS(appMessageJSON.message, caption)
                }
            }}
            webAppMessage.callerCallback = objectId
            console.log(this.tagName + " - objectId: " + objectId)
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        },

        toUserIBAN: function () {
            var receptorList = this.$.receptorBox.getUserList()
            var result = []
            for(userIdx in receptorList) {
                result.push(receptorList[userIdx].IBAN);
            }
            return result
        },
        searchUser: function() {
            var textToSearch = this.$.userSearchInput.value
            if(textToSearch.trim() == "") return
            var targetURL
            if(this.groupId != null) targetURL = "${createLink(controller: 'userVS', action: 'searchGroup')}?searchText=" + textToSearch + "&groupId=" + this.groupId
            else targetURL = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
            this.$.userSearchList.url = targetURL
        },

        setMessage:function(status, message) {
            console.log(this.tagName + " - setMessage - status: " + status, " - message: " + message)
            this.status = status
            this.messageToUser = message
        },
        show:function(operation, fromUser, fromIBAN, validTo, targetGroupId) {
            console.log(this.id + " - show - operation: " + operation)
            this.operation = operation
            this.fromUserName = fromUser
            this.fromUserIBAN = fromIBAN
            this.dateValidTo = validTo
            this.groupId = targetGroupId
            switch(operation) {
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                    this.$.caption.innerHTML = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToMember'/></div>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorMsg"/>'
                    this.$.receptorBox.multiSelection = false
                    break;
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                    this.$.caption.innerHTML = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToMemberGroup'/></div>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorsMsg"/>'
                    this.$.receptorBox.multiSelection = true
                    break;
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                    this.isDepositToAll = true
                    this.$.caption.innerHTML = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToAllMembers'/></div>"
                    this.selectReceptorMsg = '<g:message code="depositToAllGroupMembersMsg"/>'
                    this.$.tagDataDiv.style.display = 'block'
                    break;
            }
            this.selectedTags = []
            this.style.display = 'block'
        }
    });
</script>
</polymer-element>
