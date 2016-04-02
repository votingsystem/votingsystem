<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="eventvs-admin-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div id="container" style="overflow-y: auto; width:450px; padding:10px;">
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:0px 0px 0px 30px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.cancelEventCaption}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>
                    <p style="text-align: center;">${msg.adminDocumenInfoMsg}</p>
                    ${msg.documentStateSelectionMsg}:<br/>
                    <div style="font-size: 1.1em; margin:10px 0 0 10px;">
                        <div style="margin:10px 0 0 0">
                            <label>
                                <input type="radio" name="optionsRadios" id="selectCloseDocument" value="" checked="checked">
                                ${msg.selectCloseDocumentMsg}
                            </label>
                        </div>
                        <div style="margin:10px 0 0 0">
                            <label>
                                <input type="radio" name="optionsRadios" id="selectDeleteDocument" value="">
                                ${msg.selectDeleteDocumentMsg}
                            </label>
                        </div>
                    </div>
                </div>
                <div class="layout horizontal" style="margin:10px 20px 0px 0px; margin:10px;">
                    <div class="flex"></div>
                    <button on-click="submitForm" style="margin: 0px 0px 0px 5px; font-size: 1.1em;">
                        <i class="fa fa-check"></i> ${msg.acceptLbl}
                    </button>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'eventvs-admin-dialog',
            properties: {
                eventvs:{type:Object, value:{}}
            },
            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
            },
            submitForm: function() {
                var state
                var messageSubject
                if(this.$.selectDeleteDocument.checked) {
                    state = EventVS.State.DELETED_FROM_SYSTEM
                    messageSubject = '${msg.deleteEventVSMsgSubject}'
                } else if(this.$.selectCloseDocument.checked) {
                    state = EventVS.State.CANCELED
                    messageSubject = '${msg.cancelEventVSMsgSubject}'
                }
                var operationVS = new OperationVS(Operation.EVENT_CANCELLATION)
                operationVS.serviceURL= vs.contextURL + "/rest/eventElection/cancel"
                var signedContent = {operation:Operation.EVENT_CANCELLATION,
                    accessControlURL:vs.contextURL, eventId:Number(this.eventvs.id), state:state}
                operationVS.jsonStr = JSON.stringify(signedContent)
                operationVS.subject = messageSubject
                operationVS.setCallback(function(socketMessage) { this.cancelationResponse(socketMessage)}.bind(this))
                vs.client.processOperation(operationVS);
                console.log("currentOperationCode: ", this.currentOperationCode)
            },
            cancelationResponse:function(socketMessage) {
                console.log("cancelationResponse");
                if(ResponseVS.SC_OK == socketMessage.statusCode) {
                    caption = "${msg.operationOKCaption}"
                    msg = "${msg.documentCancellationOKMsg}".format(this.eventvs.subject);
                    if(this.$.selectDeleteDocument.checked) page("/eventElection")
                    else page("/rest/eventElection/id/" + this.eventvs.id)
                } else {
                    caption = "${msg.operationERRORCaption}"
                    msg = socketMessage.message
                }
                this.close()
                this.$.vsSocket.closeQRDialog()
                alert(msg, caption)
            },
            show: function() {
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>
