<%@ page contentType="text/html; charset=UTF-8" %>
<dom-module name="message-cms">
    <template>
        <style>
        .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic;  }
        .systemLbl { color:#6c0404; font-size:1.1em;  }
        .iban-link {text-decoration: underline; color: #0000ee; cursor: pointer;}
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div>
                <div  layout horizontal center center-justified>
                    <div class="pageHeader"><h3>{{caption}}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.dateLbl}: </b> {{timeStampDate}}
                </div>
                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div style="font-size: 1.1em;"><b>${msg.subjectLbl}: </b>{{messageContent.subject}}</div>
                <div class="horizontal layout">
                    <div style="font-size: 1.1em;"><b>${msg.amountLbl}: </b> {{messageContent.amount}} {{messageContent.currencyCode}}</div>
                    <div hidden="{{!messageContent.timeLimited}}" class="pageHeader" style="margin: 0 0 0 20px;"><b>
                        ${msg.timeLimitedLbl}</b>
                    </div>
                </div>

                <div id="fromUserDivContainer" style="margin-left: 20px;">
                    <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;color: #621;">
                        ${msg.senderLbl}</div>
                    <div id="fromUserDiv">
                        <div><b>${msg.nameLbl}:</b> <span>{{messageContent.fromUserName.name}}</span></div>
                        <div> <b>${msg.IBANLbl}:</b>
                            <span on-click="showByUserIBAN" class="iban-link">{{messageContent.fromUserIBAN}}</span></div>
                    </div>
                </div>
                <div hidden="{{!isReceptorVisible}}" style="margin:20px 0px 0px 20px;">
                    <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold;color: #621;">{{receptorLbl}}</div>
                    <div class="layout horizontal">
                        <div><b>${msg.IBANLbl}: </b></div>
                        <div>
                            <template is="dom-repeat" items="{{messageContent.toUserIBAN}}" as="IBAN">
                                <div on-click="showByUserIBAN" class="iban-link">{{IBAN}}</div>
                            </template>
                        </div>
                    </div>
                </div >
                <div class="layout horizontal center center-justified" style="margin: 15px 0 0 0;">
                    <template is="dom-repeat" items="{{signedDocument.tags}}" as="tag">
                        <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag.name}}</a>
                    </template>
                </div>
                <div>
                    <div class="flex"></div>
                    <div hidden="{{!isClientToolConnected}}" class="horizontal layout end-justified" style="margin:10px 0px 10px 0px;">
                        <button style="font-size: 1.1em;" on-click="checkReceipt">
                            <i class="fa fa-x509Certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'message-cms',
            properties: {
                cmsMessageContent:{type:Object, value:{}, observer:'cmsMessageContentChanged'},
                isClientToolConnected: {type:Boolean, value: false}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            cmsMessageContentChanged:function() {
                this.isReceptorVisible = true
                if(this.cmsMessageContent.toUserIBAN != null && this.cmsMessageContent.toUserIBAN.length > 1) {
                    this.receptorLbl = '${msg.receptorsLbl}'
                } else this.receptorLbl = '${msg.receptorLbl}'
                switch (this.cmsMessageContent.operation) {
                    case 'FROM_BANK':
                        this.caption = "${msg.transactionFromBank}"
                        break;
                    case 'CURRENCY_PERIOD_INIT':
                        this.caption = "${msg.currencyPeriodInitLbl}"
                        this.$.fromUserDiv.innerHTML = "${msg.systemLbl}"
                        this.$.fromUserDiv.classList.add("systemLbl");
                        this.cmsMessageContent.toUserIBAN  = []
                        this.cmsMessageContent.toUserIBAN .push(this.cmsMessageContent.toUserName.iban)
                        this.cmsMessageContent.subject = this.cmsMessageContent.tag
                        break;
                    case 'CURRENCY':
                    case 'CURRENCY_SEND':
                        this.caption = "${msg.anonymousTransactionLbl}"
                        this.$.fromUserDivContainer.style.display = 'none'
                        this.isReceptorVisible = false
                        this.iban = this.cmsMessageContent.toUserIBAN
                        this.toUserName = this.cmsMessageContent.toUserName
                        this.receptorLbl = '${msg.receptorLbl}'
                        break;
                    case 'FROM_USER':
                        this.caption = "${msg.transactionFromUser}"
                        break;
                    default:
                        this.caption = this.cmsMessageContent.operation

                }
                this.messageContent = this.cmsMessageContent
            },
            showInfoIBAN:function(e) {
                var fromUserIBANInfoURL = vs.contextURL + "/rest/IBAN/from/" + e.model.item.fromUserName.sender.fromUserIBAN
                console.log(this.tagName + " - showInfoIBAN - fromUserIBANInfoURL: " + fromUserIBANInfoURL)
            },
            showByUserIBAN:function(e) {
                console.log(this.tagName + " - showByUserIBAN - " + e)
                if(e.model) IBAN = e.model.IBAN
                else IBAN = e.target.innerText
                window.open(vs.contextURL + "/#!" + vs.contextURL + "/rest/user/IBAN/" + IBAN, "_blank")
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                operationVS.message = this.cmsMessage
                vs.client.processOperation(operationVS);
            }
        });
    </script>
</dom-module>