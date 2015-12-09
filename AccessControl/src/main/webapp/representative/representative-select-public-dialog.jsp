<%@ page contentType="text/html; charset=UTF-8" %>


<dom-module name="representative-select-public-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:0px auto;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.saveAsRepresentativeLbl}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>

                <div class="vertical layout center center-justified" style="margin: 20px 0 10px 10px;">
                    <div id="delegationMsg"></div>
                </div>

                <div>
                    <div class="layout horizontal" style="margin:0px 20px 0px 0px;  font-size:1.2em;">
                        <div class="flex"></div>
                        <div style="margin:10px 0px 10px 0px;">
                            <button on-click="accept" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-check"></i> ${msg.acceptLbl}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'representative-select-public-dialog',
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(representative) {
                this.representative = representative
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                var msgTemplate = "${msg.selectRepresentativeConfirmMsg}";
                d3.select(this).select("#delegationMsg").html(msgTemplate.format("${msg.publicLbl}", this.representative.name))
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            accept: function() {
                var operationVS = new OperationVS(Operation.REPRESENTATIVE_SELECTION)
                operationVS.jsonStr = JSON.stringify({operation:Operation.REPRESENTATIVE_SELECTION,
                        representative:this.representative})
                operationVS.signedMessageSubject = '${msg.representativeDelegationMsgSubject}'
                operationVS.serviceURL = '${contextURL}/rest/representative/delegation'
                operationVS.setCallback(function(appMessage) { this.delegationResponse(appMessage) }.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            delegationResponse:function(appMessage) {
                console.log(this.tagName + " - delegationResponse - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                var caption = '${msg.operationERRORCaption}'
                var msg = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "${msg.operationOKCaption}"
                    msg = "${msg.selectedRepresentativeMsg}".format(this.representativeFullName)
                    this.close()
                } else if (ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
                    caption = "${msg.anonymousDelegationActiveErrorCaption}"
                    msg = appMessageJSON.message + "<br/>" +
                            "${msg.downloadReceiptMsg}".format(appMessageJSON.URL)
                }
                showMessageVS(msg, caption)
                this.click() //hack to refresh screen
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>
