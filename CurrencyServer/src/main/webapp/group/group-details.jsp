<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>

<link href="../transaction/transaction-form.vsp" rel="import"/>
<link href="../user/user-list.vsp" rel="import"/>
<link href="../group/group-user.vsp" rel="import"/>

<dom-module name="group-details">
<template>
    <div id="configDialog" class="modalDialog">
        <div style="width: 350px;">
            <div class="layout horizontal center center-justified">
                <div hidden="{{!caption}}" flex style="font-size: 1.4em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;">${msg.configGroupLbl}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" on-click="closeConfigDialog"></i>
                </div>
            </div>
            <div class="vertical layout center center-justified" style="padding: 20px 10px 10px 10px;">
                <button on-click="editGroup" style="margin: 10px; width: 150px;">${msg.editDataLbl}</button>
                <button on-click="cancelGroup" style="margin: 10px; width: 150px;">${msg.cancelGroupLbl}</button>
            </div>
        </div>
    </div>

    <div id="transactionSelectorDialog" class="modalDialog">
        <div style="width: 400px;">
            <div class="layout horizontal center center-justified">
                <div hidden="{{!caption}}" flex style="font-size: 1.4em; font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;">${msg.makeTransactionFromGroupLbl}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" on-click="closeTransactionSelectorDialog"></i>
                </div>
            </div>
            <div class="vertical layout center center-justified" style="padding: 20px 10px 10px 10px;">
                <button on-click="fromGroupToMemberGroup" style="margin: 10px; width: 250px;">${msg.makeTransactionFromGroupToMemberGroupLbl}</button>
                <button on-click="fromGroupToAllMember" style="margin: 10px; width: 250px;">${msg.makeTransactionFromGroupToAllMembersLbl}</button>
            </div>
        </div>
    </div>

    <div style="max-width: 900px; margin:0 auto;">
        <div class="vertical layout flex">
            <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.1em;display:none;">
            </div>
            <div hidden="{{!isAdminView}}">
                <div class="layout horizontal center center-justified">
                    <div>
                        <button on-click="openConfigDialog" style="margin: 10px;">
                            <i class="fa fa-cogs"></i> ${msg.configGroupLbl}</button>
                    </div>
                    <div>
                        <button on-click="openTransactionSelectorDialog">
                            <i class="fa fa-money"></i> ${msg.makeTransactionFromGroupLbl}</button>
                    </div>
                </div>
            </div>
            <div hidden="{{!isUserView}}" class="layout horizontal center center-justified" style="margin:0 0 10px 10px;">
                <button style="margin:0 20px 0 0;" on-click="subscribeToGroup">
                    <i class="fa fa-sign-in"></i> ${msg.subscribeGroupLbl}
                </button>
                <button style="margin:0 20px 0 0; font-size: 1em;" on-click="sendTransaction">
                    <i class="fa fa-money"></i> ${msg.sendTransactionLbl}
                </button>
            </div>
            <div hidden="{{isClientToolConnected}}" id="clientToolMsg" class="text-center"
                 style="color:#6c0404; font-size: 1.2em;margin:30px 0 20px 0;">
                <f:view>
                    <h:outputFormat value="#{msg.nativeClientURLMsg}" escape="false">
                        <f:param value="#{contextURL}/tools/ClientTool.zip"/>
                    </h:outputFormat>
                    <br/>
                    <h:outputFormat value="#{msg.clientToolDownloadMsg}" escape="false">
                        <f:param value="#{contextURL}/app/tools.xhtml"/>
                    </h:outputFormat>
                </f:view>
            </div>
            <div id="pageHeader" class="layout horizontal center center-justified">
                <div class="horizontal layout center center-justified" style="font-size: 1.5em;
                    margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;" data-group-id$="{{group.id}}">{{group.name}}</div>
                </div>
                <div class="flex" style="margin:5px 10px 0 0; font-size: 0.7em; color:#888; text-align: right; vertical-align: bottom;">
                    <b>${msg.IBANLbl}:</b> <span>{{group.iban}}</span>
                </div>
            </div>
        </div>
        <div id="contentDiv" class="contentDiv"></div>
        <div class="layout horizontal">
            <div style="margin: 5px 30px 0 0;">
                <button on-click="goToWeekBalance"><i class="fa fa-bar-chart"></i> ${msg.goToWeekBalanceLbl}</button>
            </div>
            <div hidden="{{tagsHidden}}" class="layout horizontal center center-justified" style="margin: 5px 0 0 0;">
                <template is="dom-repeat" items="{{group.tags}}">
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
                <user-list id="userList"></user-list>
            </div>
        </div>
    </div>

    <div id="transactionFormDialog" class="modalDialog">
        <div style="width:600px;">
            <div style="position: absolute; top: 0px; right: 0px;">
                <i class="fa fa-times closeIcon" on-click="closeDialog"></i>
            </div>
            <transaction-form id="transactionForm"></transaction-form>
        </div>
    </div>

    <group-user id="userDescription"></group-user>
