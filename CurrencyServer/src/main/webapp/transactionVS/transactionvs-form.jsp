<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-user-box/vs-user-box.html" rel="import"/>
<link href="../resources/bower_components/vs-currency-selector/vs-currency-selector.html" rel="import"/>
<link href="../tagVS/tagvs-select-dialog.vsp" rel="import"/>
<link href="../userVS/uservs-selector-dialog.vsp" rel="import"/>

<dom-module name="transactionvs-form">
<template>
    <style></style>
    <div style=" margin: 0 auto; max-width: 550px;">
        <div class="layout horizontal center center-justified">
            <div hidden="{{!fabVisible}}" style="margin: 10px 20px 0 0;" title="${msg.backLbl}" >
                <paper-fab mini icon="arrow-back" on-click="back" style="color: #f9f9f9;margin: 0 0 0 20px;background: #ba0011;"></paper-fab>
            </div>
            <div class="flex" style="text-align: center; color:#6c0404;">
                <div>
                    <div style="font-size: 1.4em; font-weight: bold;">{{operationMsg}}</div>
                    <div>{{fromUserName}}</div>
                </div>
            </div>
        </div>
        <div id="formDataDiv" style="padding: 0px 20px 0px 20px; height: 100%;">
            <div class="horizontal layout center center-justified" style="margin: 10px 0 0 0;">
                <div>
                    <input type="text" id="amount" class="form-control"
                           style="width:150px;margin:0 10px 0 0;" pattern="^[0-9]*$" required
                           title="${msg.amountLbl}" placeholder="${msg.amountLbl}"/>
                </div>
                <div><vs-currency-selector id="currencySelector"></vs-currency-selector></div>
            </div>

            <div class="horizontal layout center center-justified">
                <input type="text" id="transactionvsSubject" class="form-control" required
                       style="margin:20px 0 0 0; width:440px;" title="${msg.subjectLbl}" placeholder="${msg.subjectLbl}"/>
            </div>

            <div class="layout horizontal" style="margin:20px 0px 15px 0px; border: 1px solid #ccc; font-size: 1.1em; padding: 5px;">
                <div style="margin:0px 10px 0px 0px; padding:5px;">
                    <div hidden="{{tagSelected}}" class="layout horizontal center center-justified" style="font-size: 0.8em;">
                        <button on-click="showTagDialog" style="font-size: 1em; margin:10px 0px 10px 10px;">
                            <i class="fa fa-tag"></i> ${msg.addTagLbl}
                        </button>
                        <div hidden="{{selectedTags.length == 0}}" style="margin: 0 0 0 10px;">
                            ${msg.transactionvsWithTagAdvertMsg}
                        </div>
                        <div style="max-width: 400px;">{{selectedTagMsg}}</div>
                    </div>
                    <div hidden="{{!tagSelected}}" layout horizontal center center-justified
                         style="font-weight:bold;text-align: center;display: block;">
                        <div class="layout horizontal center center-justified" style="margin: 0 0 20px 0;">
                            <div style="margin: 0 10px 0 0;">
                                <input type="checkbox" id="timeLimitedButton" style="height: 1.3em; width: 1.3em;">
                            </div>
                            <div style="color:#888; font-size: 1.1em;">${msg.timeLimitedAdviceMsg}</div>
                        </div>
                        <div>
                            <div>${msg.selectedTagsAdvice}</div>
                            <template is="dom-repeat" items="{{selectedTags}}">
                                <div style="font-size: 0.9em;">
                                    <div>
                                        <a class="btn btn-default" data-tag-id$='{{item.id}}'  on-click="removeTag"
                                           style="font-size: 0.9em;margin:5px 5px 0px 5px;padding:3px;">
                                            <i class="fa fa-minus" style="color:#6c0404;"></i> <span>{{item.name}}</span>
                                        </a>
                                    </div>
                                </div>
                            </template>
                        </div>
                    </div>
                </div>
            </div>
            <div hidden="{{!isWithUserSelector}}">
                <button on-click="openSearchUserDialog" style="margin: 0 0 5px 5px;font-size: 1.1em;">
                    <i class="fa fa-user"></i> <span>{{selectReceptorMsg}}</span>
                </button>
                <div style="margin:10px 0 0 0;">
                    <vs-user-box flex id="receptorBox" caption="${msg.receptorLbl}"></vs-user-box>
                </div>
            </div>
            <div hidden="{{isWithUserSelector}}" style="margin:10px 0 0 0;text-align:center;font-weight:bold;color:#6c0404;">{{selectReceptorMsg}}</div>
            <div class="flex"></div>
            <div class="layout horizontal" style="margin:10px 20px 0px 0px;">
                <div class="flex"></div>
                <button on-click="submit" style="margin: 0 0 5px 5px;font-size: 1.1em;">
                    <i class="fa fa-check" style="color: #388746;"></i> ${msg.acceptLbl}
                </button>
            </div>
        </div>
    </div>

    <uservs-selector-dialog id="userVSSelectorDialog" groupvs-id="{{groupId}}"></uservs-selector-dialog>
    <tagvs-select-dialog id="tagDialog" caption="${msg.addTagDialogCaption}"></tagvs-select-dialog>

