<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-user-box', file: 'votingsystem-user-box.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/tagvs-select-dialog']"/>">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/uservs-search.gsp']"/>">


<polymer-element name="vicket-deposit-form">
<template>
        <g:include view="/include/styles.gsp"/>
        <style no-shim>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        </style>
        <div id="container">

            <div layout horizontal center center-justified style="display:{{isDialog?'none':'block'}}">
                <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                    <votingsystem-html-echo html="{{caption}}"></votingsystem-html-echo>
                </div>
            </div>

            <div style="display:{{messageToUser?'block':'none'}};">
                <div style="color: {{status == 200?'#388746':'#ba0011'}};">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{messageToUser}}</div>
                            <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{status == 200?'#388746':'#ba0011'}};"></core-icon>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>
            </div>

            <div layout vertical id="formDataDiv" style="padding: 5px 20px 0px 20px; height: 100%;">
                <div layout horizontal center center-justified>
                    <div style="margin:0 15px 0 0;" >
                        <input type="text" id="amount" class="form-control" style="width:150px;" pattern="^[0-9]*$" required
                               title="<g:message code="amountLbl"/>"
                               placeholder="<g:message code="amountLbl"/> (EUR)"/>
                    </div>
                    <div>
                        <input type="text" id="depositSubject" class="form-control" style="width:350px;" required
                               title="<g:message code="subjectLbl"/>" placeholder="<g:message code="subjectLbl"/> (EUR)"/>
                    </div>
                </div>

                <div  layout horizontal id="tagDataDiv" style="width:100%;margin:15px 0px 15px 0px; border: 1px solid #ccc;
                        font-size: 1.1em; display: none; padding: 5px; display:{{isDepositToAll ? 'block':'none'}}">
                    <div style="margin:0px 10px 0px 0px; padding:5px;">
                        <div style="font-size: 0.9em;display: {{selectedTags.length == 0? 'block':'none'}};">
                            <g:message code="depositWithTagAdvertMsg"/>
                        </div>
                        <div layout horizontal center center-justified style="font-weight:bold;display: {{selectedTags.length == 0? 'none':'block'}};">
                            <g:message code="selectedTagsLbl"/>
                            <template repeat="{{tag in selectedTags}}">
                                <a class="btn btn-default" data-tagId='{{tag.id}}' on-click="{{removeTag}}"
                                   style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                    <i class="fa fa-minus"></i> {{tag.name}}</a>
                            </template>
                        </div>
                    </div>
                    <votingsystem-button on-click="{{showTagDialog}}" style="margin:10px 0px 0px 10px;display:{{(isPending || isCancelled ) ? 'none':'block'}} ">
                        <i class="fa fa-tag" style="margin:0 7px 0 3px;"></i> <g:message code="addTagLbl"/>
                    </votingsystem-button>
                </div>
                <div style="display:{{isDepositToAll ? 'none':'block'}}">
                    <div class="center" style="padding: 10px;">{{selectReceptorMsg}}</div>
                    <votingsystem-user-box flex id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></votingsystem-user-box>

                    <div id="receptorPanelDiv">
                        <div layout horizontal center center-justified id="searchPanel" style="margin:15px auto 0px auto;width: 100%;">
                            <input id="userSearchInput" type="text" style="width:200px;" class="form-control"
                                   placeholder="<g:message code="enterReceptorDataMsg"/>">
                            <votingsystem-button on-click="{{searchUser}}" style="margin: 0px 0px 0px 5px;">
                                <g:message code="userSearchLbl"/> <i class="fa fa-search"></i>
                            </votingsystem-button>
                        </div>
                        <uservs-search id="userSearchList"></uservs-search>
                    </div>
                </div>
                <div flex>
                </div>
                <div layout horizontal style="margin:10px 20px 0px 0px;">
                    <div flex></div>
                    <votingsystem-button on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                    </votingsystem-button>
                </div>
            </div>
        </div>

    <div>
        <div layout horizontal center center-justified style="padding:100px 0px 0px 0px;margin:0px auto 0px auto;">
            <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></tagvs-select-dialog>
        </div>
    </div>
