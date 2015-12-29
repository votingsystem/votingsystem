<%@ page contentType="text/html; charset=UTF-8" %>
<dom-module name="message-smime">
    <template>
        <style>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic;  }
        .systemLbl { color:#6c0404; font-size:1.1em;  }
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div>
                <div  layout horizontal center center-justified>
                    <div class="pageHeader"><h3>{{caption}}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.dateLbl}: </b> {{timeStampDate}}
                </div>
                <div hidden="{{!messageToUser}}" layout horizontal center center-justified  class="messageToUser">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div><b>${msg.subjectLbl}: </b>{{smimeMessageContent.subject}}</div>
                <div class="horizontal layout center center-justified">
                    <div><b>${msg.amountLbl}: </b> {{smimeMessageContent.amount}} {{smimeMessageContent.currencyCode}}</div>
                    <div hidden="{{!smimeMessageContent.timeLimited}}" class="pageHeader" style="margin: 0 0 0 20px;"><b>
                        ${msg.timeLimitedLbl}</b>
                    </div>
                </div>

                <div id="fromUserDivContainer" style="margin-left: 20px;">
                    <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;color: #621;">
                        ${msg.senderLbl}</div>
                    <div id="fromUserDiv">
                        <div><b>${msg.nameLbl}:</b> <span>{{smimeMessageContent.fromUserVS.name}}</span></div>
                        <div><b>${msg.IBANLbl}:</b> <span>{{smimeMessageContent.fromUserIBAN}}</span></div>
                    </div>
                </div>
                <div hidden="{{!isReceptorVisible}}" style="margin:20px 0px 0px 20px;">
                    <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold;color: #621;">{{receptorLbl}}</div>
                    <div class="layout horizontal">
                        <div><b>${msg.IBANLbl}: </b></div>
                        <div>
                            <template is="dom-repeat" items="{{smimeMessageContent.toUserIBAN}}" as="IBAN">
                                <div on-click="showToUserIBAN" style="text-decoration: underline; color: #0000ee; cursor: pointer;">{{IBAN}}</div>
                            </template>
                        </div>
                    </div>
                </div >

                <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold;margin:5px 0 0 0;color: #621">
                    {{receptorLbl}}
                </div>
                <div><b>{{toUserName}}</b></div>
                <div class="layout horizontal">
                    <div><b>${msg.IBANLbl}: </b>{{IBAN}}</div>
                </div>

                <div class="layout horizontal center center-justified" style="margin: 15px 0 0 0;">
                    <template is="dom-repeat" items="{{signedDocument.tags}}" as="tag">
                        <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag.name}}</a>
                    </template>
                </div>
                <div>
                    <div class="flex"></div>
                    <div hidden="{{!isClientToolConnected}}" class="horizontal layout end-justified" style="margin:10px 0px 10px 0px;">
                        <button style="font-size: 1.1em;" on-click="checkReceipt">
                            <i class="fa fa-certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'message-smime',
            properties: {
                smimeMessageContent:{type:Object, value:{}, observer:'smimeMessageContentChanged'},
                isClientToolConnected: {type:Boolean, value: false}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            smimeMessageContentChanged:function() {
                this.messageToUser = null
                this.isReceptorVisible = true
                if(this.smimeMessageContent.toUserIBAN != null && this.smimeMessageContent.toUserIBAN.length > 1) {
                    this.receptorLbl = '${msg.receptorsLbl}'
                } else this.receptorLbl = '${msg.receptorLbl}'
                switch (this.smimeMessageContent.operation) {
                    case 'FROM_BANKVS':
                        this.caption = "${msg.transactionVSFromBankVS}"
                        break;
                    case 'FROM_GROUP_TO_ALL_MEMBERS':
                        this.isReceptorVisible = false
                        this.caption = "${msg.transactionVSFromGroupToAllMembers}"
                        break;
                    case 'FROM_GROUP_TO_MEMBER_GROUP':
                        this.caption = "${msg.transactionVSFromGroupToMemberGroup}"
                        break;
                    case 'CURRENCY_PERIOD_INIT':
                        this.caption = "${msg.currencyPeriodInitLbl}"
                        this.$.fromUserDiv.innerHTML = "${msg.systemLbl}"
                        this.$.fromUserDiv.classList.add("systemLbl");
                        var receptor = []
                        receptor.push(this.smimeMessageContent.toUser.iban)
                        this.smimeMessageContent.toUserIBAN = receptor
                        this.smimeMessageContent.subject = this.smimeMessageContent.tag
                        break;
                    case 'CURRENCY':
                    case 'CURRENCY_SEND':
                        this.caption = "${msg.anonymousTransactionVSLbl}"
                        this.$.fromUserDivContainer.style.display = 'none'
                        this.isReceptorVisible = false
                        this.iban = this.smimeMessageContent.toUserIBAN
                        this.toUserName = this.smimeMessageContent.toUserName
                        this.receptorLbl = '${msg.receptorLbl}'
                        break;
                    default:
                        this.caption = this.smimeMessageContent.operation

                }
            },
            showToUserInfo:function(e) {
                var groupURL = contextURL + "/rest/groupVS/" + e.model.item.toUserVS.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = contextURL + "/rest/groupVS/" +  e.model.item.fromUserVS.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showInfoIBAN:function(e) {
                var fromUserIBANInfoURL = contextURL + "/rest/IBAN/from/" + e.model.item.fromUserVS.sender.fromUserIBAN
                console.log(this.tagName + " - showInfoIBAN - fromUserIBANInfoURL: " + fromUserIBANInfoURL)
            },
            showToUserIBAN:function(e) {
                console.log(this.tagName + " - showToUserIBAN - " + e)
                page.show(contextURL + "/rest/userVS/IBAN/",  e.model.item, '_blank')
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_SMIME)
                operationVS.message = this.smimeMessage
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>