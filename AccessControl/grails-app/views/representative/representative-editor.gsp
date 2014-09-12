<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">

<polymer-element name="representative-editor" attributes="pageHeader editorData">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
        </style>

        <div class="pageHeader"  layout horizontal center center-justified>
            <h3>{{pageHeader}}</h3>
        </div>

        <div style="display:{{messageToUser? 'block':'none'}}">
            <div class="messageToUser">
                <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <paper-shadow z="1"></paper-shadow>
            </div>
        </div>

        <div style="margin:15px 0 0 0;">
            <ul>
                <li><g:message code="newRepresentativeAdviceMsg2"/></li>
                <li><g:message code="newRepresentativeAdviceMsg3"/></li>
                <li><g:message code="newRepresentativeAdviceMsg4"/></li>
            </ul>
        </div>

        <div style="position:relative; width:100%;">
            <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
        </div>

        <div layout horizontal center center-justified style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
            <votingsystem-button on-click="{{selectImage}}" style="margin: 0px 0px 0px 5px;">
                <i class="fa fa-file-image-o" style="margin:0 7px 0 3px;"></i> <g:message code="selectImageLbl"/>
            </votingsystem-button>
            <div flex></div>
            <votingsystem-button on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="publishLbl"/>
            </votingsystem-button>
        </div>
        <div style="margin:10px 10px 40px 10px;">{{selectedImagePath}}</div>

    </div>
    </template>
    <script>
        Polymer('representative-editor', {
            publish: {
                representativeData: {value: {}}
            },
            appMessageJSON:null,
            selectedImagePath:null,
            editorData:null,
            pageHeader:'<g:message code="newRepresentativePageTitle"/>',
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            editorDataChanged:function() {
                console.log("editorDataChanged: " + this.editorData)
                this.$.textEditor.setData(this.editorData)
            },
            selectImage: function() {
                console.log(this.tagName + " - selectImage")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SELECT_IMAGE)
                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                this.appMessageJSON = null
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("selectImageCallback - appMessage: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        this.selectedImagePath = appMessageJSON.message
                    } else if(appMessageJSON.message){
                        showMessageVS(appMessageJSON.message, '<g:message code='errorLbl'/>')
                    }
                }.bind(this)}

                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            submitForm: function() {
                this.messageToUser = null
                this.$.textEditor.classList.remove("formFieldError");
                if(this.$.textEditor.getData().length == 0) {
                    this.$.textEditor.classList.add("formFieldError");
                    this.messageToUser = '<g:message code="emptyDocumentERRORMsg"/>'
                    return
                }
                if(this.selectedImagePath == null) {
                    showMessageVS('<g:message code="missingImageERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
                    return
                }
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.NEW_REPRESENTATIVE)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.filePath = this.selectedImagePath
                webAppMessage.signedContent = {representativeInfo:this.$.textEditor.getData(), operation:Operation.REPRESENTATIVE_DATA}
                webAppMessage.serviceURL = "${createLink( controller:'representative', absolute:true)}"
                webAppMessage.signedMessageSubject = '<g:message code="representativeDataLbl"/>'
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"

                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("newRepresentativeCallback - message from native client: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="newRepresentativeERRORCaption"/>'
                        var msg = appMessageJSON.message
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = '<g:message code="newRepresentativeOKCaption"/>';
                        }
                        showMessageVS(msg, caption)
                    }
                }.bind(this)}

                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            messagedialog:function() {
                if(this.appMessageJSON != null && this.appMessageJSON.message != null)
                    window.location.href = this.appMessageJSON.message
            }
        });
    </script>

</polymer-element>