</template>
<script>
    Polymer('vicket-deposit-form', {
        isDialog:false,
        operation:null,
        maxNumberTags:1,
        fromUserName:null,
        fromUserIBAN:null,
        dateValidTo:null,
        groupId:null,
        selectedTags: [],

        ready: function() {
            console.log(this.tagName + " - " + this.id)
            this.isDepositToAll = false
            var depositDialog = this
            this.$.userSearchList.addEventListener('user-clicked', function (e) {
                depositDialog.$.receptorBox.addUser(e.detail)
            })

            this.$.tagDialog.addEventListener('tag-selected', function (e) {
                console.log("tag-selected: " + JSON.stringify(e.detail))
                depositDialog.selectedTags = e.detail
            })

            this.$.userSearchInput.onkeypress = function(event){
                var chCode = ('charCode' in event) ? event.charCode : event.keyCode;
                if (chCode == 13) {
                    depositDialog.searchUser()
                }
            }
        },

        showTagDialog: function() {
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

        reset: function() {
            console.log(this.id + " - reset")
            this.removeErrorStyle(this.$.formDataDiv)
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
        },

        removeErrorStyle: function (element) {
            var formElements = element.children
            for(var i = 0; i < element.childNodes.length; i++) {
                var child = element.childNodes[i];
                this.removeErrorStyle(child);
                if(child != undefined) {
                    if(child.style != undefined) {
                        child.style.background = '#fff'
                        child.classList.remove("formFieldError");
                    }
                }
            }
        },

        submitForm: function () {
            this.removeErrorStyle(this.$.formDataDiv)
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
            if(!this.$.amount.validity.valid) {
                //this.$.amount.classList.add("formFieldError")
                this.$.amount.style.background = '#f6ccd0'
                this.setMessage(500, "<g:message code='emptyFieldMsg'/>")
                return
            }

            if(!this.$.depositSubject.validity.valid) {
                //this.$.depositSubject.classList.add("formFieldError")
                this.$.depositSubject.style.background = '#f6ccd0'
                this.setMessage(500, "<g:message code='emptyFieldMsg'/>")
                return
            }
            this.setMessage(200, null)
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, this.operation)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.serviceURL = "${createLink( controller:'transactionVS', action:"deposit", absolute:true)}"
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
            window[objectId] = {setClientToolMessage: function(appMessage) {
                var appMessageJSON = JSON.parse(appMessage)
                var caption
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "<g:message code='depositOKLbl'/>"
                    this.fire('operation-finished')
                } else caption = '<g:message code="depositERRORLbl"/>'
                showMessageVS(appMessageJSON.message, caption)
            }.bind(this)}
            console.log(this.tagName + " - window[objectId] - objectId: " + objectId)
            webAppMessage.callerCallback = objectId
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
        init:function(operation, fromUser, fromIBAN, validTo, targetGroupId, isDialog) {
            console.log(this.id + " - init - operation: " + operation + " - isDialog: " + isDialog)
            this.isDialog = isDialog? isDialog: false
            this.operation = operation
            this.fromUserName = fromUser
            this.fromUserIBAN = fromIBAN
            this.dateValidTo = validTo
            this.groupId = targetGroupId
            switch(operation) {
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                    this.caption = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToMember'/></div>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorMsg"/>'
                    this.$.receptorBox.multiSelection = false
                    break;
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                    this.caption = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToMemberGroup'/></div>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorsMsg"/>'
                    this.$.receptorBox.multiSelection = true
                    break;
                case Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                    this.isDepositToAll = true
                    this.caption = fromUser + "<br/><div style='font-weight: normal;'><g:message code='vicketDepositFromGroupToAllMembers'/></div>"
                    this.selectReceptorMsg = '<g:message code="depositToAllGroupMembersMsg"/>'
                    this.$.tagDataDiv.style.display = 'block'
                    break;
            }
            this.selectedTags = []
            return this.caption
        }
    });
</script>
</polymer-element>
