<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-cancel-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <style>
                .textDialog {
                    font-size: 1.2em; color:#888; font-weight: bold; text-align: center;
                }
            </style>
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            ${msg.removeRepresentativeLbl}
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div class="textDialog" style="padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <p style="text-align: center;">${msg.removeRepresentativeMsg}</p>
                </div>
                <div class="layout horizontal" style="margin:0px 20px 0px 0px;">
                    <div class="flex"></div>
                    <div>
                        <button on-click="accept" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'representative-cancel-dialog',
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function() {
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            accept: function() {
                console.log(this.tagName + " - removeRepresentative")
                var operationVS = new OperationVS(Operation.REPRESENTATIVE_REVOKE)
                operationVS.jsonStr = JSON.stringify({operation:Operation.REPRESENTATIVE_REVOKE, nif:validatedNif})
                operationVS.serviceURL = contextURL + "/rest/representative/revoke"
                operationVS.signedMessageSubject = '${msg.removeRepresentativeMsgSubject}'
                operationVS.setCallback(function(appMessage) { this.revokeResponse(appMessage) }.bind(this))
                VotingSystemClient.setMessage(operationVS);
                this.close()
            },
            revokeResponse: function(appMessageJSON) {
                console.log(this.tagName + "revokeResponse");
                var caption = '${msg.operationERRORCaption}'
                var msg = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "${msg.operationOKCaption}"
                    msg = "${msg.removeRepresentativeOKMsg}";
                } else if (ResponseVS.SC_CANCELED== appMessageJSON.statusCode) {
                    caption = "${msg.operationCANCELEDLbl}"
                }
                alert(msg, caption)
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>
