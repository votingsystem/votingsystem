<%@ page import="org.votingsystem.model.TypeVS" %>
<asset:javascript src="utilsVS.js"/>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">

<polymer-element name="receipt-votevs-canceller" attributes="receipt smimeMessage isClientToolConnected">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
        </style>
        <div layout vertical style="margin: 10px auto; max-width:1000px;">
            <div class="pageHeader"  layout horizontal center center-justified>
                <h3><g:message code="voteVSCancellerReceipt"/></h3>
            </div>
            <template if="{{messageToUser}}">
                <div  layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </template>
            <div><b><g:message code="hashAccessRequestLbl"/>: </b>{{receipt.hashAccessRequestBase64}}</div>
            <div><b><g:message code="originHashAccessRequestLbl"/>: </b>{{receipt.originHashAccessRequest}}</div>
            <div><b><g:message code="hashCertVSLbl"/>: </b>{{receipt.hashCertVSBase64}}</div>
            <div><b><g:message code="originHashCertVote"/>: </b>{{receipt.originHashCertVote}}</div>

            <template if="{{isClientToolConnected}}">
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div style="margin:10px 0px 10px 0px;">
                        <votingsystem-button on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-certificate" style="margin:0 7px 0 3px;"></i>  <g:message code="checkReceiptLbl"/>
                        </votingsystem-button>
                    </div>
                    <div flex></div>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer('receipt-votevs-canceller', {
            publish: {
                receipt: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            ready: function() {
                console.log(this.tagName + " - ready - " + document.querySelector("#voting_system_page"))
            },
            attached: function () {
                console.log(this.tagName + " - attached")
                this.fire('attached', null);
            },
            receiptChanged:function() {
                this.messageToUser = null
                if('${TypeVS.CANCEL_VOTE.toString()}' != this.receipt.operation )
                    this.messageToUser = '<g:message code="receiptTypeErrorMsg"/>' + " - " + this.receipt.operation
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