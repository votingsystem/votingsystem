<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">

<polymer-element name="eventvs-manifest-editor" attributes="opened">
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
            <h3><g:message code="publishManifestLbl"/></h3>
        </div>

        <div style="display:{{messageToUser? 'block':'none'}}">
            <div class="messageToUser">
                <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <paper-shadow z="1"></paper-shadow>
            </div>
        </div>

        <form id="mainForm" on-submit="{{submitForm}}">

            <div style="margin:0px 0px 20px 0px">
                <div>
                    <input type="text" id="subject" class="form-control" style="width:600px;"
                           placeholder="<g:message code="subjectLbl"/>" error="<g:message code="requiredLbl"/>" required/>
                </div>
                <div layout horizontal center id="dateRangeDiv" style="margin:10px 0px 0px 0px;">
                    <label style="margin:0px 0px 0px 30px;">${message(code:'dateFinishLbl')}</label>
                    <div id="dateFinish">
                        <g:datePicker name="dateFinish" value="${new Date().plus(2)}" precision="day" relativeYears="[0..1]"/>
                    </div>
                </div>
            </div>

            <div style="position:relative; width:100%;">
                <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
            </div>

            <div layout horizontal center center-justified style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                <div flex></div>
                <votingsystem-button on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                    <g:message code="publishLbl"/> <i class="fa fa-check"></i>
                </votingsystem-button>
            </div>
        </form>

    </div>


    </template>
    <script>
        Polymer('eventvs-manifest-editor', {
            ready: function() {
                console.log(this.tagName + " - ready")
                //alert( CKEDITOR.basePath );
            },
            submitForm: function() {
                this.messageToUser = null
                this.$.subject.classList.remove("formFieldError");
                var dateFinish = getDatePickerValue('dateFinish', this.$.dateFinish)

                if(!this.$.subject.validity.valid) {
                    this.$.subject.classList.add("formFieldError");
                    this.messageToUser = '<g:message code="emptyFieldMsg"/>'
                    return
                }

                if(dateFinish == null) {
                    this.messageToUser = '<g:message code="emptyFieldMsg"/>'
                    return
                }

                if(dateFinish < new Date() ) {
                    this.messageToUser = '<g:message code="dateInitERRORMsg"/>'
                    return
                }

                if(this.$.textEditor.getData().length == 0) {
                    this.$.textEditor.classList.add("formFieldError");
                    this.messageToUser = '<g:message code="emptyDocumentERRORMsg"/>'
                    return
                }

                var eventVS = {};
                eventVS.subject = this.$.subject.value;
                eventVS.content = this.$.textEditor.getData();
                eventVS.dateFinish = dateFinish.formatWithTime();
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.MANIFEST_PUBLISHING)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.signedContent = eventVS
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                webAppMessage.serviceURL = "${createLink( controller:'eventVSManifest', absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code="publishManifestSubject"/>"

                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("publishDocumentCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    electionDocumentURL = null
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="publishERRORCaption"/>'
                        var msg = appMessageJSON.message
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = '<g:message code="publishOKCaption"/>'
                            var msgTemplate = "<g:message code='documentLinkMsg'/>";
                            msg = "<p><g:message code='publishOKMsg'/>.</p>" +  msgTemplate.format(appMessageJSON.message);
                            window.location.href = appMessageJSON.message
                        }
                        showMessageVS(msg, caption)
                    }
                    }.bind(this)}

                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
            }
        });
    </script>

</polymer-element>