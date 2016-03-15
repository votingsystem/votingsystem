<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../transaction/transaction-form.vsp" rel="import"/>
<link href="../element/messagevs-send-dialog.vsp" rel="import"/>

<dom-module name="user-data">
<template>
    <style>
        .userCancelled {
            background: #ff0000; opacity:0.5; left:20%; top:-60px; font-size: 1.8em; font-weight: bold;
            color:#f9f9f9; text-align: center; text-transform:uppercase; transform:rotate(20deg);
            -ms-transform:rotate(20deg); -webkit-transform:rotate(20deg); -moz-transform: rotate(20deg);
        }
    </style>
    <div>
        <div style="max-width: 1200px; margin: 0 auto">
            <div hidden="{{isBank}}" class="layout horizontal center center-justified" style="margin: 0 0 15px 0;">
                <div class="flex" style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                    <div data-user-id$="{{user.id}}" style="text-align: center;"><span>{{user.firstName}}</span> <span>{{user.lastName}}</span></div>
                </div>
                <div style="margin: 0 0 0 0; font-size: 0.8em;vertical-align: bottom;">
                    <b>IBAN: </b><span>{{user.iban}}</span>
                </div>
            </div>
            <div hidden="{{!isBank}}" class="layout horizontal center center-justified" style="margin: 0 0 15px 0;">
                <div class="flex" style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                    <div data-user-id$="{{user.id}}" style="text-align: center;">{{user.name}}</div>
                </div>
                <div style="margin: 0 0 0 0; font-size: 0.8em;vertical-align: bottom;">
                    <b>IBAN: </b><span>{{user.iban}}</span>
                </div>
            </div>
            <div hidden="{{isActive}}" class="userCancelled">{{user.state}}</div>
            <div hidden="{{!isActive}}" class="layout horizontal center center-justified" style="margin:0px 0px 10px 0px;">
                <div hidden="{{!isClientToolConnected}}" class="horizontal layout">
                    <div hidden="{{!isConnected}}" style="margin:0 20px 0 0;">
                        <button on-click="showSendMessageDialog">
                            <i class="fa fa-envelope-o"></i> ${msg.sendMessageVSLbl}
                        </button>
                    </div>
                    <div hidden="{{isBank}}" style="margin:0 20px 0 0;">
                        <button on-click="makeTransaction">
                            <i class="fa fa-money"></i> ${msg.sendTransactionLbl}
                        </button>
                    </div>
                </div>
                <div style="margin:0 20px 0 0;">
                    <button on-click="goToWeekBalance">
                        <i class="fa fa-bar-chart"></i> ${msg.goToWeekBalanceLbl}
                    </button>
                </div>
                <div hidden="{{!isConnected}}">
                    <div hidden="{{!isAdmin}}" style="margin:0 20px 0 0;">
                        <button on-click="blockUser">
                            <i class="fa fa fa-thumbs-o-down"></i> ${msg.blockUserLbl}
                        </button>
                    </div>
                </div>
            </div>
            <div hidden="{{!user.description}}" style="margin:0 0 20px 0;">
                <div id="userDescriptionDiv" class="contentDiv" style=" border: 1px solid #c0c0c0;padding:10px;"></div>
            </div>
            <div hidden="{{subscriptionsHidden}}"
                 layout flex horizontal wrap style="border:1px solid #eee; padding: 5px;">
                <div style="font-size: 0.9em;font-weight: bold; color: #888; margin:0 15px 0 0;">
                    - ${msg.groupsLbl} -
                </div>
                <template is="dom-repeat" items="{{user.subscriptionList}}" as="subscription">
                    <a href="{{getUserURL(subscription.group.id)}}"
                       style="margin: 0 10px 10px 0;">{{subscription.group.name}}</a>
                </template>
            </div>
        </div>
    </div>
    <div hidden="{{transactionFormHidden}}">
        <transaction-form id="transactionForm" fab-visible="true"></transaction-form>
    </div>
    <messagevs-send-dialog id="sendMessageDialog"></messagevs-send-dialog>
</template>
<script>
    Polymer({
        is:'user-data',
        properties: {
            user: {type:Object, observer:'userChanged'},
            isClientToolConnected: {type:Boolean},
            isActive: {type:Boolean, value:false},
            isAdmin: {type:Boolean, value:false},
            isConnected: {type:Boolean, value:false},
            isBank: {type:Boolean, value:false},
            transactionFormHidden: {type:Boolean, value:true},
            subscriptionsHidden: {type:Boolean, value:false},
            userType: {type:String},
            url:{type:String, observer:'getHTTP'},
            message: {type:String}
        },
        ready: function() {
            this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            console.log(this.tagName + " - ready - menuType: " + this.menuType)
            this.$.transactionForm.addEventListener('closed', function (e) {
                this.page = 0;
            }.bind(this))
            if(this.message) alert(this.message, "${msg.messageLbl}")
        },
        userChanged:function() {
            console.log(this.tagName + " - userDtoChanged - user: " + JSON.stringify( this.user))
            if(this.user.name) {
                var userType
                if('BANK' == this.user.type) userType = "${msg.bankLbl}"
                if('USER' == this.user.type) userType = "${msg.userLbl}"
                if('SYSTEM' == this.user.type) userType = "${msg.systemLbl}"
            }
            this.subscriptionsHidden = (!this.user.subscriptionList ||  this.user.subscriptionList.length === 0)
            this.isActive = (this.user.state === 'ACTIVE')
            this.isConnected = (this.user.connectedDevices && this.user.connectedDevices.length > 0)
            this.isBank = ('BANK' !== this.user.type)
            this.isAdmin = ('superuser' === menuType || 'admin' === menuType)
            d3.select(this).select("#userDescriptionDiv").html(this.user.description)
         },
        getUserURL:function(id) {
            return vs.contextURL + "/rest/user/" + id
        },
        goToWeekBalance:function() {
            page.show(vs.contextURL + "/rest/balance/user/id/" + this.user.id)
        },
        blockUser:function() {
            console.log(this.tagName + " - blockUser")
        },
        makeTransaction:function() {
            console.log(this.tagName + " - makeTransaction")
            this.$.transactionForm.init(Operation.FROM_USER, this.user.name, this.user.iban , this.user.id)
            this.page = 1;
        },
        showByIBAN:function(IBAN) {
            this.url =  vs.contextURL + "/rest/user/IBAN/" + IBAN
        },
        showSendMessageDialog: function () {
            this.$.sendMessageDialog.show(this.user)
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.user = toJSON(rawData.response)
            }.bind(this));
        }
    });
</script>
</dom-module>
