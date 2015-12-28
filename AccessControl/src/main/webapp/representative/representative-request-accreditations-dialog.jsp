<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-request-accreditations-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            ${msg.requestRepresentativeAcreditationsLbl}
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>${msg.accreditationRequestMsg}</div>
                <div class="horizontal layout center center-justified" style="margin:10px 0 10px 0;">
                    <div style="margin: 0 20px 0 0;">
                        <vs-datepicker style="margin:0 0 0 10px;" id="datePicker" years-back="0" years-fwd="0"
                                       month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]'
                                       caption="${msg.dateLbl}"> </vs-datepicker>
                    </div>
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
            is:'representative-request-accreditations-dialog',
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(representative) {
                this.representative = representative
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                this.$.email.value = ""
                this.$.datePicker.reset()
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            submitForm: function() {
                var msgTemplate = "${msg.enterFieldMsg}"
                if (!this.$.email.validity.valid) {
                    alert(msgTemplate.format("${msg.emailLbl}"), "${msg.errorLbl}")
                    return
                }
                if (!this.$.datePicker.getDate()) {
                    alert(msgTemplate.format("${msg.dateLbl}"), "${msg.errorLbl}")
                    return
                }
                var operationVS = new OperationVS(Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
                operationVS.jsonStr = JSON.stringify({operation:Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST,
                    representativeNif:this.representative.nif, representativeName:this.representativeFullName,
                    email:this.$.email.value,
                    selectedDate:this.$.datePicker.getDate().getTime(),
                    UUID:Math.random().toString(36).substring(7)})
                operationVS.serviceURL = contextURL + "/rest/representative/accreditations"
                operationVS.signedMessageSubject = '${msg.requestRepresentativeAcreditationsLbl}'

                console.log(JSON.stringify(operationVS))

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