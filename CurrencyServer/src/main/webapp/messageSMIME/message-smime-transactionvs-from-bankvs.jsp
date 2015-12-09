<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-smime-transactionvs-from-bankvs">
    <template>
        <style>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .actorLbl {font-size: 1.2em; text-decoration: underline;font-weight: bold; color: #621; }
        .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic; }
        .iban-link {text-decoration: underline; color: #0000ee; cursor: pointer;}
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div class="layout vertical center center-justified" style="margin: 0px auto; color: #667;">
                <div class="horizontal layout" style="margin: 0 0 20px 0;">
                    <div class="pageHeader" style="margin:0 0 0 20px;font-size: 1.5em;text-align: center;">
                        ${msg.transactionVSFromBankVS}
                    </div>
                </div>

                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.dateLbl}: </b> <span>{{timeStampDate}}</span>
                </div>

                <div hidden="{{!messageToUser}}">
                    <div  layout horizontal center center-justified  class="messageToUser">
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                </div>

                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div><b>${msg.subjectLbl}: </b>{{smimeMessageContent.subject}}</div>
                <div class="horizontal layout">
                    <div class="flex"><b>${msg.amountLbl}: </b>{{smimeMessageContent.amount}} {{smimeMessageContent.currencyCode}}</div>
                    <div hidden="{{!smimeMessageContent.timeLimited}}" class="pageHeader"
                         style="margin: 0 20px 0 0;"><b>${msg.timeLimitedLbl}</b> ${msg.timeLimitedDateMsg} <span>{{smimeMessageContent.validTo}}</span>
                    </div>
                </div>
                <div style="margin-left: 20px;">
                    <div class="actorLbl" style=" margin:10px 0px 0px 0px;">${msg.senderLbl}</div>
                    <div>
                        <div><b>${msg.nameLbl}: </b><span>{{smimeMessageContent.fromUserVS.name}}</span></div>
                        <div><b>${msg.IBANLbl}: </b><span>{{smimeMessageContent.fromUserIBAN}}</span></div>
                        <div on-click="showFromUserVSByIBAN">
                            <b>${msg.bankVSIBANLbl}: </b>
                            <span class="iban-link">{{smimeMessageContent.fromUserIBAN}}</span>
                        </div>
                    </div>
                </div>
                <div hidden="{{!isReceptorVisible}}" style="margin:20px 0px 0px 20px;">
                    <div class="actorLbl">${msg.receptorLbl}</div>
                    <div class="layout horizontal">
                        <div><b>${msg.IBANLbl}: </b></div>
                        <div>
                            <template is="dom-repeat" items="{{smimeMessageContent.toUserIBAN}}" as="IBAN">
                                <div on-click="showToUserVSByIBAN" class="iban-link">{{IBAN}}</div>
                            </template>
                        </div>
                    </div>
                </div >
                <div  layout horizontal center center-justified style="margin: 15px 0 0 0;">
                    <div hidden="{{tagsHidden}}" layout horizontal center center-justified>
                        <template is="dom-repeat" items="{{signedDocument.tags}}" as="tag">
                            <a class="btn btn-default" style="font-size: 0.7em;">
                                <i class="fa fa-tag" style="color: #888;"></i> {{tag}}</a>
                        </template>
                    </div>
                    <div class="flex"></div>
                    <div hidden="{{!isClientToolConnected}}" class="flex horizontal layout end-justified" style="margin:10px 0px 10px 0px;">
                        <button on-click="checkReceipt" style="color: #388746;">
                            <i class="fa fa-certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>

    </template>
    <script>
        Polymer({
            is:'message-smime-transactionvs-from-bankvs',
            properties: {
                smimeMessageDto:{type:Object, observer:'smimeMessageChanged'},
                smimeMessageContent: {type:Object},
                isClientToolConnected: {type:Boolean, value: false},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean, value: true},
                messageToUser: {type:String},
                smimeMessage: {type:String},
                timeStampDate: {type:String},
                caption: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined)
                document.querySelector("#voting_system_page").addEventListener('votingsystem-client-msg',
                        function() {  this.isClientToolConnected = true }.bind(this))
                sendSignalVS({caption:"${msg.transactionVSFromBankVS}"})
            },
            smimeMessageChanged:function() {
                this.smimeMessageContent = this.smimeMessageDto.signedContentMap
                this.timeStampDate = this.smimeMessageDto.timeStampDate
                this.messageToUser = null
                console.log(this.tagName + " - smimeMessageChanged: " + JSON.stringify(this.smimeMessageDto) )
                this.tagsHidden = (!this.smimeMessageContent || !this.smimeMessageContent.tags || this.smimeMessageContent.tags.length === 0)
            },
            showFromUserVSByIBAN:function(e) {
                page.show(contextURL + "/rest/userVS/IBAN/" + this.smimeMessageContent.fromUserIBAN, '_blank')
            },
            showToUserVSByIBAN:function(e) {
                console.log(this.tagName + " - showUserVSByIBAN:" + e)
                page.show(contextURL + "/rest/userVS/IBAN/" + e.model.IBAN)
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_SMIME)
                if(this.smimeMessageDto.smimeMessage) operationVS.message = this.smimeMessageDto.smimeMessage
                else operationVS.message = this.smimeMessage
                operationVS.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);
                }.bind(this))
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>
