<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">


<polymer-element name="eventvs-admin-dialog" attributes="opened">
    <template>
        <votingsystem-dialog flex vertical id="xDialog" vertical opened="{{opened}}" layered="true" sizingTarget="{{$.container}}">
            <g:include view="/include/styles.gsp"/>
            <!-- place all overlay styles inside the overlay target -->
            <style no-shim>
                .messageToUser {
                    font-weight: bold;
                    margin:10px auto 10px auto;
                    background: #f9f9f9;
                    padding:10px 20px 10px 20px;
                }
            </style>
            <div id="container" layout vertical style="overflow-y: auto; width:450px; padding:10px;">
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:0px 0px 0px 30px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="cancelEventCaption"/></div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>

                <div style="display:{{messageToUser? 'block':'none'}}">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{messageToUser}}</div>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>

                <div>
                    <p style="text-align: center;"><g:message code="adminDocumenInfoMsg"/></p>
                    <g:message code="documentStateSelectionMsg"/>:<br/>
                    <div style="font-size: 0.9em; margin:10px 0 0 10px;">
                        <div class="radio">
                            <label>
                                <input type="radio" name="optionsRadios" id="selectDeleteDocument" value="">
                                <g:message code="selectDeleteDocumentMsg"/>
                            </label>
                        </div>
                        <div class="radio">
                            <label>
                                <input type="radio" name="optionsRadios" id="selectCloseDocument" value="">
                                <g:message code="selectCloseDocumentMsg"/>
                            </label>
                        </div>
                    </div>
                </div>

                <div layout horizontal style="margin:10px 20px 0px 0px; margin:10px;">
                    <div flex></div>
                    <votingsystem-button on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-check" style="margin:0 5px 0 2px;"></i> <g:message code="acceptLbl"/>
                    </votingsystem-button>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('eventvs-admin-dialog', {
            publish: {
                eventvs: {value: {}}
            },
            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.$.xDialog.opened = this.opened
                if(this.opened == false) this.close()
            },
            submitForm: function() {
                console.log("submitForm")
                this.messageToUser = null
                if(!this.$.selectDeleteDocument.checked && !this.$.selectCloseDocument.checked) {
                    this.messageToUser = "<g:message code='selectDocumentStateERRORMsg'/>"
                } else {
                    var state
                    var messageSubject
                    if(this.$.selectDeleteDocument.checked) {
                        state = EventVS.State.DELETED_FROM_SYSTEM
                        messageSubject = '<g:message code="deleteEventVSMsgSubject"/>'
                    } else if(this.$.selectCloseDocument.checked) {
                        state = EventVS.State.CANCELLED
                        messageSubject = '<g:message code="cancelEventVSMsgSubject"/>'
                    }
                    var webAppMessage = new WebAppMessage( ResponseVS.SC_PROCESSING, Operation.EVENT_CANCELLATION)
                    webAppMessage.serviceURL= "${createLink(controller:'eventVS', action:'cancelled', absolute:true)}"
                    var signedContent = {operation:Operation.EVENT_CANCELLATION,
                        accessControlURL:"${grailsApplication.config.grails.serverURL}",
                        eventId:Number(this.eventvs.id), state:state}
                    webAppMessage.signedContent = signedContent
                    webAppMessage.signedMessageSubject = messageSubject
                    webAppMessage.setCallback(function(appMessage) {
                        console.log("eventvs-admin-dialog callback - message: " + appMessage);
                        var appMessageJSON = toJSON(appMessage)
                        var caption
                        var msg
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='operationOKCaption'/>"
                            msg = "<g:message code='documentCancellationOKMsg'/>".format(this.eventvs.subject);
                            window.location.href = "${createLink(controller:'eventVSElection')}/" + votingEvent.id
                        } else {
                            caption = "<g:message code='operationERRORCaption'/>"
                            msg = appMessageJSON.message
                        }
                        showMessageVS(msg, caption)
                    }.bind(this))
                    VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                }
            },
            close: function() {
                this.opened = false
                this.messageToUser = null
            }
        });
    </script>
</polymer-element>
