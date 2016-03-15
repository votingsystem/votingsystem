<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vote-result-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div hidden="{{!caption}}" style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div id="voteResultMessageDiv" style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px;
                        display:block;word-wrap:break-word;">
                </div>
                <div hidden="{{!optionSelected}}">
                    <p style="text-align: center;">
                        ${msg.confirmOptionDialogMsg}:</p>
                    <div style="font-size: 1.2em; text-align: center;"><b>{{optionSelected}}</b></div>
                </div>
                <div hidden="{{!isOK}}">
                    <div class="layout horizontal" style="margin:15px 0 0 0;">
                        <div class="flex"></div>
                        <div style="margin:10px 0px 10px 0px;">
                            <button on-click="checkReceipt">
                                <i class="fa fa-x509Certificate"></i><span>{{checkSignatureButtonMsg}}</span>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'vote-result-dialog',
            properties: {
                voteReceipt:{type:String},
                hashCertVSHex:{type:String},
                hashCertVSBase64:{type:String},
                statusCode:{type:Number},
                messageType:{type:String, observer:'messageTypeChanged'},
                voteCancellationReceipt:{type:String},
                checkSignatureButtonMsg:{type:String, value:'${msg.checkVoteLbl}'}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(appMessageJSON) {
                console.log(this.tagName + " - show")
                this.message = null
                this.caption = null
                this.optionSelected = null
                this.voteReceipt = null
                this.statusCode = appMessageJSON.statusCode
                this.isOK = (this.statusCode === 200)
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.caption = "${msg.voteOKCaption}"
                    this.message = "${msg.voteResultOKMsg}"
                    this.optionSelected = appMessageJSON.optionSelected
                    this.hashCertVSHex = appMessageJSON.hashCertVSHex
                    this.voteReceipt = appMessageJSON.voteReceipt
                    this.hashCertVSBase64 = appMessageJSON.hashCertVSBase64
                    this.checkSignatureButtonMsg = '${msg.checkVoteLbl}'
                }
                this.messageType = "VOTE_RESULT"
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
                d3.select("#voteResultMessageDiv").html(this.message)
            },
            messageTypeChanged: function() {
                this.isOK = (this.statusCode == 200)
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                if(this.messageType == 'VOTE_RESULT') operationVS.message = this.voteReceipt
                else if(this.messageType == 'VOTE_CANCELLATION_RESULT') operationVS.message = this.voteCancellationReceipt
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>
