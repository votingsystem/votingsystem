<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-smime-transactionvs">
    <template>
        <style>
        :host{color: #667;}
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
            <div style="margin: 0px auto;">
                <div class="layout horizontal center center-justified">
                    <div class="pageHeader" style="text-decoration: underline;"><h3>{{messageType}}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg" style="text-align: center;">
                    <b>${msg.dateLbl}: </b> {{timeStampDate}}
                </div>
                <div hidden="{{!messageToUser}}" layout horizontal center center-justified  class="messageToUser">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <div class="horizontal layout center center-justified" style="margin:0 0 10px 0;">
                    <div style="font-size: 1.1em; font-weight: bold;">${msg.subjectLbl}:</div>
                    <div style="font-size: 1.2em; ">{{smimeMessageContent.subject}}</div>
                </div>
                <div class="horizontal layout center center-justified">
                    <div style="font-size: 1.1em;"><b>${msg.amountLbl}: </b>
                        {{smimeMessageContent.amount}} {{smimeMessageContent.currencyCode}}</div>
                    <div hidden="{{!smimeMessageContent.timeLimited}}">
                        <div title="${msg.timeLimitedDateMsg} '{{smimeMessageContent.validTo}}'" class="pageHeader" style="margin: 0 20px 0 0;"><b>
                            ${msg.timeLimitedLbl}</b>
                        </div>
                    </div>
                </div>
                <div style="margin-left: 20px;">
                    <div class="actorLbl" style=" margin:10px 0px 0px 0px;">${msg.senderLbl}</div>
                    <div>
                        <div><b>${msg.nameLbl}:  </b>{{smimeMessageContent.fromUserVS.name}}</div>
                        <div class="horizontal layout" on-click="showFromUserVSByIBAN">
                            <div><b>${msg.IBANLbl}: </b></div>
                            <div class="iban-link">{{smimeMessageContent.fromUserIBAN}}</div>
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
                <div class="horizontal layout center center-justified flex" style="margin: 5px 0 10px 0; min-width: 400px;">
                    <div>
                        <a style="font-size: 0.8em; height: 0.8em;">
                            <i class="fa fa-tag" style="color: #888;"></i> {{tagName}} </a>
                    </div>
                    <div hidden="{{!isClientToolConnected}}" class="horizontal layout end-justified flex"
                         style="margin:10px 0px 10px 0px; font-size: 0.9em;">
                        <button on-click="checkReceipt">
                            <i class="fa fa-certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>

    </template>
    <script>
        Polymer({
            is:'message-smime-transactionvs',
            properties: {
                smimeMessageContent: {type:Object, observer:'smimeMessageContentChanged'},
                isClientToolConnected: {type:Boolean, value: false},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean, value: false},
                messageToUser: {type:String},
                smimeMessage: {type:String},
                timeStampDate: {type:String},
                messageType: {type:String},
                fromUserIBAN: {type:String},
                toUserIBAN: {type:String},
                caption: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined)
                document.querySelector("#voting_system_page").addEventListener('votingsystem-client-msg',
                        function() {  this.isClientToolConnected = true }.bind(this))
            },
            smimeMessageContentChanged:function() {
                this.messageToUser = null
                console.log(this.tagName + " - smimeMessageContentChanged: " + JSON.stringify(this.smimeMessageContent))
                this.tagName = this.smimeMessageContent.tags[0]
                switch (this.smimeMessageContent.type) {
                    case 'FROM_GROUP_TO_ALL_MEMBERS':
                        this.messageType = "${msg.transactionVSFromGroupToAllMembers}"
                        this.fromUserIBAN = this.smimeMessageContent.fromUserIBAN
                        break;
                }
                sendSignalVS({caption:this.messageType})
            },
            showFromUserVSByIBAN:function(e) {
                page.show(contextURL + "/rest/userVS/IBAN/" + this.fromUserIBAN, '_blank')
            },
            showToUserVSByIBAN:function(e) {
                page.show(contextURL + "/rest/userVS/IBAN/" + this.toUserIBAN, '_blank')
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
