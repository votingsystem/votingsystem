<%@ page import="org.votingsystem.model.TypeVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog-transition.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">

<polymer-element name="message-smime-transactionvs-from-bankvs" attributes="signedDocument smimeMessage
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
                <div horizontal layout style="margin: 0 0 20px 0;">
                    <div>
                        <template if="{{signedDocument.tags.length > 0}}">
                            <div layout horizontal center center-justified style="margin: 3px 0 0 0;">
                                <template repeat="{{tag in signedDocument.tags}}">
                                    <a class="btn btn-default" style="font-size: 0.7em; height: 0.8em;">
                                        <i class="fa fa-tag" style="color: #888;"></i> {{tag}}</a>
                                </template>
                            </div>
                        </template>
                    </div>
                    <div class="pageHeader" style="margin:0 0 0 20px;font-size: 1.5em;text-align: center;">
                        <g:message code="transactionVSFromBankVS"/>
                    </div>
                </div>

                <div class="timeStampMsg" style="display:{{timeStampDate ? 'block':'none'}}">
                    <b><g:message code="timeStampDateLbl"/>: </b>{{timeStampDate}}
                </div>

                <div style="display:{{messageToUser? 'block':'none'}}">
                    <div  layout horizontal center center-justified  class="messageToUser">
                        <div>
                            <div id="messageToUser">{{messageToUser}}</div>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>

                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div style=""><b><g:message code="subjectLbl"/>: </b>{{signedDocument.subject}}</div>
                <div horizontal layout>
                    <div flex style=""><b><g:message code="amountLbl"/>: </b>{{signedDocument.amount}} {{signedDocument.currencyCode}}</div>
                    <template if="{{signedDocument.isTimeLimited}}">
                        <core-tooltip large label="<g:message code="timeLimitedDateMsg"/> '{{signedDocument.validTo}}'" position="left">
                            <div class="pageHeader" style="margin: 0 20px 0 0;"><b>
                                ${message(code: 'timeLimitedLbl', null).toUpperCase()}</b>
                            </div>
                        </core-tooltip>
                    </template>
                </div>
                <div style="margin-left: 20px;">
                    <div class="actorLbl" style=" margin:10px 0px 0px 0px;"><g:message code="senderLbl"/></div>
                    <div>
                        <div style=""><b><g:message code="nameLbl"/>:  </b>{{signedDocument.fromUser}}</div>
                        <div style=""><b><g:message code="IBANLbl"/>: </b>{{signedDocument.fromUserIBAN}}</div>
                        <div on-click="{{showFromUserVSByIBAN}}">
                            <b><g:message code="bankVSIBANLbl"/>: </b>
                            <span class="iban-link">{{signedDocument.fromUserIBAN}}</span>
                        </div>
                    </div>
                </div>
                <div style="margin:20px 0px 0px 20px;display:{{isReceptorVisible?'block':'none'}}">
                    <div class="actorLbl">
                        <g:message code="receptorLbl"/>
                    </div>
                    <div layout horizontal>
                        <div><b><g:message code="IBANLbl"/>: </b></div>
                        <div layout vertical>
                            <template repeat="{{IBAN in signedDocument.toUserIBAN}}">
                                <div on-click="{{showToUserVSByIBAN}}" class="iban-link">{{IBAN}}</div>
                            </template>
                        </div>
                    </div>
                </div >
                <div  layout horizontal center center-justified>
                    <div flex></div>
                    <div flex horizontal layout end-justified style="margin:10px 0px 10px 0px;">
                        <template if="{{isClientToolConnected}}">
                            <paper-button raised on-click="{{checkReceipt}}">
                                <i class="fa fa-certificate"></i>  <g:message code="checkSignatureLbl"/>
                            </paper-button>
                        </template>
                    </div>
                </div>
            </div>
        </div>

    </template>
    <script>
        Polymer('message-smime-transactionvs-from-bankvs', {
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