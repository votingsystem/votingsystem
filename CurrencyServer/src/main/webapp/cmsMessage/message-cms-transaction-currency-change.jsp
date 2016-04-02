<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-cms-transaction-currency-change">
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
                        ${msg.transactionCurrencyChange}
                    </div>
                </div>

                <div hidden="{{!timeStampDate}}" class="timeStampMsg" >
                    <b>${msg.dateLbl}: </b> <span>{{timeStampDate}}</span>
                </div>
                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div class="horizontal layout center-justified">
                    <div style="font-size: 1.1em;"><b>${msg.amountLbl}: </b>
                        <span>{{cmsMessageContent.batchAmount}}</span> <span>{{cmsMessageContent.currencyCode}}</span></div>
                    <div style="margin:0 0 0 10px;">
                         ${msg.timeLimitedDateMsg} '<span>{{cmsMessageContent.validTo}}</span>'
                    </div>
                </div>
                <div class="horizontal layout" style="margin:20px 0px 10px 0px;">
                    <div class="flex">
                        <a class="btn btn-default" style="font-size: 0.7em;padding:3px;">
                            <i class="fa fa-tag" style="color: #888;"></i> <span>{{cmsMessageContent.tag}}</span></a>
                    </div>
                    <div hidden="{{!isClientToolConnected}}">
                        <button raised on-click="checkReceipt" style="font-size: 1.1em;">
                            <i class="fa fa-x509Certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'message-cms-transaction-currency-change',
            properties: {
                cmsMessageContent: {type:Object},
                isClientToolConnected: {type:Boolean, value: false},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean, value: true},
                cmsMessage: {type:String},
                timeStampDate: {type:String},
                caption: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
                sendSignalVS({caption:"${msg.transactionCurrencyChange}"})
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                operationVS.message = this.cmsMessage
                vs.client.processOperation(operationVS);
            }
        });
    </script>
</dom-module>