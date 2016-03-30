<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-select-anonymous-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.4em; margin:0px auto;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.saveAsRepresentativeLbl}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div hidden="{{!infoRequestStep}}" class="vertical layout center center-justified" style="margin:10px 0;">
                    <label>${msg.numWeeksAnonymousDelegationMsg}</label>
                    <input type="number" id="numWeeksAnonymousDelegation" min="1" value="" max="52" required
                           style="width:120px;margin:10px 20px 0px 7px;" class="form-control"
                           title="${msg.numWeeksAnonymousDelegationMsg}">
                </div>
                <div hidden="{{!confirmStep}}" vertical layout center center-justified>
                    <div id="delegationMsg" style="margin: 20px 0 10px 10px;"></div>
                    <div id="anonymousDelegationMsg" style="margin:25px 0 25px 0;"></div>
                </div>
                <div hidden="{{!responseStep}}">
                    <p style="text-align: center; font-size: 1.2em;">
                        <span>{{anonymousDelegationResponseMsg}}</span>
                    </p>
                    <p style="text-align: center; font-size: 1.2em;">
                        ${msg.anonymousDelegationReceiptMsg}
                    </p>
                </div>
                <div hidden="{{responseStep}}">
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
            is:'representative-select-anonymous-dialog',
            properties: {
                representative:{type:Object},
                infoRequestStep:{type:Boolean, value:true},
                confirmStep:{type:Boolean, value:false},
                responseStep:{type:Boolean, value:false},
                anonymousLbl:{type:String, value:"${msg.anonymousLbl}"},
                anonymousDelegationMsg:{type:String, value:"${msg.anonymousLbl}", observer:'anonymousDelegationMsgChanged'},
                anonymousDelegationResponseMsg:{type:String},
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            anonymousDelegationMsgChanged: function() {
                this.$.anonymousDelegationMsg.innerHTML = this.anonymousDelegationMsg
            },
            show: function(representative) {
                this.representative = representative
                this.infoRequestStep = true
                this.confirmStep = false
                this.$.numWeeksAnonymousDelegation.value = ""
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            accept: function() {
                console.log("accept")
                if(this.confirmStep) {
                    if(!this.$.numWeeksAnonymousDelegation.validity.valid) {
                        this.messageToUser = '${msg.numberFieldLbl}'
                        return
                    }
                    var operationVS = new OperationVS(Operation.ANONYMOUS_REPRESENTATIVE_SELECTION)
                    operationVS.jsonStr = JSON.stringify({operation:Operation.ANONYMOUS_REPRESENTATIVE_SELECTION,
                            representative:this.representative,
                            weeksOperationActive:this.$.numWeeksAnonymousDelegation.value})
                    operationVS.subject = '${msg.representativeDelegationMsgSubject}'
                    operationVS.setCallback(function(appMessage) { this.delegationResponse(appMessage) }.bind(this))
                    VotingSystemClient.setMessage(operationVS);
                } else {
                    this.messageToUser = null
                    var msgTemplate = "${msg.selectRepresentativeConfirmMsg}";
                    if(!this.$.numWeeksAnonymousDelegation.validity.valid) {
                        alert('${msg.numWeeksAnonymousDelegationMsg}', '${msg.errorLbl}')
                        return
                    }
                    var weeksMsgTemplate = "${msg.numWeeksResultAnonymousDelegationMsg}";
                    this.$.delegationMsg.innerHTML = msgTemplate.format(this.anonymousLbl, this.representative.name)
                    this.anonymousDelegationMsg = weeksMsgTemplate.format(this.anonymousLbl, this.$.numWeeksAnonymousDelegation.value)
                    this.confirmStep = true
                    this.infoRequestStep = false
                }
            },
            delegationResponse:function(appMessageJSON) {
                console.log(this.tagName + " - delegationResponse");
                var caption = '${msg.operationERRORCaption}'
                var msg = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.responseStep = true
                    this.infoRequestStep = false
                    this.confirmStep = false
                    this.anonymousDelegationResponseMsg = "${msg.selectedRepresentativeMsg}".format(this.representativeFullName)
                    return
                } else if (ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
                    caption = "${msg.anonymousDelegationActiveErrorCaption}"
                    msg = appMessageJSON.message + "<br/>" +
                            "${msg.downloadReceiptMsg}".format(appMessageJSON.URL)
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
