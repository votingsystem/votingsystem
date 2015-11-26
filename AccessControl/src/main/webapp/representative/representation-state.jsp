<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representation-state">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click style="min-height: 300px;">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.userRepresentativeLbl}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div hidden="{{representativeInfoHidden}}" style="margin:10px 0 10px 0;">
                    <div style="color:#6c0404;text-align: center;">{{representativeFullName}}</div>
                    <div hidden="{{!representationState.representative.description}}"
                            style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center;
                            padding:10px 20px 10px 20px; word-wrap:break-word;">
                        <vs-html-echo html="{{representationState.representative.description}}"></vs-html-echo>
                    </div>
                </div>
                <div style="font-size: 1.2em; color:#ba0011; font-weight: bold; text-align: center;
                    padding:10px 20px 10px 20px; border-bottom: 1px solid #ba0011; border-top: 1px solid #ba0011;">
                    <span>{{representationState.stateMsg}}</span>
                    <div hidden="{{timeInfoHidden}}" class="horizontal layout" style="margin:10px 0 0 0;display: none;font-size: 0.9em">
                        <b>${msg.validFromLbl}:</b> <span>{{getDate(representationState.dateFrom)}}</span>
                        <span style="margin: 0 0 0 20px;"><b>${msg.toLbl}:</b></span>
                        <span>{{getDate(representationState.dateTo)}}</span>
                    </div>
                </div>
                <div style="color:#02227a;text-align: center; margin:10px 0 10px 0;">{{representationState.lastCheckedDateMsg}}</div>
                <div class="layout horizontal" style="margin:0px 20px 0px 0px;">
                    <div class="flex"></div>
                    <button hidden="{{cancellationHidden}}" on-click="cancelAnonymousDelegation">
                        ${msg.cancelAnonymousRepresentationMsg}
                    </button>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'representation-state',
            properties: { },
            ready: function() {
                console.log(this.tagName + " - ")
                if(clientTool !== undefined) {
                    this.cancellationHidden = true
                    this.timeInfoHidden = true
                    var operationVS = new OperationVS(Operation.REPRESENTATIVE_STATE)
                    operationVS.setCallback(function(appMessage) {
                        this.representationState = toJSON(appMessage);
                        if(this.representationState.representative != null) {
                            this.representativeFullName = this.representationState.representative.firstName + " " +
                                    this.representationState.representative.lastName
                        } else this.representativeInfoHidden = true
                        if(this.representationState.state === "WITHOUT_REPRESENTATION") return
                        if(this.representationState.state === "WITH_ANONYMOUS_REPRESENTATION") {
                            this.timeInfoHidden = false
                            this.cancellationHidden = false
                        }
                        this.fire('updated-state');
                    }.bind(this))
                    VotingSystemClient.setMessage(operationVS);
                }
            },
            show: function() {
                this.$.xDialog.opened = true
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            cancelAnonymousDelegation: function() {
                var operationVS = new OperationVS(Operation.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION)
                operationVS.serviceURL = contextURL + "/rest/representative/cancelAnonymousDelegation"
                operationVS.signedMessageSubject = '${msg.cancelAnonymousDelegationMsgSubject}'
                operationVS.setCallback(function(appMessage) { this.cancelationResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            cancelationResponse: function(appMessage) {
                console.log(this.tagName + "cancelationResponse - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                var caption = '${msg.operationERRORCaption}'
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "${msg.operationOKCaption}"
                }
                showMessageVS(appMessageJSON.message, caption)
                this.click() //hack to refresh
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</dom-module>
