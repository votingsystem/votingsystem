<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="eventvs-backup-request-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div id="container" style="overflow-y: auto; width:450px; padding:10px;">
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.backupRequestCaption}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>

                <div hidden="{{!messageToUser}}">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{messageToUser}}</div>
                        </div>
                    </div>
                </div>

                <p style="text-align: center;">
                    ${msg.backupRequestMsg}
                </p>
                <div class="layout horizontal center center-justified"  style="margin:10px 0px 10px 0px;">
                    <input type="email" class="form-control" id="eventBackupUserEmailText" style="width:360px;margin:0px auto 0px auto;"
                           placeholder="${msg.emailInputLbl}" title='${msg.enterEmailLbl}' required/>
                </div>

                <div class="layout horizontal" style="margin:0px 20px 0px 0px;">
                    <div class="flex"></div>
                    <div style="margin:10px 0px 10px 0px;">
                        <button on-click="requestBackup" style="margin: 0px 0px 0px 5px; font-size: 1.1em;">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'eventvs-backup-request-dialog',
            properties: {
                eventvs:{type:Object, value:{}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(eventvs) {
                this.messageToUser = null
                this.eventvs = eventvs
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            requestBackup: function() {
                this.messageToUser = null
                if(!this.$.eventBackupUserEmailText.validity.valid){
                    this.messageToUser = "${msg.formErrorMsg}"
                    return
                }
                var operationVS = new OperationVS(Operation.BACKUP_REQUEST)
                operationVS.serviceURL = contextURL + "/rest/backupVS"
                operationVS.signedMessageSubject = '${msg.requestEventvsBackupMsgSubject}'
                operationVS.eventVS = this.eventvs
                operationVS.jsonStr = JSON.stringify(this.eventvs)
                operationVS.email = this.$.eventBackupUserEmailText.value
                operationVS.setCallback(function(appMessage) { this.showResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS)
            },
            showResponse: function(appMessage) {
                console.log("activateUserCallback - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                var caption = '${msg.operationERRORCaption}'
                if(ResponseVS.SC_OK ==  appMessageJSON.statusCode) {
                    caption = '${msg.operationOKCaption}'
                    this.fire('backup-request-ok', appMessageJSON.message);
                    this.close()
                }
                showMessageVS(appMessageJSON.message, caption)
                this.click()
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>
