<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">

<polymer-element name="eventvs-backup-request-dialog" attributes="opened">
    <template>
        <votingsystem-dialog id="xDialog" class="dialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <style no-shim></style>
            <div id="container" layout vertical style="overflow-y: auto; width:450px; padding:10px;">
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="backupRequestCaption"/></div>
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

                <p style="text-align: center;">
                    <g:message code="backupRequestMsg"/>
                </p>
                <div  layout horizontal center center-justified  style="margin:10px 0px 10px 0px;">
                    <input type="email" class="form-control" id="eventBackupUserEmailText" style="width:360px;margin:0px auto 0px auto;"
                           placeholder="<g:message code="emailInputLbl"/>" title='<g:message code='enterEmailLbl'/>' required/>
                </div>

                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex></div>
                    <div style="margin:10px 0px 10px 0px;">
                        <votingsystem-button on-click="{{requestBackup}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('eventvs-backup-request-dialog', {
            publish: {
                eventvs: {value: {}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.$.xDialog.opened = this.opened
                if(this.opened == false) this.close()
            },
            show: function(eventvs) {
                this.eventvs = eventvs
            },
            requestBackup: function() {
                this.messageToUser = null
                if(!this.$.eventBackupUserEmailText.validity.valid){
                    this.messageToUser = "<g:message code="formErrorMsg"/>"
                    return
                }
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.BACKUP_REQUEST)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink(controller:'backupVS', absolute:true)}"
                webAppMessage.signedMessageSubject = '<g:message code="requestEventvsBackupMsgSubject"/>'
                webAppMessage.eventVS = this.eventvs
                webAppMessage.signedContent = this.eventvs
                webAppMessage.email = this.$.eventBackupUserEmailText.value
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"

                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId

                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("activateUserCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="operationERRORCaption"/>'
                        if(ResponseVS.SC_OK ==  appMessageJSON.statusCode) {
                            caption = '<g:message code="operationOKCaption"/>'
                            this.fire('backup-request-ok', appMessageJSON.message);
                            this.opened = false
                        }
                        showMessageVS(appMessageJSON.message, caption)
                    }}.bind(this)}
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
            },
            close: function() {
                this.opened = false
                this.messageToUser = null
            }
        });
    </script>
</polymer-element>
