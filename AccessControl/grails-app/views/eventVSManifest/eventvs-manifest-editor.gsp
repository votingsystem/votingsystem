<link rel="import" href="${resource(dir: '/bower_components/vs-texteditor', file: 'vs-texteditor.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">

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
        <core-signals on-core-signal-messagedialog-closed="{{messagedialog}}"></core-signals>
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
                <vs-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></vs-texteditor>
            </div>

            <div layout horizontal center center-justified style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                <div flex></div>
                <paper-button raised on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                    <i class="fa fa-check"></i> <g:message code="publishLbl"/>
                </paper-button>
            </div>
        </form>

    </div>


    </template>
    <script>
        Polymer('eventvs-manifest-editor', {
            appMessageJSON:null,
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
                var webAppMessage = new WebAppMessage(Operation.MANIFEST_PUBLISHING)
                webAppMessage.signedContent = eventVS
                webAppMessage.serviceURL = "${createLink( controller:'eventVSManifest', absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code="publishManifestSubject"/>"
                webAppMessage.setCallback(function(appMessage) {
                    console.log("publishDocumentCallback - message: " + appMessage);
                    this.appMessageJSON = toJSON(appMessage)
                    electionDocumentURL = null
                    if(this.appMessageJSON != null) {
                        var caption = '<g:message code="publishERRORCaption"/>'
                        var msg = this.appMessageJSON.message
                        if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                            caption = '<g:message code="publishOKCaption"/>'
                            var msgTemplate = "<g:message code='documentLinkMsg'/>";
                            msg = "<p><g:message code='publishOKMsg'/>.</p>" +  msgTemplate.format(this.appMessageJSON.message);
                        }
                        showMessageVS(msg, caption)
                    }
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
            },
            messagedialog:function() {
                if(this.appMessageJSON != null && this.appMessageJSON.message != null)
                    window.location.href = this.appMessageJSON.message
            }
        });
    </script>

</polymer-element>