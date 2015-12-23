<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-smime-votevs-canceler">
    <template>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
                text-align: center;
            }
            .timeStampMsg {
                color:#aaaaaa; font-size:1.1em; margin:0 0 15px 0;font-style:italic;
            }
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="pageHeader"><h3>{{smimeMessageContent.operation}}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.timeStampDateLbl}: </b>{{timeStampDate}}
                </div>
                <div hidden="{{!messageToUser}}" class="messageToUser layout horizontal center center-justified">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <div><b>${msg.hashAccessRequestLbl}: </b><span>{{smimeMessageContent.hashAccessRequestBase64}}</span></div>
                <div><b>${msg.originHashAccessRequestLbl}: </b><span>{{smimeMessageContent.originHashAccessRequest}}</span></div>
                <div><b>${msg.hashCertVSLbl}: </b><span>{{smimeMessageContent.hashCertVSBase64}}</span></div>
                <div><b>${msg.originHashCertVote}: </b><span>{{smimeMessageContent.originHashCertVote}}</span></div>
                <div class="layout horizontal">
                    <div class="flex"></div>
                    <div hidden="{{!isClientToolConnected}}" class="flex horizontal layout end-justified" style="margin:10px 0px 10px 0px;">
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
            is:'message-smime-votevs-canceler',
            properties: {
                smimeMessageContent:{type:Object, value:{}, observer:'smimeMessageContentChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready - " + document.querySelector("#voting_system_page"))
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            smimeMessageContentChanged:function() {
                this.messageToUser = null
                if('CANCEL_VOTE' != this.smimeMessageContent.operation )
                    this.messageToUser = '${msg.smimeTypeErrorMsg}' + " - " + this.smimeMessageContent.operation
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_SMIME)
                operationVS.message = this.smimeMessage
                operationVS.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage)
                }.bind(this))
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>