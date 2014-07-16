<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">

<polymer-element name="groupvs-editor" attributes="groupvs">
    <template>
        <h3>
            <div id="editGroupHeader" class="pageHeader text-center">
                <votingsystem-html-echo html="{{pageHeader}}"></votingsystem-html-echo>
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
                <button on-click="{{submitForm}}" class="btn btn-default">
                    <g:message code="saveChangesLbl"/> <i class="fa fa fa-check"></i>
                </button>
            </div>
        </div>
    </template>
    <script>
        Polymer('groupvs-editor', {
            ready:function() {
                console.log(this.tagName + " - ready")
                this.groupvsJSON = JSON.parse(this.groupvs)
                this.$.textEditor.setData(this.groupvsJSON.description)
                this.pageHeader =  "<g:message code="editingGroupMsgTitle"/>".format(this.groupvsJSON.name)
            },

            submitForm:function() {
                console.log("submitForm")
                if(this.$.textEditor.getData() == 0) {
                    this.$.textEditor.classList.add("formFieldError");
                    showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
                    return
                }
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_GROUP_EDIT)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink( controller:'groupVS', action:"edit", absolute:true)}/" + this.groupvsJSON.id
                webAppMessage.signedMessageSubject = "<g:message code='newGroupVSMsgSubject'/>"
                webAppMessage.signedContent = {groupvsInfo:this.$.textEditor.getData(), groupvsName:this.groupvsJSON.name,
                    id:this.groupvsJSON.id, operation:Operation.VICKET_GROUP_NEW}
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("editGroupVSCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="editGroupERRORCaption"/>'
                        var msg = appMessageJSON.message
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = '<g:message code="editGroupOKCaption"/>'
                            var msgTemplate = '<g:message code='accessLinkMsg'/>';
                            msg = msg + '</br></br>' + msgTemplate.format(appMessageJSON.URL + "?menu=admin")
                        }
                        showMessageVS(msg, caption)
                    }
                    window.scrollTo(0,0);
                }}
                webAppMessage.callerCallback = objectId
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        })
    </script>
</polymer-element>
