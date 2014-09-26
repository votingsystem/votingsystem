<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">


<polymer-element name="votevs-result-dialog" attributes="opened url">
    <template>
        <votingsystem-dialog id="xDialog" class="dialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <g:include view="/include/styles.gsp"/>
            <style no-shim>
            .dialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 500px;
            }
            </style>
            <core-signals on-core-signal-messagedialog-accept="{{cancellationConfirmed}}"
                          on-core-signal-messagedialog-closed="{{confirmDialogClosed}}"></core-signals>
            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;display:{{caption? 'block':'none'}}">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <votingsystem-html-echo html="{{message}}"></votingsystem-html-echo>
                </div>
                <template if="{{messageType == 'VOTE_RESULT'}}">
                    <p style="text-align: center; display:{{optionSelected == null? 'none':'block'}}">
                        <g:message code="confirmOptionDialogMsg"/>:<br>
                        <div style="font-size: 1.2em; text-align: center;"><b>{{optionSelected}}</b></div>
                    </p>
                    <template if="{{statusCode == 200}}">
                        <div layout horizontal style="margin:0px 20px 0px 0px;">
                            <div style="margin:10px 0px 10px 0px;">
                                <votingsystem-button on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                                    <i class="fa fa-certificate" style="margin:0 7px 0 3px;"></i>  <g:message code="checkReceiptLbl"/>
                                </votingsystem-button>
                            </div>
                            <div flex></div>
                            <div style="margin:10px 0px 10px 0px;">
                                <votingsystem-button on-click="{{cancelVote}}" style="margin: 0px 0px 0px 5px;">
                                    <i class="fa fa-times" style="margin:0 10px 0 0;"></i> <g:message code="cancelVoteLbl"/>
                                </votingsystem-button>
                            </div>
                        </div>
                    </template>
                </template>
                <template if="{{messageType == 'VOTE_CANCELLATION_RESULT'}}">
                    <template if="{{statusCode == 200}}">
                        <div layout horizontal style="margin:0px 20px 0px 0px;">
                            <div style="margin:10px 0px 10px 0px;">
                                <votingsystem-button on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                                    <i class="fa fa-certificate" style="margin:0 7px 0 3px;"></i>  <g:message code="checkReceiptLbl"/>
                                </votingsystem-button>
                            </div>
                            <div flex></div>
                        </div>
                    </template>
                </template>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('votevs-result-dialog', {
            votevsURL:null,
            votevsReceipt:null,
            hashCertVSHex:null,
            hashCertVSBase64:null,
            statusCode:null,
            messageType:null,
            appMessageJSON:null,
            callerCallback:null,
            voteVSCancellationReceipt:null,
            ready: function() {},
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.$.xDialog.opened = this.opened
                if(this.opened == false) this.close()
            },
            show: function(appMessageJSON) {
                this.appMessageJSON = appMessageJSON
                this.statusCode = appMessageJSON.statusCode
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.caption = "<g:message code='voteOKCaption'/>"
                    this.message = "<g:message code="voteResultOKMsg"/>"
                    //                responseJSON.put("voteURL", ((List<String>)responseVS.getData()).iterator().next());
                    //responseJSON.put("hashCertVSHex", new String(Hex.encode(eventVS.getVoteVS().getHashCertVSBase64().getBytes())));
                    this.votevsURL = appMessageJSON.message
                    this.optionSelected = appMessageJSON.optionSelected
                    this.hashCertVSHex = appMessageJSON.hashCertVSHex
                    this.votevsReceipt = appMessageJSON.voteVSReceipt
                    this.hashCertVSBase64 = appMessageJSON.hashCertVSBase64
                } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
                    this.caption = '<g:message code="voteERRORCaption"/>'
                    var msgTemplate =  "<g:message code='accessRequestRepeatedMsg'/>"
                    this.message = msgTemplate.format(appMessageJSON.eventVS.subject, appMessageJSON.message);
                } else {
                    this.caption = '<g:message code="voteERRORCaption"/>'
                    this.message = appMessageJSON.message
                }
                this.messageType = "VOTE_RESULT"
                this.opened = true
            },
            cancelVote: function() {
                this.callerCallback = Math.random().toString(36).substring(7)
                showMessageVS('<g:message code="cancelVoteConfirmMsg"/>', '<g:message code="cancelVoteLbl"/>', this.callerCallback, true)
                this.opened = false
            },
            confirmDialogClosed: function(e) {
                console.log("confirmDialogClosed - detail: " + e.detail)
                if(e.detail == this.callerCallback) this.show(this.appMessageJSON)
            },
            close: function() {
                this.opened = false
                this.message = null
                this.caption = null
                this.optionSelected = null
                this.votevsURL = null
                this.votevsReceipt = null
                this.statusCode = null
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_RECEIPT)
                if(this.messageType == 'VOTE_RESULT') webAppMessage.message = this.votevsReceipt
                else if(this.messageType == 'VOTE_CANCELLATION_RESULT') webAppMessage.message = this.voteVSCancellationReceipt
                webAppMessage.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            cancellationConfirmed: function() {
                console.log("cancellationConfirmed")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CANCEL_VOTE)
                webAppMessage.message = this.hashCertVSBase64
                webAppMessage.serviceURL = "${createLink(controller:'voteVSCanceller', absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code="cancelVoteLbl"/>"
                webAppMessage.setCallback(function(appMessage) {
                    console.log("vote cancellation callback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    this.statusCode = appMessageJSON.statusCode
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        this.messageType = "VOTE_CANCELLATION_RESULT"
                        this.voteVSCancellationReceipt = appMessageJSON.message
                        this.message = "<g:message code="voteVSCancellationOKMsg"/>"
                        this.caption =  "<g:message code="voteVSCancellationCaption"/>"
                    } else  showMessageVS(appMessageJSON.message, '<g:message code="voteVSCancellationErrorCaption"/>')
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>
