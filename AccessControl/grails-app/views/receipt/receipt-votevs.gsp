<%@ page import="org.votingsystem.model.TypeVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">

<polymer-element name="receipt-votevs" attributes="receipt smimeMessage isClientToolConnected">
    <template>
        <style></style>
        <div layout vertical style="margin: 10px auto; max-width:1000px;">
            <div class="pageHeader"  layout horizontal center center-justified>
                <h3><g:message code="voteVSReceipt"/></h3>
            </div>
            <div style="display:{{messageToUser? 'block':'none'}}">
                <div  layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </div>
            <div><b><g:message code="eventVSLbl"/>: </b><a href="{{receipt.eventURL}}">{{receipt.eventURL}}</a></div>
            <div><b><g:message code="optionSelectedLbl"/>: </b>{{receipt.optionSelected.content}}</div>

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
        Polymer('receipt-votevs', {
            publish: {
                receipt: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            attached: function () {
                console.log(this.tagName + " - attached")
                this.fire('attached', null);
            },
            receiptChanged:function() {
                this.messageToUser = null
                if('${TypeVS.SEND_SMIME_VOTE.toString()}' != this.receipt.operation )
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