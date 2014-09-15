<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-transactionvs-table']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/send-message-dialog']"/>">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">

<polymer-element name="uservs-data" attributes="opened url">
<template>
    <votingsystem-dialog id="xDialog" class="uservsDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
        <!-- place all overlay styles inside the overlay target -->
        <style no-shim>
            .uservsDialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 10px 30px 10px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 800px;
            }
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
        <core-ajax id="ajax" auto url="{{url}}" response="{{uservs}}" handleAs="json" method="get"
                   contentType="json" on-core-response="{{ajaxResponse}}"></core-ajax>
        <div style="margin:0px 30px 0px 30px;">
            <div id="messagePanel" class="messagePanel messageContent text-center" style="display: none;">
            </div>
            <div layout horizontal center center-justified>
                <div userId-data="{{uservs.id}}" flex style="text-align: center;font-size: 1.5em; margin:0px 0px 0px 30px;
                    font-weight: bold; color:#6c0404;">{{uservsType}} - {{uservs.name}}
                </div>
                <div>
                    <core-icon-button on-click="{{close}}" icon="close" style="color:#f9f9f9;"></core-icon-button>
                </div>
            </div>
            <template if="{{uservs.state != 'ACTIVE'}}">
                <div class="uservsCancelled">{{uservs.state}}</div>
            </template>


                <div layout horizontal center center-justified style="margin:0px 0px 10px 0px;">
                    <votingsystem-button id="sendMessageVSButton" type="submit" on-click="{{showMessageVSDialog}}"
                            style="margin:10px 20px 0px 0px;">
                        <g:message code="sendMessageVSLbl"/> <i class="fa fa fa-envelope-square"></i>
                    </votingsystem-button>
                    <votingsystem-button id="makeDepositButton" type="submit" on-click="{{makeDeposit}}"
                            style="margin:10px 20px 0px 0px;">
                        <g:message code="makeDepositLbl"/> <i class="fa fa fa-money"></i>
                    </votingsystem-button>
                    <template if="{{menuType == 'superadmin'}}">
                        <votingsystem-button id="blockUserVSButton" type="submit"
                                             style="margin:10px 20px 0px 0px;" on-click="{{blockUser}}">
                            <g:message code="blockUserVSLbl"/> <i class="fa fa fa-thumbs-o-down"></i>
                        </votingsystem-button>
                    </template>
                </div>

            <template if="{{uservs.description}}">
                <div style="margin: 20px 0 0px 0;">
                    <div id="userDescriptionDiv" class="eventContentDiv" style=" border: 1px solid #c0c0c0;padding:10px;">
                        <votingsystem-html-echo html="{{uservs.description}}"></votingsystem-html-echo>
                    </div>
                </div>
            </template>
            <div layout horizontal style="color: #888;">
                <template if="{{uservs.firstName}}">
                    <div flex>{{uservs.firstName}} {{uservs.lastName}}</div>
                </template>
                <div  style="margin: 0px 0 15px 0; font-size: 0.9em;"><b>IBAN: </b>{{uservs.IBAN}}</div>
            </div>


            <template if="{{isActive && menuType == 'user'}}"> </template>

            <%  def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
                def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
                def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
            %>

            <div  style="text-align:center; font-size: 1.2em;font-weight: bold; color: #888;">
                <g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom, weekTo]}"/>
            </div>

            <vicket-transactionvs-table id="transactionvsTable" transactionList="{{uservs.transactionList}}"></vicket-transactionvs-table>

        </div>
        <send-message-dialog id="sendMessageDialog" on-message-response="{{sendMessageDialogResponse}}"></send-message-dialog>
    </votingsystem-dialog>
</template>

<script>

    Polymer('uservs-data', {
        isUserVS:false,
        isVicketSource:false,
        isGroupVS:false,
        isActive:false,
        menuType:null,
        opened:false,
        uservsType:'',
        isClientToolConnected:false,
        sendMessageTemplateMsg:"<g:message code="uservsMessageVSLbl"/>",
        publish: {
            uservs: {value: {}}
        },
        ready: function() {
            this.menuType = menuType
            this.isClientToolConnected = window['isClientToolConnected']
            console.log(this.tagName + " - ready - menuType: " + this.menuType + " - isClientToolConnected: " + isClientToolConnected)
        },
        close:function() {
            this.opened = false
        },
        uservsChanged:function() {
            console.log("uservsChanged - uservs: " + this.uservs)
            if('VICKET_SOURCE' == this.uservs.type) this.uservsType = "<g:message code="vicketSourceLbl"/>"
            if('USER' == this.uservs.type) this.uservsType = "<g:message code="userLbl"/>"

        },
        onCoreOverlayOpen:function(e) {
            this.opened = this.$.xDialog.opened
        },
        openedChanged:function() {
            this.async(function() { this.$.xDialog.opened = this.opened});
        },
        urlChanged:function() {
            if(this.url != null && '' != this.url.trim()) this.opened = true
        },
        show:function(baseURL, userId) {

        },
        blockUser:function() {
            console.log(this.tagName + " - blockUser")
        },
        makeDeposit:function() {
            console.log(this.tagName + " - makeDeposit")
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
        }
    });

</script>
</polymer-element>
