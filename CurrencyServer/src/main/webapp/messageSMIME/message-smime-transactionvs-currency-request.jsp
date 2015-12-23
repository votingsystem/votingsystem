<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-smime-transactionvs-currency-request">
    <template>
        <style>
        .actorLbl {font-size: 1.2em; text-decoration: underline;font-weight: bold; color: #621; }
        .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic; }
        .iban-link {text-decoration: underline; color: #0000ee; cursor: pointer;}
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div class="layout vertical" style="margin: 0px auto; color: #667;">
                <div class="horizontal layout center center-justified" style="margin: 0 0 10px 0;">
                    <div class="pageHeader" style="margin:0 0 0 20px; font-size: 1.2em;">
                        ${msg.currencyRequestLbl}
                    </div>
                </div>

                <div hidden="{{!timeStampDate}}" class="timeStampMsg" >
                    <b>${msg.dateLbl}: </b> <span>{{timeStampDate}}</span>
                </div>
                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div class="horizontal layout center-justified">
                    <div style="font-size: 1.1em;"><b>${msg.amountLbl}: </b>
                        <span>{{smimeMessageContent.totalAmount}}</span> <span>{{smimeMessageContent.currencyCode}}</span></div>
                    <div style="margin:0 0 0 10px;">
                         ${msg.timeLimitedDateMsg} '<span>{{smimeMessageContent.validTo}}</span>'
                    </div>
                </div>
                <div style="margin: 20px 0 0 20px;">
                    <div class="actorLbl">${msg.senderLbl}</div>
                    <div>
                        <div><b>${msg.nameLbl}:</b> <span>{{smimeMessageContent.fromUserVS.name}}</span></div>
                    </div>
                </div>
                <div class="horizontal layout" style="margin:10px 0px 10px 0px; ">
                    <div class="flex">
                        <a class="btn btn-default" style="font-size: 0.7em;padding:3px;">
                            <i class="fa fa-tag" style="color: #888;"></i> <span>{{smimeMessageContent.tagVS.name}}</span></a>
                    </div>
                    <div hidden="{{!isClientToolConnected}}">
                        <button raised on-click="checkReceipt" style="font-size: 1.1em;">
                            <i class="fa fa-certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'message-smime-transactionvs-currency-request',
            properties: {
                smimeMessageContent: {type:Object, observer:'smimeMessageContentChanged'},
                isClientToolConnected: {type:Boolean, value: false},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean, value: true},
                smimeMessage: {type:String},
                timeStampDate: {type:String},
                caption: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
                sendSignalVS({caption:"${msg.transactionVSCurrencyRequest}"})
            },
            smimeMessageContentChanged:function() {
                console.log(this.tagName + " - smimeMessageContentChanged: " + JSON.stringify(this.smimeMessageContent))
            },
            showFromUserVSByIBAN:function(e) {
                page.show(contextURL + "/rest/userVS/IBAN/" + this.smimeMessageContent.bankIBAN)
            },
            showToUserVSByIBAN:function(e) {
                console.log(this.tagName + " - showUserVSByIBAN - " + e)
                page.show(contextURL + "/rest/userVS/IBAN/" + e.model.item)
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_SMIME)
                operationVS.message = this.smimeMessage
                operationVS.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);
                }.bind(this))
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>