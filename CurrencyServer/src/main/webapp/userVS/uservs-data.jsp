<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../transactionVS/transactionvs-form.vsp" rel="import"/>
<link href="../element/messagevs-send-dialog.vsp" rel="import"/>

<dom-module name="uservs-data">
<template>
    <style>
        .uservsCancelled {
            background: #ff0000; opacity:0.5; left:20%; top:-60px; font-size: 1.8em; font-weight: bold;
            color:#f9f9f9; text-align: center; text-transform:uppercase; transform:rotate(20deg);
            -ms-transform:rotate(20deg); -webkit-transform:rotate(20deg); -moz-transform: rotate(20deg);
        }
    </style>
    <div>
        <div hidden="{{isBankVS}}" class="layout horizontal center center-justified" style="margin: 0 0 15px 0;">
            <div class="flex" style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                <div data-user-id$="{{uservs.id}}" style="text-align: center;"><span>{{uservs.firstName}}</span> <span>{{uservs.lastName}}</span></div>
            </div>
            <div style="margin: 0 0 0 0; font-size: 0.8em;vertical-align: bottom;">
                <b>IBAN: </b><span>{{uservs.iban}}</span>
            </div>
        </div>
        <div hidden="{{!isBankVS}}" class="layout horizontal center center-justified" style="margin: 0 0 15px 0;">
            <div class="flex" style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                <div data-user-id$="{{uservs.id}}" style="text-align: center;">{{uservs.name}}</div>
            </div>
            <div style="margin: 0 0 0 0; font-size: 0.8em;vertical-align: bottom;">
                <b>IBAN: </b><span>{{uservs.iban}}</span>
            </div>
        </div>
        <div hidden="{{isActive}}" class="uservsCancelled">{{uservs.state}}</div>
        <div hidden="{{!isActive}}" class="layout horizontal center center-justified" style="margin:0px 0px 10px 0px;">
            <div hidden="{{!isClientToolConnected}}" class="horizontal layout">
                <div hidden="{{!isConnected}}" style="margin:0 20px 0 0;">
                    <button on-click="alertDialog">
                        <i class="fa fa-envelope-o"></i> ${msg.sendMessageVSLbl}
                    </button>
                </div>
                <div hidden="{{isBankVS}}" style="margin:0 20px 0 0;">
                    <button on-click="makeTransactionVS">
                        <i class="fa fa-money"></i> ${msg.sendTransactionVSLbl}
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
                        <i class="fa fa fa-thumbs-o-down"></i> ${msg.blockUserVSLbl}
                    </button>
                </div>
            </div>
        </div>
        <div hidden="{{!uservs.description}}" style="margin:0 0 20px 0;">
            <div id="userDescriptionDiv" class="contentDiv" style=" border: 1px solid #c0c0c0;padding:10px;"></div>
        </div>
        <div hidden="{{subscriptionsHidden}}"
             layout flex horizontal wrap style="border:1px solid #eee; padding: 5px;">
            <div style="font-size: 0.9em;font-weight: bold; color: #888; margin:0 15px 0 0;">
                - ${msg.groupsLbl} -
            </div>
            <template is="dom-repeat" items="{{uservs.subscriptionVSList}}" as="subscriptionVS">
                <a href="{{getUserVSURL(subscriptionVS.groupVS.id)}}"
                   style="margin: 0 10px 10px 0;">{{subscriptionVS.groupVS.name}}</a>
            </template>
        </div>
    </div>
    <div hidden="{{transactionFormHidden}}">
        <transactionvs-form id="transactionvsForm" fab-visible="true"></transactionvs-form>
    </div>
    <messagevs-send-dialog id="sendMessageDialog" on-message-response="{{sendMessageDialogResponse}}"></messagevs-send-dialog>
</template>
<script>
    Polymer({
        is:'uservs-data',
        properties: {
            uservs: {type:Object, observer:'uservsChanged'},
            isClientToolConnected: {type:Boolean},
            isActive: {type:Boolean, value:false},
            isAdmin: {type:Boolean, value:false},
            isConnected: {type:Boolean, value:false},
            isBankVS: {type:Boolean, value:false},
            transactionFormHidden: {type:Boolean, value:true},
            subscriptionsHidden: {type:Boolean, value:false},
            uservsType: {type:String},
            url:{type:String, observer:'getHTTP'},
            message: {type:String}
        },
        ready: function() {
            this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            console.log(this.tagName + " - ready - menuType: " + this.menuType)
            this.$.transactionvsForm.addEventListener('closed', function (e) {
                this.page = 0;
            }.bind(this))
            if(this.message) alert(this.message, "${msg.messageLbl}")
        },
        uservsChanged:function() {
            console.log(this.tagName + " - uservsDtoChanged - uservs: " + JSON.stringify( this.uservs))
            if(this.uservs.name) {
                var uservsType
                if('BANKVS' == this.uservs.type) uservsType = "${msg.bankVSLbl}"
                if('USER' == this.uservs.type) uservsType = "${msg.userLbl}"
                if('SYSTEM' == this.uservs.type) uservsType = "${msg.systemLbl}"
            }
            this.subscriptionsHidden = (!this.uservs.subscriptionVSList ||  this.uservs.subscriptionVSList.length === 0)
            this.isActive = (this.uservs.state === 'ACTIVE')
            this.isConnected = (this.uservs.connectedDevices && this.uservs.connectedDevices.length > 0)
            this.isBankVS = ('BANKVS' !== this.uservs.type)
            this.isAdmin = ('superuser' === menuType || 'admin' === menuType)
            d3.select(this).select("#userDescriptionDiv").html(this.uservs.description)
         },
        getUserVSURL:function(id) {
            return contextURL + "/rest/userVS/" + id
        },
        goToWeekBalance:function() {
            page.show(contextURL + "/rest/balance/userVS/id/" + this.uservs.id)
        },
        blockUser:function() {
            console.log(this.tagName + " - blockUser")
        },
        makeTransactionVS:function() {
            console.log(this.tagName + " - makeTransactionVS")
            this.$.transactionvsForm.init(Operation.FROM_USERVS, this.uservs.name, this.uservs.iban , this.uservs.id)
            this.page = 1;
        },
        showByIBAN:function(IBAN) {
            this.url =  contextURL + "/rest/userVS/IBAN/" + IBAN
        },
        alertDialog: function () {
            this.$.sendMessageDialog.show(this.uservs)
        },
        sendMessageDialogResponse:function(e) {
            var appMessageJSON = JSON.parse(e.detail)
            var caption = '${msg.sendMessageERRORCaption}'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '${msg.sendMessageOKCaption}'
            }
            alert(msg, caption)
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.uservs = toJSON(rawData.response)
            }.bind(this));
        }
    });
</script>
</dom-module>
