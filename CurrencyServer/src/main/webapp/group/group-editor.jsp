<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-editor/vs-editor.html" rel="import"/>
<link href="../tagVS/tagvs-select-dialog.vsp" rel="import"/>

<dom-module name="group-editor">
    <template>
        <div class="horizontal layout center center-justified">
            <div style="max-width: 1000px; width: 100%;">
                <div class="horizontal layout center center-justified" style="margin: 0 10px 10px 0;">
                    <input type="text" id="subject" class="form-control" required
                           title="${msg.newGroupNameLbl}" placeholder="${msg.newGroupNameLbl}"/>
                </div>
                <div>
                    <vs-editor id="editor"></vs-editor>
                </div>
                <div class="horizontal layout">
                    <template is="dom-repeat" items="{{selectedTags}}" as="tag">
                        <div hidden="{{tagDivHidden}}" class="horizontal layout center center-justified">
                            <div style="color: #888; margin: 0 15px;">
                                <i class="fa fa-tag"></i> ${msg.tagLbl}
                            </div>
                            <div class="horizontal layout" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                <div hidden="{{removeTagIconHidden}}"  data-tag-id$='{{tag.id}}' on-click="removeTag">
                                    <i class="fa fa-minus" style="color: #6c0404; cursor: pointer;"></i>
                                </div>
                                <div>{{tag.name}}</div>
                            </div>
                        </div>
                    </template>
                    <div hidden="{{tagButtonHidden}}" style="margin:10px 0px 10px 10px;">
                        <button on-click="showTagDialog">
                            <i class="fa fa-tag"></i> ${msg.addTagLbl}
                        </button>
                    </div>
                    <div class="flex"></div>
                    <div style="margin:10px 0px 10px 10px;">
                        <button on-click="submitForm"><i class="fa fa-check"></i> ${msg.acceptLbl}</button>
                    </div>
                </div>
            </div>
        </div>
        <tagvs-select-dialog id="tagDialog" caption="${msg.addTagDialogCaption}"></tagvs-select-dialog>
    </template>
    <script>
        Polymer({
            is:'group-editor',
            properties: {
                selectedTags: {object:Array, value:[], observer:'selectedTagsChanged'},
                tagButtonHidden: {object:Boolean, value:false}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.selectedTags = []
                this.$.tagDialog.addEventListener('tag-selected', function (e) {
                    console.log("tag-selected: " + JSON.stringify(e.detail))
                    this.selectedTags = e.detail
                }.bind(this))
            },
            attached:function() {
                if(vs.group) {
                    this.$.subject.readOnly = true;
                    this.operationVS = Operation.CURRENCY_GROUP_EDIT
                    this.group = toJSON(JSON.stringify(vs.group))
                    this.groupId = this.group.id
                    this.$.subject.value = this.group.name
                    this.$.editor.setContent(window.atob(this.group.description))
                    this.signedMessageSubject = "${msg.editGroupLbl}"
                    if(this.group.tags.length > 0) {
                        this.selectedTags = [this.group.tags[0]]
                        this.removeTagIconHidden = true;
                        this.tagDivHidden = false;
                    } else this.tagDivHidden = true;
                    this.tagButtonHidden = true
                } else {
                    this.$.subject.readOnly = false;
                    this.removeTagIconHidden = false;
                    this.signedMessageSubject = "${msg.newGroupLbl}"
                    this.selectedTags = []
                    this.tagDivHidden = false;
                    this.group = null
                    this.operationVS = Operation.CURRENCY_GROUP_NEW
                    this.tagButtonHidden = false
                }
                sendSignalVS({caption:this.signedMessageSubject})
            },
            showTagDialog: function() {
                this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
            },
            selectedTagsChanged: function(e) {
                console.log(this.tagName + " - selectedTagsChanged - num. tags: " + this.selectedTags.length)
                this.tagButtonHidden = (this.selectedTags.length > 0)
            },
            removeTag: function(e) {
                var tagToDelete = e.model.tag
                for (tagIdx in this.selectedTags) {
                    if (tagToDelete.id == this.selectedTags[tagIdx].id) {
                        this.selectedTags.splice(tagIdx, 1)
                    }
                }
                this.selectedTags = this.selectedTags.slice(0); //hack to notify array changes
            },
            submitForm: function() {
                var msgTemplate = "${msg.enterFieldMsg}"
                if(FormUtils.checkIfEmpty(this.$.subject.value)) {
                    alert(msgTemplate.format("${msg.electionSubjectLbl}"), "${msg.errorLbl}")
                    return
                }
                var operationVS = new OperationVS(this.operationVS)
                operationVS.serviceURL = vs.contextURL + "/rest/group/saveGroup"
                operationVS.signedMessageSubject = this.signedMessageSubject
                var description = window.btoa(this.$.editor.getContent())
                operationVS.jsonStr = JSON.stringify({operation:this.operationVS , id:this.groupId,
                    name:this.$.subject.value, description:description, tags: this.selectedTags,
                    UUID: "${spa.getUUID()}"})
                VotingSystemClient.setMessage(operationVS);
            }
        })
    </script>

</dom-module>