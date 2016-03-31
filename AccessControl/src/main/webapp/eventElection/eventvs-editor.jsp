<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-editor/vs-editor.html" rel="import"/>
<link href="../resources/bower_components/vs-datepicker/vs-datepicker.html" rel="import"/>
<link href="../element/reason-dialog.vsp" rel="import"/>

<dom-module name="eventvs-editor">
    <style>
        :host {
            display: none;
        }
    </style>
    <template>
        <div class="pagevs">
            <div class="horizontal layout center center-justified" style="margin: 0 10px 10px 0;">
                <input type="text" id="subject" class="form-control" required
                       title="${msg.electionSubjectLbl}" placeholder="${msg.electionSubjectLbl}"/>
                <vs-datepicker style="margin:0 0 0 10px;" id="datePicker" years-back="0" years-fwd="0"
                               month-labels='[${msg.monthsShort}]' day-labels='[${msg.weekdaysShort}]'
                               caption="${msg.dateBeginLbl}"> </vs-datepicker>
            </div>
            <vs-editor id="editor"></vs-editor>
            <div style="margin: 15px 0 0 15px;font-size: 0.9em;">
                <a class="buttonvs" on-click="addOption">
                    <i class="fa fa-check"></i> ${msg.missingOptionsErrorMsg}
                </a>
            </div>
            <template is="dom-repeat" items="{{optionList}}">
                <div style="margin: 15px 0 0 25px;font-size: 0.8em;">
                    <a class="buttonvs" on-click="removeOption">
                        <i class="fa fa-times" style="color: #ba0011;"></i> ${msg.deleteLbl}
                    </a>
                    <span style="font-size: 2em; font-weight: bold;margin: 0 0 0 10px;">
                        {{item}}
                    </span>
                </div>
            </template>
            <div class="horizontal layout">
                <div class="flex"></div>
                <div style="margin: 0 15px 0 0;">
                    <a class="buttonvs" on-click="submitForm">
                        <i class="fa fa-check"></i> ${msg.acceptLbl}
                    </a>
                </div>
            </div>
        </div>
        <reason-dialog id="reasonDialog" caption="${msg.enterOptionLbl}"></reason-dialog>
    </template>
    <script>
        Polymer({
            is:'eventvs-editor',
            properties:{
                numMaxCharacters:{type:Number, value:4000},
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.resetForm();
                document.querySelector("#voting_system_page").addEventListener('on-submit-reason',
                        function(e) {
                            this.optionList.push(e.detail)
                            this.optionList = this.optionList.slice(0) //to make changes visible to template
                            console.table(this.optionList)
                        }.bind(this))
                this.$.editor.addEventListener ('tiny-init', function(e) {
                    this.style.display = 'block';
                }.bind(this))
            },
            resetForm: function() {
                this.tomorrow = new Date().getTime() + 24 * 60 * 60 * 1000;
                this.$.datePicker.setDate(new Date(this.tomorrow))
                this.$.subject.value = ""
                if(this.$.editor.editorLoaded === true) this.$.editor.setContentText("")
                this.optionList = []
            },
            submitForm: function() {
                var msgTemplate = "${msg.enterFieldMsg}"
                if(FormUtils.checkIfEmpty(this.$.subject.value)) {
                    alert(msgTemplate.format("${msg.electionSubjectLbl}"), "${msg.errorLbl}")
                    return
                }
                if(new Date() > this.$.datePicker.getDate()) {
                    alert("${msg.dateBeforeCurrentErrorMsg}", "${msg.errorLbl}")
                    return
                }
                if(this.optionList.length < 2) {
                    alert("${msg.missingOptionsErrorMsg}", "${msg.errorLbl}")
                    return
                }
                var editorContent = this.$.editor.getContent()
                console.log("editorContent.length: " + editorContent.length, " - numMaxCharacters: " + this.numMaxCharacters)
                if(editorContent.length > this.numMaxCharacters)  {
                    this.searchResultMsg =
                    alert("${msg.documentSizeExceededMsg}".format(this.numMaxCharacters), "${msg.errorLbl}")
                    return
                }
                var operationVS = new OperationVS(Operation.PUBLISH_EVENT)
                operationVS.serviceURL = vs.contextURL + "/rest/eventElection"
                operationVS.subject = "${msg.publishVoteLbl}"
                var content = window.btoa(this.$.editor.getContent())
                operationVS.setCallback(function(socketMessage) { this.processSocketMessage(socketMessage)}.bind(this))
                var fieldsEventVS = []
                this.optionList.forEach(function(option) {
                    fieldsEventVS.push({content:option})
                })
                operationVS.eventVS = {subject:this.$.subject.value, content:content,
                    fieldsEventVS:fieldsEventVS, dateBegin:this.$.datePicker.getDate().getTime()}
                console.log(" -- operation: ", operationVS)
                VotingSystemClient.setMessage(operationVS);
            },
            removeOption: function(e) {
                var index = this.optionList.indexOf(e.model.item);
                this.optionList.splice(index, 1);
                this.optionList = this.optionList.slice(0) //to make changes visible to template
            },
            addOption: function() {
                this.$.reasonDialog.show()
            },
            processSocketMessage: function(socketMessage) {
                console.log(this.tagName + " - processSocketMessage: ", socketMessage)
                switch (socketMessage.messageType) {
                    case "OPERATION_RESULT":
                        if(ResponseVS.SC_OK === socketMessage.statusCode) {
                            var msgDto = toJSON(socketMessage.message)
                            page(msgDto.url)
                            this.resetForm();
                        } else alert(socketMessage.message, "${msg.errorLbl}")
                        break;
                }
                vs.socketElement.closeQRDialog()
            }
        });
    </script>
</dom-module>