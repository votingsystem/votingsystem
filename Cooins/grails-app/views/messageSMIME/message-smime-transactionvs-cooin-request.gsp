<%@ page import="org.votingsystem.model.TypeVS" %>
<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>
<vs:webresource dir="core-tooltip" file="core-tooltip.html"/>

<polymer-element name="message-smime-transactionvs-cooin-request" attributes="signedDocument smimeMessage
        isClientToolConnected timeStampDate">
    <template>
        <g:include view="/include/styles.gsp"/>
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
        <div layout horizontal>
            <div layout vertical style="margin: 0px auto; color: #667;">
                <div horizontal layout style="margin: 0 0 10px 0; min-width: 400px;">
                    <div layout horizontal center center-justified style="margin: 3px 0 0 0;">
                        <a class="btn btn-default" style="font-size: 0.7em; height: 0.8em;">
                            <i class="fa fa-tag" style="color: #888;"></i> {{signedDocument.tag}}</a>
                    </div>
                    <div layout horizontal center center-justified class="pageHeader" style="margin:0 0 0 20px; font-size: 1.2em;">
                        <g:message code="cooinRequestLbl"/>
                    </div>
                    <div flex horizontal layout end-justified style="margin:10px 0px 10px 0px; font-size: 0.9em;">
                        <template if="{{isClientToolConnected}}">
                            <paper-button raised on-click="{{checkReceipt}}">
                                <i class="fa fa-certificate"></i>  <g:message code="checkSignatureLbl"/>
                            </paper-button>
                        </template>
                    </div>
                </div>

                <div hidden?="{{!timeStampDate}}" class="timeStampMsg" style="text-align: center;">
                    <b><g:message code="dateLbl"/>: </b>
                        <time is="local-time" datetime="{{timeStampDate}}" day="numeric" month="short" year="numeric"
                              hour="numeric" minute="numeric"/>
                </div>
                <div hidden?="{{!messageToUser}}" layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div horizontal layout center-justified>
                    <div style="font-size: 1.1em;"><b><g:message code="amountLbl"/>: </b>
                        {{signedDocument.totalAmount}} {{signedDocument.currencyCode}}</div>
                    <template if="{{signedDocument.isTimeLimited}}">
                        <core-tooltip large label="<g:message code="timeLimitedDateMsg"/> '{{signedDocument.validTo}}'" position="left">
                            <div class="pageHeader" style="margin: 0 20px 0 0;"><b>
                                ${message(code: 'timeLimitedLbl', null).toUpperCase()}</b>
                            </div>
                        </core-tooltip>
                    </template>
                </div>
                <div horizontal layout center-justified>
                    <div style="margin-left: 20px;">
                        <div class="actorLbl" style=" margin:10px 0px 0px 0px;"><g:message code="senderLbl"/></div>
                        <div>
                            <div style=""><b><g:message code="nameLbl"/>:  </b>{{signedDocument.fromUserVS.name}}</div>
                        </div>
                    </div>
                </div>

            </div>
        </div>

    </template>
    <script>
        Polymer('message-smime-transactionvs-cooin-request', {
            publish: {
                signedDocument: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
            isReceptorVisible:true,
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('votingsystem-clienttoolconnected', function() {
                    this.isClientToolConnected = true
                }.bind(this))
                sendSignalVS({caption:"<g:message code="transactionVSFromBankVS"/>"})
            },
            attached: function () {
                console.log(this.tagName + " - attached")
                this.fire('attached', null);
            },
            signedDocumentChanged:function() {
                this.messageToUser = null
                console.log(this.tagName + " - signedDocumentChanged: " + JSON.stringify(this.signedDocument))
            },
            showFromUserVSByIBAN:function(e) {
                loadURL_VS("${createLink( controller:'userVS')}/BANKVS/IBAN/" + this.signedDocument.bankIBAN, '_blank')
            },
            showToUserVSByIBAN:function(e) {
                console.log(this.tagName + " - showUserVSByIBAN - " + e)
                loadURL_VS("${createLink( controller:'userVS')}/IBAN/" + e.target.templateInstance.model.IBAN, '_blank')
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(Operation.OPEN_SMIME)
                webAppMessage.message = this.smimeMessage
                webAppMessage.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>