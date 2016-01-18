<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transactionvs-data">
    <template>
        <style>
        .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic;  }
        .IBANLink{ text-decoration: underline; color: #0000ee; cursor: pointer; }
        </style>
        <div id="modalDialog" class="modalDialog">
            <div style="width: 450px;">
                <div class="layout horizontal center center-justified" style="margin: 0px auto; max-width:800px; color:#667;">
                    <div hidden="{{!caption}}" flex style="font-size: 1.4em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>

                <div style="margin: 10px auto 0 auto; max-width:800px; color:#667;">
                    <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                        <b>${msg.dateLbl}:</b> <span>{{timeStampDate}}</span>
                    </div>

                    <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                    <div><b>${msg.subjectLbl}: </b>{{transactionvs.subject}}</div>

                    <div><b>${msg.amountLbl}: </b><span>{{transactionvs.amount}}</span> <span>{{transactionvs.currencyCode}}</span></div>

                    <div class="horizontal layout">
                        <div><b>${msg.dateLbl}: </b>{{getDate(transactionvs.dateCreated)}}</div>
                        <div hidden="{{!transactionvs.validTo}}" style="margin: 0 0 0 15px;"><b>${msg.validToLbl}: </b>
                            <span>{{getDate(transactionvs.validTo)}}</span></div>
                    </div>
                    <div hidden="{{!isSenderVisible}}" style="margin-left: 20px;">
                        <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;color: #621;">
                            ${msg.senderLbl}</div>
                        <div id="fromUserDiv">
                            <div><b>${msg.nameLbl}: </b>{{getFromUserName(transactionvs)}}</div>
                            <div on-click="showFromUserIBAN">
                                <b>${msg.IBANLbl}: </b>
                                <span class="IBANLink">{{getFromUserIBAN(transactionvs)}}</span>
                            </div>
                        </div>
                    </div>

                    <div hidden="{{!isReceptorVisible}}" style="margin:20px 0px 0px 20px;">
                        <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold;color: #621">{{receptorLbl}}</div>
                        <div hidden="{{receptorMsg}}">
                            <div><b>${msg.nameLbl}: </b> <span>{{transactionvs.toUserVS.name}}</span></div>
                            <div class="horizontal layout">
                                <b>${msg.IBANLbl}: </b>
                                <div on-click="showToUserIBAN" class="IBANLink">{{transactionvs.toUserVS.iban}}</div>
                            </div>
                        </div>
                    </div >
                    <div hidden="{{!receptorMsg}}" style="margin: 15px 0 15px 0;">{{receptorMsg}}</div>
                </div>

                <div class="horizontal layout center center-justified" style="margin: 20px 0 0 0;">
                    <div hidden="{{tagsHidden}}" class="flex">
                        <template is="dom-repeat" items="{{transactionvs.tags}}" as="tag">
                            <a class="btn btn-default" style="font-size: 0.7em; height: 0.8em; padding:2px 5px 7px 5px;">
                                <i class="fa fa-tag" style="color: #888;"></i> <span>{{tag}}</span></a>
                        </template>
                    </div>
                    <div hidden="{{!isClientToolConnected}}" layout horizontal style="margin:1px 20px 0px 0px;">
                        <div class="flex"></div>
                        <div>
                            <button on-click="checkReceipt" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-certificate" style="color: #388746;"></i>  ${msg.checkSignatureLbl}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'transactionvs-data',
            properties: {
                transactionvs: {type:Object, value: {}, observer:'transactionvsChanged'},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean},
                isSenderVisible: {type:Boolean},
                isClientToolConnected: {type:Boolean, value: false},
                timeStampDate: {type:String},
                receptorLbl: {type:String},
                caption: {type:String},
                receptorMsg: {type:String},
                smimeMessage: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            getDate:function(dateStamp) {
                if(!dateStamp) return null
                return new Date(dateStamp).getDayWeekFormat()
            },
            getFromUserName: function (transactionvs) {
                var result
                if(transactionvs.fromUserVS) {
                    if(transactionvs.fromUserVS.sender != null && transactionvs.fromUserVS.sender.fromUser != null)
                        result = transactionvs.fromUserVS.sender.fromUser
                    else result = transactionvs.fromUserVS.name
                }
                return result
            },
            getFromUserIBAN: function (transactionvs) {
                var result
                if(transactionvs.fromUserVS) {
                    if(transactionvs.fromUserVS.sender != null && transactionvs.fromUserVS.sender.fromUserIBAN != null)
                        result = transactionvs.fromUserVS.sender.fromUserIBAN
                    else result = transactionvs.fromUserVS.iban
                }
                return result
            },
            transactionvsChanged:function() {
                this.receptorMsg = null
                this.isSenderVisible = true
                this.isReceptorVisible = true
                if(this.transactionvs.toUserIBAN != null && this.transactionvs.toUserIBAN.length > 1) {
                    this.receptorLbl = '${msg.receptorsLbl}'
                } else this.receptorLbl = '${msg.receptorLbl}'
                console.log(this.tagName + " - transactionvsChanged - transactionvs.messageSMIMEURL: " +
                        this.transactionvs.messageSMIMEURL)
                switch (this.transactionvs.type) {
                    case 'FROM_USERVS':
                        this.caption = "${msg.transactionVSFromUserVS}"
                        break;
                    case 'FROM_BANKVS':
                        this.caption = "${msg.transactionVSFromBankVS}"
                        break;
                    case 'FROM_GROUP_TO_ALL_MEMBERS':
                        this.isReceptorVisible = false
                        this.caption = "${msg.transactionVSFromGroupToAllMembers}"
                        this.receptorMsg = "${msg.transactionVSGroupVSReceptorsMsg}".format(
                                this.transactionvs.numChildTransactions)
                        break;
                    case 'FROM_GROUP_TO_MEMBER_GROUP':
                        this.caption = "${msg.transactionVSFromGroupToMemberGroup}"
                        break;
                    case 'CURRENCY_PERIOD_INIT':
                        this.caption = "${msg.currencyPeriodInitLbl}"
                        this.$.fromUserDiv.innerHTML = "${msg.systemLbl}"
                        break;
                    case 'CURRENCY_SEND':
                        this.caption = "${msg.currencySendLbl}"
                        this.isSenderVisible = false
                        break;
                    case 'CURRENCY_REQUEST':
                        this.caption = "${msg.currencyRequestLbl}"
                        this.isSenderVisible = false
                        this.isReceptorVisible = false
                        break;
                    case 'FROM_BANKVS':
                        this.caption = "${msg.transactionVSFromBankVS}"
                        break;
                    default:
                        this.caption = this.transactionvs.type

                }
                this.tagsHidden = (!this.transactionvs.tags || this.transactionvs.tags.length === 0)
                this.timeStampDate = this.getDate(this.transactionvs.dateCreated)
            },
            showToUserInfo:function(e) {
                var groupURL = contextURL + "/rest/groupVS/" + this.transactionvs.toUserVS.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = contextURL + "/rest/groupVS/" +  this.transactionvs.fromUserVS.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showFromUserIBAN:function(e) {
                var serviceURL = contextURL + "/rest/userVS/IBAN/" + this.getFromUserIBAN(this.transactionvs)
                window.open(serviceURL, '_blank');
            },
            showToUserIBAN:function(e) {
                var serviceURL = contextURL + "/rest/userVS/IBAN/" + this.transactionvs.toUserVS.iban
                window.open(serviceURL, '_blank');
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_SMIME)
                if(this.smimeMessage == null) {
                    operationVS.serviceURL = this.transactionvs.messageSMIMEURL
                    operationVS.operation = Operation.OPEN_SMIME_FROM_URL
                } else operationVS.message = this.smimeMessage
                VotingSystemClient.setMessage(operationVS);
            },
            show: function(transactionvs) {
                this.transactionvs = transactionvs
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            close: function(e) {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>