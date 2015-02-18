<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="core-ajax" file="core-ajax.html"/>
<vs:webresource dir="core-animated-pages" file="core-animated-pages.html"/>
<vs:webcomponent path="/transactionVS/transactionvs-form"/>
<vs:webcomponent path="/element/messagevs-send-dialog"/>
<vs:webcomponent path="/transactionVS/transactionvs-table"/>

<polymer-element name="uservs-data" attributes="messageToUser">
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
            <div hidden?="{{!uservs.name}}">
                <div layout horizontal center center-justified style="margin: 0 0 15px 0;">
                    <div hidden?="{{!uservs.firstName}}" flex>
                        <div  style="margin: 0 0 0 0; font-size: 0.8em;" flex>{{uservs.firstName}} {{uservs.lastName}}</div>
                    </div>
                    <div flex style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                        <div userId-data="{{uservs.id}}" style="text-align: center;">{{uservs.name}}</div>
                    </div>
                    <div flex style="margin: 0 0 0 0; font-size: 0.8em;vertical-align: bottom;">
                        <b>IBAN: </b>{{uservs.IBAN}}
                    </div>
                </div>
                <div hidden?="{{uservs.state === 'ACTIVE'}}" class="uservsCancelled">{{uservs.state}}</div>
                <div hidden?="{{uservs.state !== 'ACTIVE'}}" layout horizontal center center-justified style="margin:0px 0px 10px 0px;">
                    <div hidden?="{{!isClientToolConnected || uservs.connectedDevices.length === 0}}">
                        <paper-button raised on-click="{{showMessageVSDialog}}">
                            <i class="fa fa-envelope-o"></i> <g:message code="sendMessageVSLbl"/>
                        </paper-button>
                    </div>
                    <div hidden?="{{'BANKVS' == uservs.type || !isClientToolConnected}}">
                        <paper-button raised on-click="{{makeTransactionVS}}">
                            <i class="fa fa-money"></i> <g:message code="makeTransactionVSLbl"/>
                        </paper-button>
                    </div>
                    <div>
                        <paper-button raised on-click="{{goToWeekBalance}}">
                            <i class="fa fa-bar-chart"></i> <g:message code="goToWeekBalanceLbl"/>
                        </paper-button>
                    </div>
                    <div hidden?="{{'superuser' !== menuType}}">
                        <paper-button raised on-click="{{blockUser}}">
                            <g:message code="blockUserVSLbl"/> <i class="fa fa fa-thumbs-o-down"></i>
                        </paper-button>
                    </div>
                </div>
                <div hidden?="{{!uservs.description}}" style="margin:0 0 20px 0;">
                    <div id="userDescriptionDiv" class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
                        <vs-html-echo html="{{uservs.description}}"></vs-html-echo>
                    </div>
                </div>
                <div hidden="{{!uservs.subscriptionVSList ||  uservs.subscriptionVSList.length === 0}}"
                     layout flex horizontal wrap style="border:1px solid #eee; padding: 5px;">
                    <div style="font-size: 0.9em;font-weight: bold; color: #888; margin:0 15px 0 0;">
                        - <g:message code="groupsLbl"/> -
                    </div>
                    <template repeat="{{subscriptionVS in uservs.subscriptionVSList}}">
                        <a href="${createLink(controller: 'userVS')}/{{subscriptionVS.groupVS.id}}"
                           style="margin: 0 10px 10px 0;">{{subscriptionVS.groupVS.name}}</a>
                    </template>
                </div>
            </div>
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
            if(this.messageToUser) showMessageVS(this.messageToUser, "<g:message code="messageLbl"/>")
        },
        userVSDataChanged:function() {
            console.log(this.tagName + " - userVSDataChanged - userVSData: " + Object.prototype.toString.call(this.userVSData))
            this.userVSData = toJSON(this.userVSData)
            this.uservs = this.userVSData.userVS
            if(this.uservs.name) {
                var uservsType
                if('BANKVS' == this.uservs.type) uservsType = "<g:message code="bankVSLbl"/>"
                if('USER' == this.uservs.type) uservsType = "<g:message code="userLbl"/>"
                if('SYSTEM' == this.uservs.type) uservsType = "<g:message code="systemLbl"/>"
                this.fire('core-signal', {name: "vs-innerpage", data: {caption:uservsType}});
            }
        },
        goToWeekBalance:function() {
            loadURL_VS("${createLink( controller:'balance', action:"userVS", absolute:true)}/" + this.uservs.id)
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
            this.$.transactionvsForm.init(Operation.FROM_USERVS, this.uservs.name, this.uservs.IBAN , this.uservs.id)
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
            this.$.sendMessageDialog.show(this.uservs)
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
