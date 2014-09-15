<%@ page import="org.votingsystem.model.TypeVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">

<polymer-element name="vicket-transactionvs" attributes="signedDocument smimeMessage isClientToolConnected timeStampDate">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .timeStampMsg {
            color:#aaaaaa; font-size:1.1em; margin:0 0 15px 0;font-style:italic;
        }
        </style>
        <div layout vertical style="margin: 0px 30px; max-width:1000px;">
            <div class="pageHeader"  layout horizontal center center-justified><h3>{{caption}}</h3></div>
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
            <div style=""><b><g:message code="amountLbl"/>: </b>{{signedDocument.amount}} {{signedDocument.currency}}</div>
            <div style=""><b><g:message code="validToLbl"/>: </b>{{signedDocument.validTo}}</div>

            <div style="margin-left: 20px;">
                <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;">
                    <g:message code="pagerLbl"/></div>
                <div id="fromUserDiv">
                    <div style=""><b><g:message code="nameLbl"/>: </b>{{signedDocument.fromUser}}</div>
                    <div style=""><b><g:message code="IBANLbl"/>: </b>{{signedDocument.fromUserIBAN}}</div>
                </div>
            </div>
            <div style="margin:20px 0px 0px 20px;">
                <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
                <div id="toUserDiv">
                    <div style=""><b><g:message code="IBANLbl"/>: </b>{{signedDocument.toUserIBAN}}</div>
                </div>
            </div >
            <template if="{{signedDocument.tags.length > 0}}">
                <div layout horizontal center center-justified style="margin: 15px 0 0 0;">
                    <template repeat="{{tag in signedDocument.tags}}">
                        <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag.name}}</a>
                    </template>
                </div>
            </template>
        </div>
        <template if="{{isClientToolConnected}}">
            <div layout horizontal style="margin:0px 20px 0px 0px;">
                <div flex></div>
                <div style="margin:10px 0px 10px 0px;">
                    <votingsystem-button on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-certificate" style="margin:0 7px 0 3px;"></i>  <g:message code="checkSignatureLbl"/>
                    </votingsystem-button>
                </div>
            </div>
        </template>
    </template>
    <script>
        Polymer('vicket-transactionvs', {
            publish: {
                signedDocument: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
            caption:null,
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('votingsystem-clienttoolconnected', function() {
                    this.isClientToolConnected = true
                }.bind(this))
            },
            attached: function () {
                console.log(this.tagName + " - attached")
                this.fire('attached', null);
            },
            signedDocumentChanged:function() {
                this.messageToUser = null
                this.signedDocumentStr = JSON.stringify(this.signedDocument)
                console.log(this.tagName + " - signedDocumentChanged - signedDocument: " + this.signedDocumentStr)
                switch (this.signedDocument.operation) {
                    case 'VICKET_DEPOSIT_FROM_VICKET_SOURCE':
                        this.caption = "<g:message code="depositFromVicketSourceLbl"/>"
                        break
                    default:
                        this.caption = signedDocument.operation
                }


            },
            showToUserInfo:function(e) {
                var groupURL = "${createLink(uri:'/groupVS')}/" + e.target.templateInstance.model.signedDocument.toUserVS.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = "${createLink(uri:'/groupVS')}/" +  e.target.templateInstance.model.signedDocument.fromUserVS.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showInfoIBAN:function(e) {
                var fromUserIBANInfoURL = "${createLink(uri:'/IBAN')}/from/" + e.target.templateInstance.model.signedDocument.fromUserVS.payer.fromUserIBAN
                console.log(this.tagName + " - showInfoIBAN - fromUserIBANInfoURL: " + fromUserIBANInfoURL)
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_RECEIPT)
                webAppMessage.message = this.smimeMessage

                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);}}
                webAppMessage.callerCallback = objectId
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>