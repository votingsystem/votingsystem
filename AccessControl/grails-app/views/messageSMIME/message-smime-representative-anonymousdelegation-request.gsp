<%@ page import="org.votingsystem.model.TypeVS" %>

<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">

<polymer-element name="message-smime-representative-anonymousdelegation-request"
                 attributes="smimeMessageContent smimeMessage isClientToolConnected timeStampDate">
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
        <div layout vertical style="margin: 10px auto; max-width:1000px;">
            <div class="pageHeader"  layout horizontal center center-justified>
                <h3><g:message code="anonymousdelegationRequest"/></h3>
            </div>
            <div class="timeStampMsg" style="display:{{timeStampDate ? 'block':'none'}}">
                <b><g:message code="timeStampDateLbl"/>: </b>{{timeStampDate}}
            </div>

            <template if="{{messageToUser}}">
                <div  layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </template>
            <div><b><g:message code="weeksAnonymousDelegation"/>: </b>{{smimeMessageContent.weeksOperationActive}}</div>
            <template if="{{isClientToolConnected}}">
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div style="margin:10px 0px 10px 0px;">
                        <paper-button raised on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-certificate" style="margin:0 5px 0 2px;"></i>  <g:message code="checkReceiptLbl"/>
                        </paper-button>
                    </div>
                    <div flex></div>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer('message-smime-representative-anonymousdelegation-request', {
            publish: {
                smimeMessageContent: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
            ready: function() {
                console.log(this.tagName + " - ready - " + document.querySelector("#voting_system_page"))
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
                if('${TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST.toString()}' != this.smimeMessageContent.operation )
                    this.messageToUser = '<g:message code="smimeTypeErrorMsg"/>' + " - " + this.smimeMessageContent.operation
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