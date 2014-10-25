<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-table']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/messageVS/messagevs-send-dialog']"/>">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-form']"/>">

<polymer-element name="uservs-data" attributes="">
<template>
    <g:include view="/include/styles.gsp"/>
    <style no-shim>
        .uservsCancelled {
            background: #ff0000; opacity:0.5; left:20%; top:-60px; font-size: 1.8em; font-weight: bold;
            color:#f9f9f9; text-align: center; text-transform:uppercase; transform:rotate(20deg);
            -ms-transform:rotate(20deg); -webkit-transform:rotate(20deg); -moz-transform: rotate(20deg);
        }
    </style>
    <core-ajax id="ajax" on-core-response="{{ajaxResponse}}" handleAs="json"
               method="get" contentType="json"></core-ajax>
    <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
             transitions="cross-fade-all">
        <section id="page1">
            <template if="{{uservs.name}}" on-template-bound="{{templateBound}}">
                <div>
                    {{uservs | templateBound}}
                    <div layout horizontal center center-justified style="margin: 0 0 15px 0;">
                        <div flex>
                            <template if="{{uservs.firstName}}">
                                <div  style="margin: 0 0 0 0; font-size: 0.8em;" flex>{{uservs.firstName}} {{uservs.lastName}}</div>
                            </template>
                        </div>
                        <div flex style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                            <div userId-data="{{uservs.id}}" style="text-align: center;">{{uservs.name}}</div>
                        </div>
                        <div flex style="margin: 0 0 0 0; font-size: 0.8em;vertical-align: bottom;">
                            <b>IBAN: </b>{{uservs.IBAN}}
                        </div>
                    </div>

                    <template if="{{uservs.state != 'ACTIVE'}}">
                        <div class="uservsCancelled">{{uservs.state}}</div>
                    </template>

                    <template if="{{uservs.state == 'ACTIVE'}}">
                        <div layout horizontal center center-justified style="margin:0px 0px 10px 0px;">
                            <div class="linkVS" on-click="{{showMessageVSDialog}}">
                                <i class="fa fa-envelope-square"></i> <g:message code="sendMessageVSLbl"/>
                            </div>
                            <div style="display: {{'BANKVS' == uservs.type ? 'none':'block'}}" class="linkVS"
                                 on-click="{{makeTransactionVS}}">
                                <i class="fa fa-money"></i> <g:message code="makeTransactionVSLbl"/>
                            </div>
                            <div class="linkVS" on-click="{{goToWeekBalance}}">
                                <i class="fa fa-bar-chart"></i> <g:message code="goToWeekBalanceLbl"/>
                            </div>
                            <div style="display: {{'superuser' == menuType ? 'block':'none'}}" class="linkVS"
                                 on-click="{{blockUser}}">
                                <g:message code="blockUserVSLbl"/> <i class="fa fa fa-thumbs-o-down"></i>
                            </div>
                        </div>
                    </template>

                    <template if="{{uservs.description}}">
                        <div style="margin:0 0 20px 0;">
                            <div id="userDescriptionDiv" class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
                                <vs-html-echo html="{{uservs.description}}"></vs-html-echo>
                            </div>
                        </div>
                    </template>

                    <template if="{{uservs.subscriptionVSList && uservs.subscriptionVSList.length > 0}}">
                        <div layout flex horizontal wrap style="border:1px solid #eee; padding: 5px;">
                            <div style="font-size: 0.9em;font-weight: bold; color: #888; margin:0 15px 0 0;">
                                - <g:message code="groupsLbl"/> -
                            </div>
                            <template repeat="{{subscriptionVS in uservs.subscriptionVSList}}">
                                <a href="${createLink(controller: 'userVS')}/{{subscriptionVS.groupVS.id}}" style="margin: 0 10px 10px 0;">{{subscriptionVS.groupVS.name}}</a>
                            </template>
                        </div>
                    </template>

                </div>
            </template>
        </section>
        <section id="page2">
            <div class="pageContentDiv" cross-fade>
                <transactionvs-form id="transactionvsForm" subpage></transactionvs-form>
            </div>
        </section>
    </core-animated-pages>
    <messagevs-send-dialog id="sendMessageDialog" on-message-response="{{sendMessageDialogResponse}}"></messagevs-send-dialog>
</template>
<script>
    Polymer('uservs-data', {
        isActive:false,
        menuType:null,
        uservsType:'',
        isClientToolConnected:false,
        sendMessageTemplateMsg:"<g:message code="uservsMessageVSLbl"/>",
        publish: {
            userVSData: {}
        },
        ready: function() {
            this.menuType = menuType
            this.isClientToolConnected = window['isClientToolConnected']
            console.log(this.tagName + " - ready - menuType: " + this.menuType + " - isClientToolConnected: " + isClientToolConnected)
            this.$.transactionvsForm.addEventListener('operation-finished', function (e) {
                this.page = 0;
            }.bind(this))
        },
        templateBound:function() {
            console.log(this.tagName + " - templateBound")
            var uservsType
            if('BANKVS' == this.uservs.type) uservsType = "<g:message code="bankVSLbl"/>"
            if('USER' == this.uservs.type) uservsType = "<g:message code="userLbl"/>"
            if('SYSTEM' == this.uservs.type) uservsType = "<g:message code="systemLbl"/>"
            this.fire('core-signal', {name: "votingsystem-innerpage", data: {title:uservsType}});
        },
        userVSDataChanged:function() {
            //console.log(this.tagName + " - userVSDataChanged - userVSData: " + JSON.stringify(this.userVSData))
            this.uservs = this.userVSData.userVS
        },
        goToWeekBalance:function() {
            this.asyncFire('core-signal', {name: "votingsystem-innerpage", data: {
                url:"${createLink( controller:'balance', action:"userVS", absolute:true)}/" + this.uservs.id}});
            console.log("======== asyncFire")
            //loadURL_VS("${createLink( controller:'balance', action:"userVS", absolute:true)}/" + this.uservs.id)
        },
        ajaxResponse:function() {
            console.log(this.tagName + " - ajaxResponse - userVSData: " + JSON.stringify(this.userVSData))
            this.uservs = this.userVSData.userVS
        },
        blockUser:function() {
            console.log(this.tagName + " - blockUser")
        },
        makeTransactionVS:function() {
            console.log(this.tagName + " - makeTransactionVS")
            this.$.transactionvsForm.init(Operation.FROM_USERVS_TO_USERVS, this.uservs.name, this.uservs.IBAN , this.uservs.id)
            this.page = 1;
        },
        showByIBAN:function(IBAN) {
            var serviceURL =  "${createLink( controller:'userVS')}/IBAN/" + IBAN
            if(this.$.ajax.url != serviceURL) {
                console.log(this.tagName + " - showByIBAN - url: " + serviceURL)
                this.$.ajax.url = serviceURL
                this.$.ajax.go()
            }
        },
        showMessageVSDialog: function () {
            this.$.sendMessageDialog.show(this.uservs.nif, this.sendMessageTemplateMsg.format(this.uservs.name),
                    this.uservs.certificateList)
        },
        sendMessageDialogResponse:function(e) {
            var appMessageJSON = JSON.parse(e.detail)
            var caption = '<g:message code="sendMessageERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="sendMessageOKCaption"/>'
            }
            showMessageVS(msg, caption)
        },
        domReady: function() {
            updateLinksVS(this.shadowRoot.querySelectorAll("a"))
        }
    });

</script>
</polymer-element>