</template>
<script>
    Polymer({
        is:'transactionvs-form',
        properties: {
            fabVisible:{type:Boolean, value:false},
            selectedTags:{type:Array, value:[], observer:'selectedTagsChanged'},
            tagSelected:{type:Boolean, value:false},
            searchString:{type:String, value:null},
            selectedTagMsg:{type:String, value:null}
        },
        ready: function() {
            console.log(this.tagName + " - " + this.id)
            this.operation = null
            this.maxNumberTags = 1
            this.fromUserName = null
            this.fromUserIBAN = null
            this.toUserName = null
            this.groupId = null
            this.selectedTags = []
            this.isWithUserSelector = false
            if(document.querySelector("#ironSignals")) {
                document.querySelector("#ironSignals").addEventListener('iron-signal-user-clicked', function(e) {
                    if(this.$.receptorBox) this.$.receptorBox.addUser(e.detail)
                }.bind(this));
            }
            this.$.tagDialog.addEventListener('tag-selected', function (e) {
                console.log("tag-selected: " + JSON.stringify(e.detail))
                this.selectedTags = e.detail
            }.bind(this))
        },
        openSearchUserDialog:function(){
            this.$.userVSSelectorDialog.show(this.groupId)
        },
        showTagDialog: function() {
            this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
        },
        selectedTagsChanged: function(e) {
            console.log(this.tagName + " - selectedTagsChanged")
            if(this.selectedTags.length > 0) {
                this.selectedTagMsg = "${msg.onlyTagAllowedExpendingMsg}"
                this.tagSelected = true
            } else {
                this.selectedTagMsg = null
                this.tagSelected = false
            }
        },
        removeTag: function(e) {
            var tagToDelete = e.model.item
            for(tagIdx in this.selectedTags) {
                if(tagToDelete.id == this.selectedTags[tagIdx].id) {
                    this.selectedTags.splice(tagIdx, 1)
                }
            }
            this.selectedTags = this.selectedTags.slice(0); //hack to notify array changes
        },
        reset: function() {
            console.log(this.id + " - reset")
            this.removeErrorStyle(this.$.formDataDiv)
            this.isWithUserSelector = false
            this.$.amount.value = ""
            this.$.transactionvsSubject.value = ""
            this.$.timeLimitedButton.checked = false
            this.$.receptorBox.removeUsers()
            this.$.tagDialog.reset()
        },
        removeErrorStyle: function (element) {
            var formElements = element.children
            for(var i = 0; i < element.childNodes.length; i++) {
                var child = element.childNodes[i];
                this.removeErrorStyle(child);
                if(child != undefined) {
                    if(child.style != undefined && child.tagName === 'INPUT') {
                        child.style.background = '#fff'
                        child.classList.remove("formFieldError");
                    }
                }
            }
        },
        submit: function () {
            showMessageVS("${msg.emptyFieldMsg}", "${msg.errorLbl}")
            this.removeErrorStyle(this.$.formDataDiv)
            if(!this.$.amount.validity.valid || this.$.amount.value == 0) {
                //this.$.amount.classList.add("formFieldError")
                this.$.amount.style.background = '#f6ccd0'
                showMessageVS("${msg.enterValidAmountMsg}", "${msg.errorLbl}")
                return
            }
            if(!this.$.transactionvsSubject.validity.valid) {
                //this.$.transactionvsSubject.classList.add("formFieldError")
                this.$.transactionvsSubject.style.background = '#f6ccd0'
                showMessageVS("${msg.emptyFieldMsg}", "${msg.errorLbl}")
                return
            }
            var tagList = []
            if(this.selectedTags.length > 0) {
                for(tagIdx in this.selectedTags) {
                    //tagList.push({id:this.selectedTags[tagIdx].id, name:this.selectedTags[tagIdx].name});
                    tagList.push(this.selectedTags[tagIdx].name);
                }
            } else tagList.push('WILDTAG'); //No tags, receptor can expend money with any tag
            switch(this.operation) {
                case Operation.FROM_GROUP_TO_MEMBER_GROUP:
                    if(this.$.receptorBox.getUserList().length == 0){
                        showMessageVS("${msg.receptorMissingErrorLbl}", "${msg.errorLbl}")
                        return false
                    }
                    break;
                case Operation.FROM_USERVS:
                    return this.transactionFromUser(tagList)
                    break;
            }
            var operationVS = new OperationVS(this.operation)
            operationVS.serviceURL = contextURL + "/rest/transactionVS"
            operationVS.signedMessageSubject = "${msg.transactionvsFromGroupMsgSubject}"
            var signedContent = {operation:this.operation, subject:this.$.transactionvsSubject.value,
                timeLimited:this.$.timeLimitedButton.checked, tags:tagList, amount: this.$.amount.value,
                currencyCode:this.$.currencySelector.getSelected(), fromUser:this.fromUserName,
                fromUserIBAN:this.fromUserIBAN, toUserIBAN: this.getToUserIBAN()}
            if(this.toUserName)  signedContent.toUser = this.toUserName
            operationVS.jsonStr = JSON.stringify(signedContent)
            operationVS.setCallback(function(appMessage) { this.transactionResponse(appMessage)}.bind(this))
            VotingSystemClient.setMessage(operationVS);
        },
        transactionResponse:function(appMessage) {
            var appMessageJSON = JSON.parse(appMessage)
            var caption
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "${msg.transactionvsOKLbl}"
                this.fire('closed')
            } else caption = '${msg.transactionvsERRORLbl}'
            showMessageVS(appMessageJSON.message, caption)
            this.click() //hack to refresh screen
        },
        transactionFromUser:function(tagList) {
            var operationVS = new OperationVS(this.operation)
            operationVS.serviceURL = contextURL + "/rest/transactionVS"
            operationVS.signedMessageSubject = "${msg.transactionVSFromUserVS}"
            var signedContent = {operation:this.operation, subject:this.$.transactionvsSubject.value,
                timeLimited:this.$.timeLimitedButton.checked, tags:tagList, amount: this.$.amount.value,
                currencyCode:this.$.currencySelector.getSelected(), toUser:this.toUserName, toUserIBAN: this.getToUserIBAN()}
            operationVS.jsonStr = JSON.stringify(signedContent)
            operationVS.setCallback(this.transactionVSCallback)
            VotingSystemClient.setMessage(operationVS);
        },
        transactionVSCallback: function (appMessage) {
            var appMessageJSON = toJSON(appMessage)
            var caption
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "${msg.transactionvsOKLbl}"
            } else caption = '${msg.transactionvsERRORLbl}'
            showMessageVS(appMessageJSON.message, caption)
        },
        getToUserIBAN: function () {
            var result = []
            if(this.operation === Operation.FROM_GROUP_TO_ALL_MEMBERS) return null
            else if(this.operation === Operation.FROM_USERVS) {
                result.push(this.toUserIBAN);
            } else {
                var receptorList = this.$.receptorBox.getUserList()
                for(userIdx in receptorList) {
                    result.push(receptorList[userIdx].iban);
                }
            }
            return result
        },
        back:function() {
            this.fire('closed')
        },
        init:function(operation, userName, userIBAN, userVSId) {
            console.log(this.id + " - init - operation: " + operation + " - fabVisible: " + this.fabVisible +
                    " - userIBAN: " + userIBAN)
            this.reset()
            this.operation = operation
            this.fromUserName = userName
            this.fromUserIBAN = userIBAN
            this.toUserIBAN = null
            this.groupId = userVSId
            this.$.transactionvsSubject.value = ""
            this.$.amount.value = ""
            this.isWithUserSelector = true
            this.toUserName = null
            switch(operation) {
                case Operation.FROM_GROUP_TO_MEMBER_GROUP:
                    this.operationMsg = "${msg.transactionVSFromGroupToMemberGroup}"
                    this.selectReceptorMsg = '${msg.selectReceptorsMsg}'
                    this.$.receptorBox.multiSelection = true
                    break;
                case Operation.FROM_GROUP_TO_ALL_MEMBERS:
                    this.isWithUserSelector = false
                    this.operationMsg = "${msg.transactionVSFromGroupToAllMembers}"
                    this.selectReceptorMsg = '${msg.transactionvsToAllGroupMembersMsg}'
                    break;
                case Operation.FROM_USERVS:
                    this.operationMsg = "${msg.transactionVSFromUserVS} ${msg.forLbl} '" +
                            userName + "'"
                    this.fromUserName = null
                    this.fromUserIBAN = null
                    this.toUserIBAN = userIBAN
                    this.isWithUserSelector = false
                    this.toUserName = userName
                    this.$.receptorBox.uservsList= [{name:userName, IBAN:userIBAN}]
                    break;
            }
            this.selectedTags = []
            this.tagSelected =  false
            return this.caption
        }
    });
</script>
</dom-module>
