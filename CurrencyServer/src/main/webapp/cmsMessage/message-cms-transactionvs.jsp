<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-cms-transactionvs">
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
                    <div style="font-size: 1.2em; ">{{cmsMessageContent.subject}}</div>
                </div>
                <div class="horizontal layout center center-justified">
                    <div style="font-size: 1.1em;"><b>${msg.amountLbl}: </b>
                        {{cmsMessageContent.amount}} {{cmsMessageContent.currencyCode}}</div>
                    <div hidden="{{!cmsMessageContent.timeLimited}}">
                        <div title="${msg.timeLimitedDateMsg} '{{cmsMessageContent.validTo}}'" class="pageHeader" style="margin: 0 20px 0 0;"><b>
                            ${msg.timeLimitedLbl}</b>
                        </div>
                    </div>
                </div>
                <div style="margin-left: 20px;">
                    <div class="actorLbl" style=" margin:10px 0px 0px 0px;">${msg.senderLbl}</div>
                    <div>
                        <div><b>${msg.nameLbl}:  </b>{{cmsMessageContent.fromUserVS.name}}</div>
                        <div class="horizontal layout">
                            <div><b>${msg.IBANLbl}: </b></div>
                            <div class="iban-link" on-click="showByUserIBAN">{{cmsMessageContent.fromUserIBAN}}</div>
                        </div>
                    </div>
                </div>
                <div hidden="{{!isReceptorVisible}}" style="margin:20px 0px 0px 20px;">
                    <div class="actorLbl">${msg.receptorLbl}</div>
                    <div class="layout horizontal">
                        <div><b>${msg.IBANLbl}: </b></div>
                        <div>
                            <template is="dom-repeat" items="{{cmsMessageContent.toUserIBAN}}" as="IBAN">
                                <div on-click="showByUserIBAN" class="iban-link">{{IBAN}}</div>
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
            is:'message-cms-transactionvs',
            properties: {
                cmsMessageContent: {type:Object, observer:'cmsMessageContentChanged'},
                isClientToolConnected: {type:Boolean, value: false},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean, value: false},
                messageToUser: {type:String},
                cmsMessage: {type:String},
                timeStampDate: {type:String},
                messageType: {type:String},
                fromUserIBAN: {type:String},
                toUserIBAN: {type:String},
                caption: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            cmsMessageContentChanged:function() {
                this.messageToUser = null
                console.log(this.tagName + " - cmsMessageContentChanged: " + JSON.stringify(this.cmsMessageContent))
                this.tagName = this.cmsMessageContent.tags[0]
                switch (this.cmsMessageContent.type) {
                    case 'FROM_GROUP_TO_ALL_MEMBERS':
                        this.messageType = "${msg.transactionVSFromGroupToAllMembers}"
                        this.fromUserIBAN = this.cmsMessageContent.fromUserIBAN
                        break;
                }
                sendSignalVS({caption:this.messageType})
            },
            showByUserIBAN:function(e) {
                page.show(vs.contextURL + "/rest/userVS/IBAN/" + this.fromUserIBAN, '_blank')
            },
            showByUserIBAN:function(e) {
                console.log(this.tagName + " - showByUserIBAN - " + e)
                if(e.model) IBAN = e.model.IBAN
                else IBAN = e.target.innerText
                window.open(vs.contextURL + "/#!" + vs.contextURL + "/rest/userVS/IBAN/" + IBAN, "_blank")
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                operationVS.message = this.cmsMessage
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>
