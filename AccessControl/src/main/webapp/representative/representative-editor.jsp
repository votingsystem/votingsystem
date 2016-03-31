<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-editor/vs-editor.html" rel="import"/>

<dom-module name="representative-editor">
    <style>
        .representativeNameHeader { font-size: 1.3em; text-overflow: ellipsis; color:#6c0404; padding: 0 40px 0 40px; text-align: center;}
    </style>
    <template>
        <div class="pagevs">
            <div style="margin: 0 10px 10px 0;">${msg.newRepresentativeAdviceMsg1}</div>
            <div style="margin: 0 10px 10px 0;">${msg.newRepresentativeAdviceMsg2}</div>
            <div style="margin: 0 10px 10px 0;">${msg.newRepresentativeAdviceMsg3}</div>
            <div class="flex representativeNameHeader">
                <div>{{representativeFullName}}</div>
            </div>
            <div>
                <vs-editor id="editor"></vs-editor>
            </div>
            <div class="horizontal layout center">
                <div>
                    <div style="font-size: 0.8em;">${msg.selectRepresentativeImgLbl}</div>
                    <input type="file" id="imageFile" accept=".jpg, .png, .jpeg, .gif|image/*/">
                </div>
                <div class="flex"></div>
                <a class="buttonvs" on-click="submitForm">
                    <i class="fa fa-check"></i> ${msg.acceptLbl}
                </a>
            </div>
            <div id="holder" style="width: 250px;height: 250px;"></div>
        </div>
    </template>
    <script>
        Polymer({
            is:'representative-editor',
            properties:{
                representativeImageMaxSize: { type: Number, value: 1024*1024 },
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.tomorrow = new Date().getTime() + 24 * 60 * 60 * 1000;
                this.optionList = []
                this._validFileExtensions = [".jpg", ".jpeg", "png"];
                if (window.File && window.FileReader && window.FileList && window.Blob) {
                } else {
                    alert('The File APIs are not fully supported in this browser.');
                }
                this.$.imageFile.addEventListener('change', this.handleFileSelect.bind(this), false);
            },
            attached: function() {
                if(vs.representative) {
                    this.representative = toJSON(JSON.stringify(vs.representative))
                    vs.representative = null
                    this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                    sendSignalVS({caption:"${msg.editRepresentativeLbl}"})
                    this.subject = "${msg.editRepresentativeLbl}"
                    this.$.editor.setContent(window.atob(this.representative.description))
                } else {
                    this.representativeFullName = null;
                    this.$.editor.setContent("")
                    sendSignalVS({caption:"${msg.newRepresentativeLbl}"})
                    this.subject = "${msg.newRepresentativeLbl}"
                }
            },
            submitForm: function() {
                if(!this.selectedFileBase64) {
                    alert("${msg.selectRepresentativeImgLbl}", "${msg.errorLbl}")
                    return;
                }
                var operationVS = new OperationVS(Operation.EDIT_REPRESENTATIVE)
                operationVS.serviceURL = vs.contextURL + "/rest/representative/save"
                operationVS.subject = this.subject
                var description = window.btoa(this.$.editor.getContent())
                operationVS.jsonStr = JSON.stringify({description:description, base64Image:this.selectedFileBase64})
                operationVS.setCallback(function(socketMessage) {
                    if(ResponseVS.SC_OK === socketMessage.statusCode) {
                        var msgDto = toJSON(socketMessage.message)
                        page(msgDto.url)
                        this.resetForm()
                    } else alert(socketMessage.message, "${msg.errorLbl}")
                }.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            loadFile:function(file) {
                this.className = '';
                var reader = new FileReader();
                reader.onload = function (event) {
                    console.log(event.target);
                    holder.style.background = 'url(' + event.target.result + ') no-repeat center';
                };
                console.log(file);
                reader.readAsDataURL(file);
            },
            resetForm:function() {
                if(this.$.editor.editorLoaded === true) this.$.editor.setContentText("")
                this.$.holder.innerHTML = ""
                this.selectedFileBase64 = null
            },
            handleFileSelect:function(evt) {
                var files = evt.target.files; // FileList object
                if(files[0].size > this.representativeImageMaxSize) {
                    this.$.imageFile.value = ""
                    evt.target.files = []
                    var messageTemplate = "${msg.fileSizeExceededMsg}"
                    alert(messageTemplate.format(this.representativeImageMaxSize /1024 + "KB"), "${msg.errorLbl}")
                    this.$.holder.innerHTML = ""
                    this.selectedFileBase64 = null
                } else {
                    this.selectedFile = files[0]
                    var description = escape(this.selectedFile.name) + " - type: " + this.selectedFile.type +
                            " - size: " + this.selectedFile.size + ' - last modified: ' +
                            (this.selectedFile.lastModifiedDate ? this.selectedFile.lastModifiedDate.toLocaleDateString() : 'n/a')
                    console.log("selected file: " + description)
                    var reader = new FileReader();
                    var holder = this.$.holder
                    reader.onload = function (event) {
                        console.log(event.target);
                        holder.style.background = 'url(' + event.target.result + ') no-repeat center';
                        this.selectedFileBase64 = event.target.result.split(",")[1]
                    }.bind(this);
                    reader.readAsDataURL(this.selectedFile);
                }
            }
        });
    </script>
</dom-module>