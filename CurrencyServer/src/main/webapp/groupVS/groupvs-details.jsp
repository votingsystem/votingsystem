<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>

<link href="../transactionVS/transactionvs-form.vsp" rel="import"/>
<link href="../userVS/uservs-list.vsp" rel="import"/>
<link href="../groupVS/groupvs-user.vsp" rel="import"/>

<dom-module name="groupvs-details">
<template>
    <iron-ajax auto url="{{url}}" method="get" last-response="{{groupvs}}" handle-as="json" content-type="application/json"></iron-ajax>
    <paper-dialog id="configDialog" with-backdrop no-cancel-on-outside-click style="border: 1px solid #6c0404;">
        <div style="width: 350px;">
            <div class="layout horizontal center center-justified">
                <div hidden="{{!caption}}" flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;">${msg.configGroupvsLbl}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" onclick="javascript:document.querySelector('#configDialog').opened = false;"></i>
                </div>
            </div>
            <div class="vertical layout center center-justified" style="padding: 20px 10px 10px 10px;">
                <button on-click="editGroup" style="margin: 10px; width: 150px;">${msg.editDataLbl}</button>
                <button on-click="cancelGroup" style="margin: 10px; width: 150px;">${msg.cancelGroupVSLbl}</button>
            </div>
        </div>
    </paper-dialog>

    <paper-dialog id="transactionSelectorDialog" with-backdrop no-cancel-on-outside-click style="border: 1px solid #6c0404;">
        <div style="width: 350px;">
            <div class="layout horizontal center center-justified">
                <div hidden="{{!caption}}" flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;">${msg.makeTransactionVSFromGroupVSLbl}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" onclick="javascript:document.querySelector('#transactionSelectorDialog').opened = false;"></i>
                </div>
            </div>
            <div class="vertical layout center center-justified" style="padding: 20px 10px 10px 10px;">
                <button on-click="fromGroupToMemberGroup" style="margin: 10px; width: 250px;">${msg.makeTransactionVSFromGroupVSToMemberGroupLbl}</button>
                <button on-click="fromGroupToAllMember" style="margin: 10px; width: 250px;">${msg.makeTransactionVSFromGroupVSToAllMembersLbl}</button>
            </div>
        </div>
    </paper-dialog>

    <iron-signals on-iron-signal-messagedialog-accept="messagedialogAccepted"
                  on-iron-signal-uservs-selected="showUserDetails" ></iron-signals>

    <div hidden="{{detailsHidden}}" style="max-width: 900px; margin:0 auto;">
        <div class="vertical layout flex">
            <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.1em;display:none;">
            </div>
            <div hidden="{{!isAdminView}}">
                <div class="layout horizontal center center-justified">
                    <div>
                        <button onclick="javascript:document.querySelector('#configDialog').opened = true" style="margin: 10px;">
                            <i class="fa fa-cogs"></i> ${msg.configGroupvsLbl}</button>
                    </div>
                    <div>
                        <button onclick="javascript:document.querySelector('#transactionSelectorDialog').opened = true">
                            <i class="fa fa-money"></i> ${msg.makeTransactionVSFromGroupVSLbl}</button>
                    </div>
                </div>
            </div>
            <div hidden="{{!isUserView}}" class="layout horizontal center center-justified" style="margin:0 0 10px 10px;">
                <button style="margin:0 20px 0 0;" on-click="subscribeToGroup">
                    <i class="fa fa-sign-in"></i> ${msg.subscribeGroupVSLbl}
                </button>
                <button style="margin:0 20px 0 0; font-size: 1em;" on-click="sendTransactionVS">
                    <i class="fa fa-money"></i> ${msg.sendTransactionVSLbl}
                </button>
            </div>
            <div hidden="{{isClientToolConnected}}" id="clientToolMsg" class="text-center"
                 style="color:#6c0404; font-size: 1.2em;margin:30px 0 20px 0;">
                <f:view>
                    <h:outputFormat value="#{msg.clientToolNeededMsg}" escape="false">
                        <f:param value="#{contextURL}/tools/ClientTool.zip"/>
                    </h:outputFormat>
                    <br/>
                    <h:outputFormat value="#{msg.clientToolDownloadMsg}" escape="false">
                        <f:param value="#{contextURL}/app/tools.xhtml"/>
                    </h:outputFormat>
                </f:view>
            </div>
            <div id="pageHeader" class="layout horizontal center center-justified">
                <div class="horizontal layout flex" style="padding:7px 0px 0px 7px;">
                    <div hidden="{{!fabVisible}}" style="margin: 10px 20px 10px 0;" title="${msg.backLbl}" >
                        <paper-fab mini icon="arrow-back" on-click="back"
                                   style="color: #f9f9f9;margin: 0 0 0 20px;background: #ba0011;"></paper-fab>
                    </div>
                </div>
                <div style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;" data-groupvs-id$="{{groupvs.id}}">{{groupvs.name}}</div>
                </div>
                <div class="flex" style="margin:5px 10px 0 0; font-size: 0.7em; color:#888; text-align: right; vertical-align: bottom;">
                    <b>${msg.IBANLbl}:</b> <span>{{groupvs.iban}}</span>
                </div>
            </div>
        </div>
        <div class="eventContentDiv">
            <vs-html-echo html="{{decodeBase64(groupvs.description)}}"></vs-html-echo>
        </div>
        <div class="layout horizontal">
            <div style="margin: 5px 30px 0 0;">
                <button on-click="goToWeekBalance"><i class="fa fa-bar-chart"></i> ${msg.goToWeekBalanceLbl}</button>
            </div>
            <div hidden="{{tagsHidden}}" class="layout horizontal center center-justified"
                    style="margin: 5px 0 0 0;">
                <template is="dom-repeat" items="{{groupvs.tags}}">
                    <a class="btn btn-default" style="font-size: 0.7em;margin:0px 5px 0px 0px;padding:3px;">
                        <i class="fa fa-tag" style="color:#888; margin: 0 5px 0 0;"></i><span>{{item.name}}</span></a>
                </template>
            </div>
            <div class="flex"></div>
            <div style="margin:5px 10px 0 0; font-size: 0.9em; color:#888;">
                <b>${msg.representativeLbl}:</b> <span>{{representativeName}}</span>
            </div>
        </div>

        <div style="min-height: 300px; border-top: 1px solid #ccc; margin: 15px 0 0 0; padding: 10px 0 0 0;">
            <div  style="text-align:center; font-size: 1.3em;font-weight: bold; color: #888;margin: 0 0 10px 0;
                text-decoration: underline;">
                ${msg.usersLbl}
            </div>
            <div style="margin: 0 0 100px 0;">
                <uservs-list id="userList"></uservs-list>
            </div>
        </div>
    </div>

    <div hidden="{{!detailsHidden}}">
        <transactionvs-form id="transactionvsForm" fab-visible="true"></transactionvs-form>
    </div>

    <groupvs-user id="userDescription"></groupvs-user>
