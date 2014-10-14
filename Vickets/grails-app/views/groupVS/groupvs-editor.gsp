<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">

<polymer-element name="groupvs-editor">
    <template>
        <h3>
            <div id="editGroupHeader" class="pageHeader text-center">
                <votingsystem-html-echo html="{{groupvs.name}}"></votingsystem-html-echo>
            </div>
        </h3>
        <div class="text-left" style="margin:10px 0 10px 0;">
            <ul>
                <li><g:message code="signatureRequiredMsg"/></li>
                <li><g:message code="newGroupVSAdviceMsg2"/></li>
                <li><g:message code="newGroupVSAdviceMsg3"/></li>
            </ul>
        </div>
        <div style="position:relative; width:100%;">
            <votingsystem-texteditor id="textEditor"  dataJSON=""
                             type="pc" style="height:300px; width:100%;">
            </votingsystem-texteditor>
        </div>
        <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
            <div style="position:absolute; right:0;">
                <votingsystem-button on-click="{{submitForm}}">
                    <i class="fa fa-check" style="margin:0 5px 0 2px;"></i> <g:message code="saveChangesLbl"/>
                </votingsystem-button>
            </div>
        </div>
    </template>
    <script>
        Polymer('groupvs-editor', {
            publish: {
                groupvs: {value: {}}
            },
            ready:function() {
                console.log(this.tagName + " - ready")
                if(this.groupvs != null) {
                    this.$.textEditor.setData(this.groupvs.description)
                }
            },

            submitForm:function() {
                console.log("submitForm")
                if(this.$.textEditor.getData() == 0) {
                    this.$.textEditor.classList.add("formFieldError");
                    showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
                    return
                }
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_GROUP_EDIT)
                webAppMessage.serviceURL = "${createLink( controller:'groupVS', action:"edit", absolute:true)}/" + this.groupvs.id
                webAppMessage.signedMessageSubject = "<g:message code='newGroupVSMsgSubject'/>"
                webAppMessage.signedContent = {groupvsInfo:this.$.textEditor.getData(), groupvsName:this.groupvs.name,
                    id:this.groupvs.id, operation:Operation.VICKET_GROUP_NEW}
                webAppMessage.setCallback(function(appMessage) {
                    console.log("editGroupVSCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    var caption = '<g:message code="editGroupERRORCaption"/>'
                    var msg = appMessageJSON.message
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = '<g:message code="editGroupOKCaption"/>'
                        var msgTemplate = '<g:message code='accessLinkMsg'/>';
                        msg = msg + '</br></br>' + msgTemplate.format(appMessageJSON.URL + "?menu=admin")
                    }
                    showMessageVS(msg, caption)
                    window.scrollTo(0,0);
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        })
    </script>
</polymer-element>
