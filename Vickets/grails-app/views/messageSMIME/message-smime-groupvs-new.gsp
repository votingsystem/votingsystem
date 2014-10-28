<%@ page import="org.votingsystem.model.TypeVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">

<polymer-element name="message-smime-groupvs-new" attributes="signedDocument smimeMessage isClientToolConnected timeStampDate">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser { font-weight: bold; margin:10px auto 10px auto;
                background: #f9f9f9;  padding:10px 20px 10px 20px;
            }
            .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic; }
        </style>
        <div layout vertical style="margin: 0px auto; max-width:800px;">
            <div  layout horizontal center center-justified>
                <div flex></div>
                <div class="pageHeader"><h3><g:message code="newGroupVSMsgSubject"/></h3></div>
                <div flex horizontal layout end-justified style="margin:10px 0px 10px 0px;">
                    <template if="{{isClientToolConnected}}">
                        <paper-button raised on-click="{{checkReceipt}}">
                            <i class="fa fa-certificate"></i>  <g:message code="checkSignatureLbl"/>
                        </paper-button>
                    </template>
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
            <div style=""><b><g:message code="nameLbl"/>: </b>{{signedDocument.groupvsName}}</div>
            <div class="eventContentDiv" style="">
                <vs-html-echo html="{{signedDocument.groupvsInfo}}"></vs-html-echo>
            </div>

            <template if="{{signedDocument.tags.length > 0}}">
                <div layout horizontal center center-justified style="margin: 15px 0 0 0;">
                    <template repeat="{{tag in signedDocument.tags}}">
                        <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag.name}}</a>
                    </template>
                </div>
            </template>
            <template if="{{isClientToolConnected}}">
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex></div>
                    <div style="margin:10px 0px 10px 0px;">
                        <paper-button raised on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-certificate"></i>  <g:message code="checkSignatureLbl"/>
                        </paper-button>
                    </div>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer('message-smime-groupvs-new', {
            publish: {
                signedDocument: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
            caption:null,
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_SMIME)
                webAppMessage.message = this.smimeMessage
                webAppMessage.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>