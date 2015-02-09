<%@ page import="org.votingsystem.model.TypeVS" %>

<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-shadow" file="paper-shadow.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>

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
        <div layout vertical style="margin: 10px auto;">
            <div  layout horizontal center center-justified>
                <div flex></div>
                <div class="pageHeader"><h3><g:message code="anonymousdelegationRequest"/></h3></div>
                <div flex horizontal layout end-justified style="margin:10px 0px 10px 0px; display: block; min-width: 150px;">
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

            <template if="{{messageToUser}}">
                <div  layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </template>
            <div><b><g:message code="weeksAnonymousDelegation"/>: </b>{{smimeMessageContent.weeksOperationActive}}</div>
            <div horizontal layout style="margin:10px 0 0 0;"><b><g:message code="validFromLbl"/>:</b> {{smimeMessageContent.dateFrom}}
                <span style="margin: 0 0 0 20px;"><b><g:message code="toLbl"/>:</b></span> {{smimeMessageContent.dateFrom}}</div>
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