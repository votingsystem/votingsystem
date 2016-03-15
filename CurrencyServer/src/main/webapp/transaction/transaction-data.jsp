<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transaction-data">
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
                    <div><b>${msg.subjectLbl}: </b>{{transaction.subject}}</div>

                    <div><b>${msg.amountLbl}: </b><span>{{transaction.amount}}</span> <span>{{transaction.currencyCode}}</span></div>

                    <div class="horizontal layout">
                        <div><b>${msg.dateLbl}: </b>{{getDate(transaction.dateCreated)}}</div>
                        <div hidden="{{!transaction.validTo}}" style="margin: 0 0 0 15px;"><b>${msg.validToLbl}: </b>
                            <span>{{getDate(transaction.validTo)}}</span></div>
                    </div>
                    <div hidden="{{!isSenderVisible}}" style="margin-left: 20px;">
                        <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;color: #621;">
                            ${msg.senderLbl}</div>
                        <div id="fromUserDiv">
                            <div><b>${msg.nameLbl}: </b>{{getFromUserName(transaction)}}</div>
                            <div on-click="showFromUserIBAN">
                                <b>${msg.IBANLbl}: </b>
                                <span class="IBANLink">{{getFromUserIBAN(transaction)}}</span>
                            </div>
                        </div>
                    </div>

                    <div hidden="{{!isReceptorVisible}}" style="margin:20px 0px 0px 20px;">
                        <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold;color: #621">{{receptorLbl}}</div>
                        <div hidden="{{receptorMsg}}">
                            <div><b>${msg.nameLbl}: </b> <span>{{transaction.toUserName.name}}</span></div>
                            <div class="horizontal layout">
                                <b>${msg.IBANLbl}: </b>
                                <div on-click="showToUserIBAN" class="IBANLink">{{transaction.toUserName.iban}}</div>
                            </div>
                        </div>
                    </div >
                    <div hidden="{{!receptorMsg}}" style="margin: 15px 0 15px 0;">{{receptorMsg}}</div>
                </div>

                <div class="horizontal layout center center-justified" style="margin: 20px 0 0 0;">
                    <div hidden="{{tagsHidden}}" class="flex">
                        <template is="dom-repeat" items="{{transaction.tags}}" as="tag">
                            <a class="btn btn-default" style="font-size: 0.7em; height: 0.8em; padding:2px 5px 7px 5px;">
                                <i class="fa fa-tag" style="color: #888;"></i> <span>{{tag}}</span></a>
                        </template>
                    </div>
                    <div hidden="{{!isClientToolConnected}}" layout horizontal style="margin:1px 20px 0px 0px;">
                        <div class="flex"></div>
                        <div>
                            <button on-click="checkReceipt" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-x509Certificate" style="color: #388746;"></i>  ${msg.checkSignatureLbl}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'transaction-data',
            properties: {
                transaction: {type:Object, value: {}, observer:'transactionChanged'},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean},
                isSenderVisible: {type:Boolean},
                isClientToolConnected: {type:Boolean, value: false},
                timeStampDate: {type:String},
                receptorLbl: {type:String},
                caption: {type:String},
                receptorMsg: {type:String},
                cmsMessage: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            getDate:function(dateStamp) {
                if(!dateStamp) return null
                return new Date(dateStamp).getDayWeekFormat()
            },
            getFromUserName: function (transaction) {
                var result
                if(transaction.fromUserName) {
                    if(transaction.fromUserName.sender != null && transaction.fromUserName.sender.fromUserName != null)
                        result = transaction.fromUserName.sender.fromUserName
                    else result = transaction.fromUserName.name
                }
                return result
            },
            getFromUserIBAN: function (transaction) {
                var result
                if(transaction.fromUserName) {
                    if(transaction.fromUserName.sender != null && transaction.fromUserName.sender.fromUserIBAN != null)
                        result = transaction.fromUserName.sender.fromUserIBAN
                    else result = transaction.fromUserName.iban
                }
                return result
            },
            transactionChanged:function() {
                this.receptorMsg = null
                this.isSenderVisible = true
                this.isReceptorVisible = true
                if(this.transaction.toUserIBAN != null && this.transaction.toUserIBAN.length > 1) {
                    this.receptorLbl = '${msg.receptorsLbl}'
                } else this.receptorLbl = '${msg.receptorLbl}'
                console.log(this.tagName + " - transactionChanged - transaction.cmsMessageURL: " +
                        this.transaction.cmsMessageURL)
                switch (this.transaction.type) {
                    case 'FROM_USER':
                        this.caption = "${msg.transactionFromUser}"
                        break;
                    case 'FROM_BANK':
                        this.caption = "${msg.transactionFromBank}"
                        break;
                    case 'FROM_GROUP_TO_ALL_MEMBERS':
                        this.isReceptorVisible = false
                        this.caption = "${msg.transactionFromGroupToAllMembers}"
                        this.receptorMsg = "${msg.transactionGroupReceptorsMsg}".format(
                                this.transaction.numChildTransactions)
                        break;
                    case 'FROM_GROUP_TO_MEMBER_GROUP':
                        this.caption = "${msg.transactionFromGroupToMemberGroup}"
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
                    case 'FROM_BANK':
                        this.caption = "${msg.transactionFromBank}"
                        break;
                    default:
                        this.caption = this.transaction.type

                }
                this.tagsHidden = (!this.transaction.tags || this.transaction.tags.length === 0)
                this.timeStampDate = this.getDate(this.transaction.dateCreated)
            },
            showToUserInfo:function(e) {
                var groupURL = vs.contextURL + "/rest/group/" + this.transaction.toUserName.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = vs.contextURL + "/rest/group/" +  this.transaction.fromUserName.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showFromUserIBAN:function(e) {
                var serviceURL = vs.contextURL + "/rest/user/IBAN/" + this.getFromUserIBAN(this.transaction)
                window.open(serviceURL, '_blank');
            },
            showToUserIBAN:function(e) {
                var serviceURL = vs.contextURL + "/rest/user/IBAN/" + this.transaction.toUserName.iban
                window.open(serviceURL, '_blank');
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                if(this.cmsMessage == null) {
                    operationVS.serviceURL = this.transaction.cmsMessageURL
                    operationVS.operation = Operation.OPEN_CMS_FROM_URL
                } else operationVS.message = this.cmsMessage
                console.log(operationVS)
                console.log(this.transaction)
                VotingSystemClient.setMessage(operationVS);
            },
            show: function(transaction) {
                this.transaction = transaction
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