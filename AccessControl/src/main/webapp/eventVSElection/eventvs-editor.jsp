<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/vs-editor.vsp" rel="import"/>
<link href="../resources/bower_components/vs-datepicker/vs-datepicker.html" rel="import"/>
<link href="../element/reason-dialog.vsp" rel="import"/>

<dom-module name="eventvs-editor">
    <template>
        <div class="horizontal layout center center-justified">
            <div style="max-width: 1000px; width: 100%;">
                <div class="horizontal layout center center-justified" style="margin: 0 10px 10px 0;">
                    <input type="text" id="subject" class="form-control" required
                           title="${msg.electionSubjectLbl}" placeholder="${msg.electionSubjectLbl}"/>
                    <vs-datepicker style="margin:0 0 0 10px;" id="datePicker" years-back="0" years-fwd="0"
                                   month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]'
                                   caption="${msg.dateBeginLbl}"> </vs-datepicker>
                </div>
                <vs-editor id="editor"></vs-editor>
                <div>
                    <button on-click="addOption">${msg.missingOptionsErrorMsg}</button>
                </div>
                <template is="dom-repeat" items="{{optionList}}">
                    <div class="horizontal layout center" style="margin: 10px 0 0 0;">
                        <div class="numVotesClass" style="margin:0 10px 0 0;">
                            <button on-click="removeOption"><i class="fa fa-times"></i> ${msg.deleteLbl}</button>
                        </div>
                        <div class="voteOption" style="font-size: 2em; font-weight: bold;">
                            {{item}}
                        </div>
                    </div>
                </template>
                <div class="horizontal layout">
                    <div class="flex"></div>
                    <button on-click="submitForm"><i class="fa fa-check"></i> ${msg.acceptLbl}</button>
                </div>
            </div>
        </div>
        <reason-dialog id="reasonDialog" caption="${msg.enterOptionLbl}"></reason-dialog>
    </template>
    <script>
        Polymer({
            is:'eventvs-editor',
            properties:{

            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.tomorrow = new Date().getTime() + 24 * 60 * 60 * 1000;
                this.$.datePicker.setDate(new Date(this.tomorrow))
                this.optionList = []
                document.querySelector("#voting_system_page").addEventListener('on-submit-reason',
                        function(e) {
                            this.optionList.push(e.detail)
                            this.optionList = this.optionList.slice(0) //to make changes visible to template
                            console.table(this.optionList)
                        }.bind(this))
            },
            submitForm: function() {
                var msgTemplate = "${msg.enterFieldMsg}"
                if(FormUtils.checkIfEmpty(this.$.subject.value)) {
                    alert(msgTemplate.format("${msg.electionSubjectLbl}"), "${msg.errorLbl}")
                    return
                }
                if(this.tomorrow > this.$.datePicker.getDate()) {
                    alert("${msg.dateBeforeCurrentErrorMsg}", "${msg.errorLbl}")
                    return
                }
                if(this.optionList.length < 2) {
                    alert("${msg.missingOptionsErrorMsg}", "${msg.errorLbl}")
                    return
                }
                var operationVS = new OperationVS(Operation.PUBLISH_EVENT)
                operationVS.serviceURL = contextURL + "/rest/eventVSElection"
                operationVS.signedMessageSubject = "${msg.publishVoteLbl}"
                var content = window.btoa(encodeURIComponent( escape(this.$.editor.getContent())))
                var fieldsEventVS = []
                this.optionList.forEach(function(option) {
                    fieldsEventVS.push({content:option})
                })
                operationVS.jsonStr = JSON.stringify({subject:this.$.subject.value, content:content,
                    fieldsEventVS:fieldsEventVS, dateBegin:this.$.datePicker.getDate().getTime(),
                    UUID: "#{spa.getUUID()}"})
                console.log(JSON.stringify(operationVS))
                VotingSystemClient.setMessage(operationVS);
            },
            removeOption: function(e) {
                var index = this.optionList.indexOf(e.model.item);
                this.optionList.splice(index, 1);
                this.optionList = this.optionList.slice(0) //to make changes visible to template
            },
            addOption: function() {
                this.$.reasonDialog.show()
            }
        });
    </script>
</dom-module>