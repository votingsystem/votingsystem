<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-datepicker/vs-datepicker.html" rel="import"/>

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
                <div class="horizontal layout center center-justified">
                    <div>
                        <vs-datepicker style="margin:0 0 0 10px;" id="dateFromPicker" years-back="0" years-fwd="0"
                                       month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]'
                                       caption="${msg.dateBeginLbl}"> </vs-datepicker>
                    </div>
                    <div>
                        <vs-datepicker style="margin:0 0 0 10px;" id="dateToPicker" years-back="0" years-fwd="0"
                                       month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]'
                                       caption="${msg.dateFinishLbl}"> </vs-datepicker>
                    </div>
                </div>
                <div class="horizontal layout center center-justified" style="margin:10px 0 10px 0;">
                    <input id="email" type="email" name="email" title="${msg.emailLbl}" placeholder="${msg.emailLbl}" required>
                </div>
                <div class="layout horizontal">
                    <div class="flex"></div>
                    <div>
                        <button on-click="submitForm">
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
                this.$.email.onkeypress = function(event){
                    if (event.keyCode == 13) this.submitForm()
                }.bind(this)
            },
            show: function(representative) {
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
                this.$.email.value = ""
                this.$.dateFromPicker.reset()
                this.$.dateToPicker.reset()
                this.representative = representative
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
            },
            submitForm: function() {
                var msgTemplate = "${msg.enterFieldMsg}"
                if (!this.$.email.validity.valid) {
                    alert(msgTemplate.format("${msg.emailLbl}"), "${msg.errorLbl}")
                    return
                }
                if(this.$.dateFromPicker.getDate() >= this.$.dateToPicker.getDate()) {
                    alert("${msg.dateRangeShortErrorMsg}", "${msg.errorLbl}")
                    return
                }
                var operationVS = new OperationVS(Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST)
                operationVS.jsonStr = JSON.stringify({operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
                    representativeNif:this.representative.nif, representativeName:this.representativeFullName,
                    email:this.$.email.value,
                    dateFrom:this.$.dateFromPicker.getDate().getTime(),
                    dateTo:this.$.dateToPicker.getDate().getTime(),
                    UUID:"#{spa.getUUID()}"})
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