<%@ page import="org.votingsystem.model.TypeVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">

<polymer-element name="message-smime" attributes="smimeMessageContent smimeMessage isClientToolConnected timeStampDate">
    <template>
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
        <div layout vertical style="margin: 10px 30px; max-width:1000px;">
            <div  layout horizontal center center-justified>
                <div flex></div>
                <div class="pageHeader"><h3>{{smimeMessageContent.operation}}</h3></div>
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
            <vs-html-echo html="{{smimeMessageContentStr}}"></vs-html-echo>
        </div>
    </template>
    <script>
        Polymer('message-smime', {
            publish: {
                smimeMessageContent: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
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
            smimeMessageContentChanged:function() {
                this.messageToUser = null
                this.smimeMessageContentStr = JSON.stringify(this.smimeMessageContent)
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_SMIME)
                webAppMessage.message = this.smimeMessage
                webAppMessage.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>