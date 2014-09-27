<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-user-box', file: 'votingsystem-user-box.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-checkbox', file: 'paper-checkbox.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/vicketTagVS/tagvs-select-dialog']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-selector-dialog']"/>">

<polymer-element name="transactionvs-form" attributes="subpage">
<template>
        <g:include view="/include/styles.gsp"/>
        <style no-shim>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .container{
            margin: 0 auto;
            max-width: 550px;
        }
        </style>
        <div class="container">

            <div layout horizontal center center-justified style="">
                <template if="{{subpage}}">
                    <div style="margin: 20px 0 0 0;" title="<g:message code="backLbl"/>" >
                        <paper-fab icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                    </div>
                </template>
                <div flex style=" margin:0 0 0 20px; color:#6c0404;">
                    <div>
                        <div style="font-size: 1.5em; font-weight: bold;">{{operationMsg}}</div>
                        <div>{{fromUserName}}</div>
                    </div>
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

            <div layout vertical id="formDataDiv" style="padding: 15px 20px 0px 20px; height: 100%;">

                <div layout horizontal center center-justified>
                    <div style="margin: 0 10px 0 0;"><paper-checkbox id="timeLimitedCheckBox"></paper-checkbox></div>
                    <div><h4><g:message code="timeLimitedAdviceMsg"/></h4></div>
                </div>

                <div>
                    <input type="text" id="amount" class="form-control" style="width:150px;margin:0 0 10px 0;" pattern="^[0-9]*$" required
                           title="<g:message code="amountLbl"/>"
                           placeholder="<g:message code="amountLbl"/> (EUR)"/>
                    <input type="text" id="transactionvsSubject" class="form-control" style="width:350px;" required
                           title="<g:message code="subjectLbl"/>" placeholder="<g:message code="subjectLbl"/> (EUR)"/>
                </div>

                <div  layout horizontal style="margin:15px 0px 15px 0px; border: 1px solid #ccc;
                    font-size: 1.1em; padding: 5px;display: block;}}">
                    <div style="margin:0px 10px 0px 0px; padding:5px;">
                        <div layout horizontal style="font-size: 0.8em;">
                            <div style="max-width: 400px;display: {{selectedTags.length == 0? 'block':'none'}};">
                                <g:message code="transactionvsWithTagAdvertMsg"/>
                            </div>
                            <div style="max-width: 400px;">{{selectedTagMsg}}</div>
                            <votingsystem-button on-click="{{showTagDialog}}" style="font-size: 0.9em;margin:10px 0px 10px 10px;display:{{(isPending || isCancelled ) ? 'none':'block'}} ">
                                <i class="fa fa-tag" style="margin:0 7px 0 3px;"></i> <g:message code="addTagLbl"/>
                            </votingsystem-button>
                        </div>
                        <div layout horizontal center center-justified style="font-weight:bold;font-size: 0.8em;
                            display: {{selectedTags.length == 0? 'none':'block'}};">
                            <g:message code="selectedTagsLbl"/>
                            <template repeat="{{tag in selectedTags}}">
                                <a class="btn btn-default" data-tagId='{{tag.id}}' on-click="{{removeTag}}"
                                   style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                    <i class="fa fa-minus"></i> {{tag.name}}</a>
                            </template>
                        </div>
                    </div>
                </div>
                <div style="display:{{isTransactionVSFromGroupToAllMembers ? 'none':'block'}}">
                    <votingsystem-button on-click="{{openSearchUserDialog}}" style="margin: 0 0 5px 5px;">
                        <i class="fa fa-user" style="margin:0 7px 0 3px;"></i> {{selectReceptorMsg}}
                    </votingsystem-button>
                    <div style="margin:10px 0 0 0;">
                        <votingsystem-user-box flex id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></votingsystem-user-box>
                    </div>
                </div>
                <div flex>
                </div>
                <div layout horizontal style="margin:10px 20px 0px 0px;">
                    <div flex></div>
                    <votingsystem-button on-click="{{submitForm}}" style="margin: 20px 0px 0px 5px;">
                        <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                    </votingsystem-button>
                </div>
            </div>
        </div>

        <uservs-selector-dialog id="searchDialog"></uservs-selector-dialog>
    <div>
        <div layout horizontal center center-justified style="padding:100px 0px 0px 0px;margin:0px auto 0px auto;">
            <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></tagvs-select-dialog>
        </div>
    </div>
