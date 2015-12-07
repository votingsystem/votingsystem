<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-request-votinghistory-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            ${msg.requestVotingHistoryLbl}
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>${msg.representativeHistoryRequestMsg}</div>
                <div class="layout horizontal">
                    <div class="flex"></div>
                    <div>
                        <button on-click="submit">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'representative-request-votinghistory-dialog',
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(representative) {
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
                this.representative = representative
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
            },
            submit: function() {
                var operationVS = new OperationVS(Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST)
                operationVS.jsonStr = JSON.stringify({operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
                    representativeNif:this.representative.nif, representativeName:this.representativeFullName})
                operationVS.serviceURL = contextURL + "/rest/representative/history"
                operationVS.signedMessageSubject = '${msg.requestVotingHistoryLbl}'
                VotingSystemClient.setMessage(operationVS);
                this.close()
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>