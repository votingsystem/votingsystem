<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-editor/vs-editor.html" rel="import"/>
<link href="../tagVS/tagvs-select-dialog.vsp" rel="import"/>

<dom-module name="groupvs-editor">
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
                        <div>
                            <div style="color: #888;">
                                <i class="fa fa-tag"></i> ${msg.tagLbl}
                            </div>
                            <a class="btn btn-default" data-tag-id$='{{tag.id}}' on-click="removeTag"
                               style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px; cursor: pointer;">
                                <i class="fa fa-minus" style="color: #6c0404;"></i> <span>{{tag.name}}</span></a>
                        </div>
                    </template>
                    <div hidden="{{!selectedTagsHidden}}" style="margin:10px 0px 10px 10px;">
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
            is:'groupvs-editor',
            properties: {
                selectedTags: {object:Array, value:[], observer:'selectedTagsChanged'},
                selectedTagsHidden: {object:Boolean, value:true}
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
                if(vs.groupvs) {
                    this.operationVS = Operation.CURRENCY_GROUP_EDIT
                    this.groupvs = toJSON(JSON.stringify(vs.groupvs))
                    this.$.subject.value = this.groupvs.name
                    this.$.editor.setContent(decodeURIComponent(escape(window.atob(this.groupvs.description))))
                    this.selectedTags.push(this.groupvs.tags[0])
                } else {
                    this.operationVS = Operation.CURRENCY_GROUP_NEW
                }
                vs.groupvs = null
            },
            showTagDialog: function() {
                this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
            },
            selectedTagsChanged: function(e) {
                console.log(this.tagName + " - selectedTagsChanged - num. tags: " + this.selectedTags.length)
                this.selectedTagsHidden = (this.selectedTags.length === 0)
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
                operationVS.serviceURL = contextURL + "/rest/groupVS/saveGroup"
                operationVS.signedMessageSubject = "${msg.newGroupVSLbl}"
                var description = window.btoa(encodeURIComponent( escape(this.$.editor.getContent())))
                //dto.setId(groupId);
                operationVS.jsonStr = JSON.stringify({operation:Operation.CURRENCY_GROUP_NEW ,
                    name:this.$.subject.value, description:description, tags: this.selectedTags,
                    UUID: "${spa.getUUID()}"})
                console.log(JSON.stringify(operationVS))
                VotingSystemClient.setMessage(operationVS);
            }
        })
    </script>

</dom-module>