</template>
<script>
    Polymer({
        is:'group-details',
        properties: {
            group:{type:Object, observer:'groupChanged'},
            representativeName:{type:String},
            url:{type:String, observer:'getHTTP'},
            tagsHidden:{type:Boolean}
        },
        ready:function() {
            console.log(this.tagName + " - ready ")
            this.isSelected = false
            this.isClientToolConnected = true
            if(!this.isClientToolConnected) {
                this.isAdminView = false
                this.isUserView = false
            }
            document.querySelector("#voting_system_page").addEventListener('user-selected',
                    function(e) { this.showUserDetails(e) }.bind(this))
        },
        closeDialog:function() {
            this.$.certDetailsDialog.style.opacity = 0
            this.$.certDetailsDialog.style['pointer-events'] = 'none'
        },
        sendTransaction:function() {
            console.log(this.tagName + " - sendTransaction")
            this.$.transactionForm.init(Operation.FROM_USER, this.group.name, this.group.iban , this.group.id)
            this.$.transactionFormDialog.style.opacity = 1
            this.$.transactionFormDialog.style['pointer-events'] = 'auto'
        },
        messagedialogAccepted:function(e) {
            console.log(this.tagName + ".messagedialogAccepted")
            if('cancel_group' == e.detail) {
                var operationVS = new OperationVS(Operation.CURRENCY_GROUP_CANCEL)
                operationVS.serviceURL = vs.contextURL + "/rest/group/id/" + this.group.id + "/cancel"
                operationVS.signedMessageSubject = "${msg.cancelGroupSignedMessageSubject}"
                operationVS.jsonStr = JSON.stringify({operation:Operation.CURRENCY_GROUP_CANCEL, name:this.group.name,
                    id:this.group.id})
                operationVS.setCallback(function(appMessage) { this.cancelResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS);
                this.appMessageJSON = null
            }
        },
        cancelResponse:function(appMessage) {
            this.appMessageJSON = appMessage
            var caption = '${msg.groupCancelERRORLbl}'
            if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                caption = "${msg.groupCancelOKLbl}"
                page.show(vs.contextURL + "/rest/group/id/" + this.group.id)
            }
            alert(this.appMessageJSON.message, caption)
        },
        goToWeekBalance:function() {
            page.show(vs.contextURL + "/rest/balance/user/id/" + this.group.id)
        },
        subscribeToGroup: function () {
            console.log("subscribeToGroup")
            var representative = {id:this.group.representative.id, nif:this.group.representative.nif}
            var operationVS = new OperationVS(Operation.CURRENCY_GROUP_SUBSCRIBE)
            operationVS.serviceURL = vs.contextURL + "/rest/group/id/" + this.group.id + "/subscribe"
            operationVS.signedMessageSubject = "${msg.subscribeToCurrencyGroupMsg}"
            operationVS.jsonStr = JSON.stringify({operation:Operation.CURRENCY_GROUP_SUBSCRIBE,
                id:this.group.id, name:this.group.name , representative:representative})
            operationVS.setCallback(function(appMessage) { this.subscribeResponse(appMessage)}.bind(this))
            VotingSystemClient.setMessage(operationVS);
        },
        subscribeResponse:function(appMessageJSON) {
            console.log(this.tagName + " - subscribeResponse");
            var caption
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "${msg.groupSubscriptionOKLbl}"
                page.show(vs.contextURL + "/rest/group/id/" + this.group.id)
            } else caption = '${msg.groupSubscriptionERRORLbl}'
            alert(appMessageJSON.message, caption)
        },
        groupChanged:function() {
            console.log(this.tagName + " - groupChanged - menuType: " + menuType)
            this.isAdminView = false
            this.isUserView = false
            if(("admin" == menuType || "superuser" == menuType) && 'ACTIVE' == this.group.state) this.isAdminView = true
            if("user" === menuType && 'ACTIVE' === this.group.state) this.isUserView = true

            if('ACTIVE' == this.group.state) {
                this.$.messagePanel.style.display = 'none'
            } else if('PENDING' == this.group.state) {
                this.$.pageHeader.style.color = "#fba131"
                this.$.messagePanel.classList.add("groupPendingBox");
                this.$.messagePanel.innerHTML = "${msg.groupPendingLbl}"
                this.$.messagePanel.style.display = 'block'
            } else if('CANCELED' == this.group.state) {
                this.$.pageHeader.style.color = "#6c0404"
                this.$.messagePanel.classList.add("groupClosedBox");
                this.$.messagePanel.innerHTML = "${msg.groupClosedLbl}"
                this.$.messagePanel.style.display = 'block'
                this.isAdminView = false
            }
            if(this.group.representative) this.representativeName =
                    this.group.representative.firstName + " " + this.group.representative.lastName
            if(this.group.tags) this.tagsHidden = (this.group.tags.length === 0)
            this.$.userList.url = vs.contextURL + "/rest/group/id/" + this.group.id + "/listUsers"
            if(this.group.id) this.$.userList.loadGroupUsers(this.group.id)
            this.$.contentDiv.innerHTML = window.atob(this.group.description)
            console.log("this.isUserView: " + this.isUserView + " - group.state: " + this.group.state +
                " - menuType: " + menuType)
        },
        fromGroupToAllMember:function() {
            this.$.transactionForm.init(Operation.FROM_GROUP_TO_ALL_MEMBERS, this.group.name,
                    this.group.iban, this.group.id)
            this.closeTransactionSelectorDialog()
        },
        fromGroupToMemberGroup:function() {
            this.$.transactionForm.init(Operation.FROM_GROUP_TO_MEMBER_GROUP, this.group.name,
                    this.group.iban, this.group.id)
            this.closeTransactionSelectorDialog()
        },
        closeTransactionSelectorDialog:function() {
            this.$.transactionSelectorDialog.style.opacity = 0
            this.$.transactionSelectorDialog.style['pointer-events'] = 'none'
        },
        openTransactionSelectorDialog:function() {
            this.$.transactionSelectorDialog.style.opacity = 1
            this.$.transactionSelectorDialog.style['pointer-events'] = 'auto'
        },
        closeConfigDialog:function() {
            this.$.configDialog.style.opacity = 0
            this.$.configDialog.style['pointer-events'] = 'none'
        },
        openConfigDialog:function() {
            this.$.configDialog.style.opacity = 1
            this.$.configDialog.style['pointer-events'] = 'auto'
        },
        cancelGroup:function() {
            alert("${msg.cancelGroupDialogMsg}".format(this.group.name), "${msg.confirmOperationMsg}", this.messagedialogAccepted.bind(this))
        },
        editGroup:function() {
            vs.group = this.group
            page.show("/group_editor")
        },
        showUserDetails:function(e) {
            console.log(this.tagName + " - showUserDetails - user id: " + e.detail)
            this.$.userDescription.show(vs.contextURL + "/rest/group/id/" + this.group.id + "/user/id", e.detail)
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.group = toJSON(rawData.response)
            }.bind(this));
        }
    })
</script>
</dom-module>
