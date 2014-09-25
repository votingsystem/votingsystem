<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-table']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/messageVS/messagevs-send-dialog']"/>">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="uservs-data" attributes="">
<template>
    <g:include view="/include/styles.gsp"/>
    <style no-shim>
        .uservsCancelled {
            background: #ff0000;
            opacity:0.5;
            left:20%;
            top:-60px;
            font-size: 1.8em;
            font-weight: bold;
            color:#f9f9f9;
            text-align: center;
            text-transform:uppercase;
            transform:rotate(20deg);
            -ms-transform:rotate(20deg);
            -webkit-transform:rotate(20deg);
            -moz-transform: rotate(20deg);
        }
    </style>
    <core-ajax id="ajax" auto on-core-response="{{ajaxResponse}}" response="{{uservs}}" handleAs="json"
               method="get" contentType="json"></core-ajax>

    <template if="{{uservs.name}}">
    <div style="margin:0px 30px 0px 30px;">
        {{uservs | setType}}
        <div layout horizontal center center-justified>
            <div flex style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                <div userId-data="{{uservs.id}}" style="text-align: center;">{{uservsType}} - {{uservs.name}}</div>
            </div>
        </div>
        <div layout horizontal style="color: #888; margin: 0 0 0 0;">
            <template if="{{uservs.firstName}}">
                <div  style="margin: 0 0 0 0; font-size: 0.8em;" flex>{{uservs.firstName}} {{uservs.lastName}}</div>
            </template>
            <div style="margin: 0 0 0 0; font-size: 0.8em;"><b>IBAN: </b>{{uservs.IBAN}}</div>
        </div>

        <template if="{{uservs.state != 'ACTIVE'}}">
            <div class="uservsCancelled">{{uservs.state}}</div>
        </template>

        <div style="display:{{isClientToolConnected?'block':'none'}}">
            <div layout horizontal center center-justified style="margin:0px 0px 10px 0px;">
                <div>
                    <votingsystem-button id="sendMessageVSButton" type="submit" on-click="{{showMessageVSDialog}}"
                                         style="margin:10px 20px 0px 0px;">
                        <i class="fa fa-envelope-square" style="margin:0 7px 0 3px;"></i> <g:message code="sendMessageVSLbl"/>
                    </votingsystem-button>
                </div>

                <div style="display: {{'BANKVS' == uservs.type ? 'none':'block'}}">
                    <votingsystem-button id="makeDepositButton" type="submit" on-click="{{makeDeposit}}"
                                         style="margin:10px 20px 0px 0px;">
                        <i class="fa fa-money" style="margin:0 7px 0 3px;"></i> <g:message code="makeDepositLbl"/>
                    </votingsystem-button>
                </div>
                <div style="display: {{'superadmin' == menuType ? 'block':'none'}}">
                    <votingsystem-button id="blockUserVSButton" type="submit"
                                         style="margin:10px 20px 0px 0px;" on-click="{{blockUser}}">
                        <g:message code="blockUserVSLbl"/> <i class="fa fa fa-thumbs-o-down"></i>
                    </votingsystem-button>
                </div>
            </div>
        </div>

        <template if="{{uservs.description}}">
            <div style="margin:0 0 20px 0;">
                <div id="userDescriptionDiv" class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
                    <votingsystem-html-echo html="{{uservs.description}}"></votingsystem-html-echo>
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

        <%  def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
            def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewShortDateFormat')
            def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
        %>

        <template if="{{uservs.transactionVSMap && uservs.transactionVSMap.queryRecordCount > 0}}">
            <div  style="text-align:center; font-size: 1.3em;font-weight: bold; color: #888; margin:25px 0 0 0;">
                <g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom]}"/>
            </div>
            <transactionvs-table id="transactionvsTable" userNif="{{uservs.nif}}"
                        transactionList="{{uservs.transactionVSMap.transactionVSList}}"></transactionvs-table>
        </template>
        <template if="{{!uservs.transactionVSMap || uservs.transactionVSMap.queryRecordCount == 0}}">
            <div  style="text-align:center; font-size: 1.3em;font-weight: bold; color: #888; margin:15px 0 0 0;">
                <g:message code="transactionsEmptyCurrentWeekPeriodMsg" args="${[weekFrom]}"/>
            </div>
        </template>

    </div>
    </template>
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
            uservs: {}
        },
        ready: function() {
            this.menuType = menuType
            //this.isClientToolConnected = window['isClientToolConnected']
            this.isClientToolConnected = true
            console.log(this.tagName + " - ready - menuType: " + this.menuType + " - isClientToolConnected: " + isClientToolConnected)
        },
        setType:function() {
            console.log(this.tagName + " - setType")
            if('BANKVS' == this.uservs.type) this.uservsType = "<g:message code="bankVSLbl"/>"
            if('USER' == this.uservs.type) this.uservsType = "<g:message code="userLbl"/>"
        },
        blockUser:function() {
            console.log(this.tagName + " - blockUser")
        },
        makeDeposit:function() {
            console.log(this.tagName + " - makeDeposit")
        },
        showByIBAN:function(IBAN) {
            var serviceURL =  "${createLink( controller:'userVS')}/IBAN/" + IBAN
            if(this.$.ajax.url != serviceURL) {
                this.$.ajax.url = serviceURL
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
