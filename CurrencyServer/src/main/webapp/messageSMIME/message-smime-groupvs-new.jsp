<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-smime-groupvs-new">
    <template>
        <style>
            .messageToUser { font-weight: bold; margin:10px auto 10px auto;
                background: #f9f9f9;  padding:10px 20px 10px 20px;
            }
            .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic; }
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="pageHeader"><h3>${msg.newGroupVSMsgSubject}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.dateLbl}: </b> <span>{{timeStampDate}}</span>
                </div>
                <div hidden="{{!messageToUser}}" layout horizontal center center-justified  class="messageToUser">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div><b>${msg.nameLbl}: </b><span>{{signedDocument.name}}</span></div>
                <div class="eventContentDiv">
                    <vs-html-echo html="{{signedDocument.groupvsInfo}}"></vs-html-echo>
                </div>
                <div hidden="{{tagsHidden}}" class="layout horizontal center center-justified" style="margin: 15px 0 0 0;">
                    <template is="dom-repeat" items="{{signedDocument.tags}}" as="tag">
                        <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag}}</a>
                    </template>
                </div>
                <div hidden="{{!isClientToolConnected}}" layout horizontal style="margin:0px 20px 0px 0px;">
                    <div class="flex"></div>
                    <div style="margin:10px 0px 10px 0px;">
                        <button on-click="checkReceipt" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'message-smime-groupvs-new',
            properties: {
                smimeMessageContent: {type:Object, value: {}, observer:'smimeMessageContentChanged'},
                isClientToolConnected: {type:Boolean, value: false},
                tagsHidden: {type:Boolean, value: true},
                messageToUser: {type:String},
                smimeMessage: {type:String},
                timeStampDate: {type:String},
                caption: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined)
                document.querySelector("#voting_system_page").addEventListener('votingsystem-client-connected',
                        function() {  this.isClientToolConnected = true }.bind(this))
            },
            smimeMessageContentChanged: function() {
                this.tagsHidden = (this.smimeMessageContent.tags.length === 0)
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