</template>
<script>
    Polymer('transactionvs-form', {
        operation:null,
        maxNumberTags:1,
        fromUserName:null,
        fromUserIBAN:null,
        groupId:null,
        selectedTags: [],
        subpage:false,
        ready: function() {
            console.log(this.tagName + " - " + this.id)
            this.isTransactionVSFromGroupToAllMembers = false

            document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
                this.$.receptorBox.addUser(e.detail)
            }.bind(this));

            this.$.tagDialog.addEventListener('tag-selected', function (e) {
                console.log("tag-selected: " + JSON.stringify(e.detail))
                this.selectedTags = e.detail
            }.bind(this))

        },
        openSearchUserDialog:function(){
            this.$.searchDialog.show()
        },
        showTagDialog: function() {
            this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
        },
        selectedTagsChanged: function(e) {
            if(this.selectedTags.length > 0) this.selectedTagMsg = "<g:message code="onlyTagAllowedExpendingMsg"/>"
            else this.selectedTagMsg = null
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
            this.isTransactionVSFromGroupToAllMembers = false
            this.$.amount.value = ""
            this.$.transactionvsSubject.value = ""
            this.setMessage(200, null)
            this.$.receptorBox.removeUsers()
            this.$.tagDialog.reset()
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
                case Operation.TRANSACTIONVS_FROM_GROUP_TO_MEMBER:
                    if(this.$.receptorBox.getUserList().length == 0){
                        this.setMessage(500, "<g:message code='receptorMissingErrorLbl'/>")
                        return false
                    }
                    break;
                case Operation.TRANSACTIONVS_FROM_GROUP_TO_MEMBER_GROUP:
                    if(this.$.receptorBox.getUserList().length == 0){
                        this.setMessage(500, "<g:message code='receptorMissingErrorLbl'/>")
                        return false
                    }
                    break;
                case Operation.TRANSACTIONVS_FROM_GROUP_TO_ALL_MEMBERS:
                    break;
            }
            if(!this.$.amount.validity.valid) {
                //this.$.amount.classList.add("formFieldError")
                this.$.amount.style.background = '#f6ccd0'
                this.setMessage(500, "<g:message code='emptyFieldMsg'/>")
                return
            }

            if(!this.$.transactionvsSubject.validity.valid) {
                //this.$.transactionvsSubject.classList.add("formFieldError")
                this.$.transactionvsSubject.style.background = '#f6ccd0'
                this.setMessage(500, "<g:message code='emptyFieldMsg'/>")
                return
            }
            this.setMessage(200, null)

            var tagList = []
            if(this.selectedTags.length > 0) {
                for(tagIdx in this.selectedTags) {
                    tagList.push({id:this.selectedTags[tagIdx].id, name:this.selectedTags[tagIdx].name});
                }
            } else tagList.push({id:1, name:'WILDTAG'}); //No tags, receptor can expend money with any tag

            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, this.operation)
            webAppMessage.serviceURL = "${createLink( controller:'transactionVS', action:" ", absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code='transactionvsFromGroupMsgSubject'/>"
            webAppMessage.signedContent = {operation:this.operation, subject:this.$.transactionvsSubject.value,
                isTimeLimited:this.$.timeLimitedCheckBox.checked, toUserIBAN:this.toUserIBAN(), tags:tagList,
                amount: this.$.amount.value, currency:"EUR", fromUser:this.fromUserName, fromUserIBAN:this.fromUserIBAN}
            webAppMessage.setCallback(function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    var caption
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='transactionvsOKLbl'/>"
                        this.fire('operation-finished')
                    } else caption = '<g:message code="transactionvsERRORLbl"/>'
                    showMessageVS(appMessageJSON.message, caption)
                }.bind(this))
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
        setMessage:function(status, message) {
            console.log(this.tagName + " - setMessage - status: " + status, " - message: " + message)
            this.status = status
            this.messageToUser = message
        },
        back:function() {
            this.fire('operation-finished')
        },
        init:function(operation, fromUser, fromIBAN, targetGroupId) {
            console.log(this.id + " - init - operation: " + operation + " - subpage: " + this.subpage)
            this.operation = operation
            this.fromUserName = fromUser
            this.fromUserIBAN = fromIBAN
            this.groupId = targetGroupId
            this.isTransactionVSFromGroupToAllMembers = false
            this.$.timeLimitedCheckBox.checked = false
            switch(operation) {
                case Operation.TRANSACTIONVS_FROM_GROUP_TO_MEMBER:
                    this.operationMsg = "<g:message code='transactionVSFromGroupToMember'/>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorMsg"/>'
                    this.$.receptorBox.multiSelection = false
                    break;
                case Operation.TRANSACTIONVS_FROM_GROUP_TO_MEMBER_GROUP:
                    this.operationMsg = "<g:message code='transactionVSFromGroupToMemberGroup'/>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorsMsg"/>'
                    this.$.receptorBox.multiSelection = true
                    break;
                case Operation.TRANSACTIONVS_FROM_GROUP_TO_ALL_MEMBERS:
                    this.isTransactionVSFromGroupToAllMembers = true
                    this.operationMsg = "<g:message code='transactionVSFromGroupToAllMembers'/>"
                    this.selectReceptorMsg = '<g:message code="transactionvsToAllGroupMembersMsg"/>'
                    break;
            }
            this.selectedTags = []
            return this.caption
        }
    });
</script>
</polymer-element>