</template>
<script>
    Polymer({
        is:'groupvs-details',
        properties: {
            fabVisible:{type:Boolean, value:false},
            groupvs:{type:Object, value:{}, observer:'groupvsChanged'},
            representativeName:{type:String},
            url:{type:String},
            tagsHidden:{type:Boolean}
        },
        ready:function() {
            console.log(this.tagName + " - ready ")
            this.isSelected = false
            this.detailsHidden = false
            //this.isClientToolConnected = window['isClientToolConnected']
            this.isClientToolConnected = true
            if(!this.isClientToolConnected) {
                this.isAdminView = false
                this.isUserView = false
            }
            this.$.transactionvsForm.addEventListener('closed', function (e) {
                this.detailsHidden = false;
            }.bind(this))
        },
        decodeBase64:function(base64EncodedString) {
            if(base64EncodedString == null) return null
            return decodeURIComponent(escape(window.atob(base64EncodedString)))
        },
        closeTransactionSelectorDialog:function() {
            this.$.transactionSelectorDialog.opened = false
        },
        closeConfigDialog:function() {
            this.$.configDialog.opened = false
        },
        sendTransactionVS:function() {
            console.log(this.tagName + " - sendTransactionVS")
            this.$.transactionvsForm.init(Operation.FROM_USERVS, this.groupvs.name, this.groupvs.iban , this.groupvs.id)
            this.detailsHidden = true;
        },
        messagedialogAccepted:function(e, detail, sender) {
            console.log(this.tagName + ".messagedialogAccepted")
            if('cancel_group' == detail.callerId) {
                var operationVS = new OperationVS(Operation.CURRENCY_GROUP_CANCEL)
                operationVS.serviceURL = contextURL + "/rest/groupVS/id/" + this.groupvs.id + "/cancel"
                operationVS.signedMessageSubject = "${msg.cancelGroupVSSignedMessageSubject}"
                operationVS.jsonStr = JSON.stringify({operation:Operation.CURRENCY_GROUP_CANCEL, name:this.groupvs.name,
                    id:this.groupvs.id})
                operationVS.setCallback(function(appMessage) { this.cancelResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS);
                this.appMessageJSON = null
            }
        },
        cancelResponse:function(appMessage) {
            this.appMessageJSON = JSON.parse(appMessage)
            var caption = '${msg.groupCancelERRORLbl}'
            if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                caption = "${msg.groupCancelOKLbl}"
                page.show(contextURL + "/rest/groupVS/id/" + this.groupvs.id)
            }
            showMessageVS(this.appMessageJSON.message, caption, this.tagName)
            this.click()
        },
        goToWeekBalance:function() {
            page.show(contextURL + "/rest/balance/userVS/id/" + this.groupvs.id)
        },
        subscribeToGroup: function () {
            console.log("subscribeToGroup")
            var representative = {id:this.groupvs.representative.id, nif:this.groupvs.representative.nif}
            var operationVS = new OperationVS(Operation.CURRENCY_GROUP_SUBSCRIBE)
            operationVS.serviceURL = contextURL + "/rest/groupVS/id/" + this.groupvs.id + "/subscribe"
            operationVS.signedMessageSubject = "${msg.subscribeToCurrencyGroupMsg}"
            operationVS.jsonStr = JSON.stringify({operation:Operation.CURRENCY_GROUP_SUBSCRIBE,
                id:this.groupvs.id, name:this.groupvs.name , representative:representative})
            operationVS.setCallback(function(appMessage) { this.subscribeResponse(appMessage)}.bind(this))
            VotingSystemClient.setMessage(operationVS);
        },
        subscribeResponse:function(appMessage) {
            console.log(this.tagName + " - subscribeResponse - message: " + appMessage);
            var appMessageJSON = JSON.parse(appMessage)
            var caption
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "${msg.groupSubscriptionOKLbl}"
                page.show(contextURL + "/rest/groupVS/id/" + this.groupvs.id)
            } else caption = '${msg.groupSubscriptionERRORLbl}'
            showMessageVS(appMessageJSON.message, caption)
            this.click() //hack to refresh screen
        },
        groupvsChanged:function() {
            console.log(this.tagName + " - groupvsChanged - menuType: " + menuType)
            this.isAdminView = false
            this.isUserView = false
            if(("admin" == menuType || "superuser" == menuType) && 'ACTIVE' == this.groupvs.state) this.isAdminView = true
            if("user" === menuType && 'ACTIVE' === this.groupvs.state) this.isUserView = true

            if('ACTIVE' == this.groupvs.state) {
                this.$.messagePanel.style.display = 'none'
            } else if('PENDING' == this.groupvs.state) {
                this.$.pageHeader.style.color = "#fba131"
                this.$.messagePanel.classList.add("groupvsPendingBox");
                this.$.messagePanel.innerHTML = "${msg.groupvsPendingLbl}"
                this.$.messagePanel.style.display = 'block'
            } else if('CANCELED' == this.groupvs.state) {
                this.$.pageHeader.style.color = "#6c0404"
                this.$.messagePanel.classList.add("groupvsClosedBox");
                this.$.messagePanel.innerHTML = "${msg.groupvsClosedLbl}"
                this.$.messagePanel.style.display = 'block'
                this.isAdminView = false
            }
            if(this.groupvs.representative) this.representativeName =
                    this.groupvs.representative.firstName + " " + this.groupvs.representative.lastName
            if(this.groupvs.tags) this.tagsHidden = (this.groupvs.tags.length === 0)
            this.$.userList.url = contextURL + "/rest/groupVS/id/" + this.groupvs.id + "/listUsers"
            this.fire('iron-signal', {name: "vs-innerpage", data: {caption:"${msg.groupvsLbl}"}});
            if(this.groupvs.id) this.$.userList.loadGroupUsers(this.groupvs.id)
            console.log("this.isUserView: " + this.isUserView + " - groupvs.state: " + this.groupvs.state +
                " - menuType: " + menuType)
        },
        fromGroupToAllMember:function() {
            this.$.transactionvsForm.init(Operation.FROM_GROUP_TO_ALL_MEMBERS, this.groupvs.name,
                    this.groupvs.iban, this.groupvs.id)
            this.detailsHidden = true
            this.$.transactionSelectorDialog.opened = false
        },
        fromGroupToMemberGroup:function() {
            this.$.transactionvsForm.init(Operation.FROM_GROUP_TO_MEMBER_GROUP, this.groupvs.name,
                    this.groupvs.iban, this.groupvs.id)
            this.detailsHidden = true
            this.$.transactionSelectorDialog.opened = false
        },
        cancelGroup:function() {
            showMessageVS("${msg.cancelGroupVSDialogMsg}".format(this.groupvs.name),
                    "${msg.confirmOperationMsg}", 'cancel_group', true)
        },
        editGroup:function() {
            var operationVS = new OperationVS(Operation.CURRENCY_GROUP_EDIT)
            operationVS.message = this.groupvs.id
            VotingSystemClient.setMessage(operationVS);
        },
        back:function() {
            this.fire('iron-signal', {name: "groupvs-details-closed", data: this.groupvs.id});
        },
        showUserDetails:function(e, detail, sender) {
            console.log(this.tagName + " - showUserDetails")
            this.$.userDescription.show(contextURL + "/rest/groupVS/id/" + this.groupvs.id + "/user/id", detail)
        }
    })
</script>
</dom-module>
