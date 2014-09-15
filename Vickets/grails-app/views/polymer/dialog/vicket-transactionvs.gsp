<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">

<polymer-element name="vicket-transactionvs" attributes="url opened">
    <template>
        <votingsystem-dialog id="xDialog" class="vicketTransactionDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <!-- place all overlay styles inside the overlay target -->
        <style no-shim>
            .vicketTransactionDialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 1em;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 400px;
            }
        </style>
        <g:include view="/include/styles.gsp"/>
        <core-ajax id="ajax" auto url="{{url}}" response="{{transactionvs}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <div class="" style="margin:0px auto 0px auto;">
            <div layout horizontal>
                <h3 flex style="text-align: center;color:#6c0404;">{{transactionvs.description}}</h3>
            </div>
            <div layout vertical center center-justified>
                <div>
                    <template if="{{transactionvs.tags.length > 0}}">
                        <div layout horizontal center center-justified>
                            <template repeat="{{tag in transactionvs.tags}}">
                                <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag.name}}</a>
                            </template>
                        </div>
                    </template>
                    <div style=""><b><g:message code="subjectLbl"/>: </b>{{transactionvs.subject}}</div>
                    <div style=""><b><g:message code="amountLbl"/>: </b>{{transactionvs.amount}} {{transactionvs.currency}}</div>
                    <div style=""><b><g:message code="dateCreatedLbl"/>: </b>{{transactionvs.dateCreated}}</div>
                    <template if="{{transactionvs.validTo}}">
                        <div style=""><b><g:message code="validToLbl"/>: </b>{{transactionvs.validTo}}</div>
                    </template>

                    <template if="{{transactionvs.fromUserVS}}">
                        <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;">
                            <g:message code="pagerLbl"/></div>
                        <div id="fromUserDiv" style="margin-left: 20px;">
                            <template if="{{'VICKET_SOURCE_INPUT' != transactionvs.type}}">
                                <template if="{{'GROUP' == transactionvs.fromUserVS.type}}">
                                    <div style=""><b><g:message code="groupLbl"/>: </b>
                                        <a on-click="{{showFromUserInfo}}">{{transactionvs.fromUserVS.name}}</a>
                                    </div>
                                </template>
                                <template if="{{'GROUP' != transactionvs.fromUserVS.type}}">
                                    <div style=""><b><g:message code="nifLbl"/>: </b>{{transactionvs.fromUserVS.nif}}</div>
                                    <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs.fromUserVS.name}}</div>
                                </template>
                            </template>
                            <template if="{{'VICKET_SOURCE_INPUT' == transactionvs.type && transactionvs.fromUserVS}}">
                                <div style=""><b><g:message code="externalEntityLbl"/>: </b>{{transactionvs.fromUserVS.name}}</div>
                                <template if="{{transactionvs.fromUserVS.payer.fromUser}}">
                                    <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs.fromUserVS.payer.fromUser}}</div>
                                </template>

                                <div style=""><b><g:message code="IBANLbl"/>: </b>
                                    <a on-click="{{showInfoIBAN}}">{{transactionvs.fromUserVS.payer.fromUserIBAN}}</a></div>
                            </template>
                        </div>
                    </template>
                    <template if="{{transactionvs.fromUserVS == null}}">
                        <div style="font-weight: bold;"><g:message code="anonymousPagerLbl"/></div>
                    </template>

                    <template if="{{transactionvs.childTransactions}}">
                        <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;margin:10px 0px 0px 0px;">
                            <g:message code="receptorsLbl"/></div>
                        <div  layout flex horizontal wrap around-justified>
                            <template repeat="{{childTransactions in transactionvs.childTransactions}}">
                                <div class="btn btn-default" style="margin:5px 0px 0px 5px;">
                                    <div style=""><b><g:message code="nifLbl"/>: </b>{{childTransactions.toUserVS.nif}}</div>
                                    <div style=""><b><g:message code="nameLbl"/>: </b>{{childTransactions.toUserVS.name}}</div>
                                    <div style=""><b><g:message code="amountLbl"/>: </b>{{childTransactions.amount}} {{childTransactions.currency}}</div>
                                </div >
                            </template>
                        </div>
                    </template>

                    <template if="{{transactionvs.toUserVS}}">
                        <div style="margin:20px 0px 0px 20px;">
                            <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
                            <template if="{{'GROUP' == transactionvs.toUserVS.type}}">
                                <div style=""><b><g:message code="groupLbl"/>: </b>
                                    <a on-click="{{showGroupInfo}}">
                                        {{transactionvs.toUserVS.name}}
                                    </a>
                                </div>
                            </template>
                            <template if="{{'GROUP' != transactionvs.toUserVS.type}}">
                                <div style=""><b><g:message code="nifLbl"/>: </b>{{transactionvs.toUserVS.nif}}</div>
                                <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs.toUserVS.name}}</div>
                            </template>
                        </div >
                    </template>

                    <div layout horizontal style="margin:10px 0px 0px 0px; display:{{isClientToolConnected?'block':'none'}}">
                        <div flex></div>
                        <votingsystem-button on-click="{{openReceipt}}">
                            <g:message code="openReceiptLbl"/> <i class="fa fa-cogs"></i>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('vicket-transactionvs', {
            publish: {
                transactionvs: {value: {}}
            },
            ready: function() {
                console.log(this.tagName + " - " + this.id + " - ready")
                this.isClientToolConnected = window['isClientToolConnected']
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.async(function() { this.$.xDialog.opened = this.opened});
            },
            showToUserInfo:function(e) {
                var groupURL = "${createLink(uri:'/groupVS')}/" + e.target.templateInstance.model.transactionvs.toUserVS.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = "${createLink(uri:'/groupVS')}/" +  e.target.templateInstance.model.transactionvs.fromUserVS.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showInfoIBAN:function(e) {
                var fromUserIBANInfoURL = "${createLink(uri:'/IBAN')}/from/" + e.target.templateInstance.model.transactionvs.fromUserVS.payer.fromUserIBAN
                console.log(this.tagName + " - showInfoIBAN - fromUserIBANInfoURL: " + fromUserIBANInfoURL)
            },
            formatDate : function(dateValue) {

            },
            openReceipt: function () {
                console.log("openReceipt")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_RECEIPT)
                webAppMessage.message = this.transactionvs.receipt
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("openReceiptCallback - message: " + appMessage);
                    var appMessageJSON = JSON.parse(appMessage)
                    }}
                webAppMessage.callerCallback = objectId
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            saveReceipt:function () {
                console.log("saveReceipt")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SAVE_RECEIPT)
                webAppMessage.message = this.transactionvs.receipt
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);
                    var appMessageJSON = JSON.parse(appMessage)
                    }}
                webAppMessage.callerCallback = objectId
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>