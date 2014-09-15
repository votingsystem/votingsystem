<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">

<polymer-element name="send-message-dialog" attributes="opened">
    <template>
        <votingsystem-dialog id="xDialog" class="sendMsgDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <!-- place all overlay styles inside the overlay target -->
            <style>
                .sendMsgDialog {
                    box-sizing: border-box;
                    -moz-box-sizing: border-box;
                    font-family: Arial, Helvetica, sans-serif;
                    font-size: 13px;
                    -webkit-user-select: none;
                    -moz-user-select: none;
                    overflow: auto;
                    background: white;
                    padding:0px 0px 0px 0px;
                    outline: 1px solid rgba(0,0,0,0.2);
                    box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                    width: 500px;
                }
            </style>
            <div layout vertical style=" min-height:300px;">
                <div layout horizontal center center-justified style="background: #6c0404;">
                    <div flex style="font-size: 1.3em; margin:0px 0px 0px 30px;font-weight: bold; color:#f9f9f9;display:{{messageToUser? 'block':'none'}}">
                        <g:message code="sendMessageVSDialogCaption"/>
                    </div>
                    <div>
                        <core-icon-button on-click="{{close}}" icon="close" style="color:#f9f9f9;"></core-icon-button>
                    </div>
                </div>
                <div flex layout vertical style="padding: 10px 10px 10px 10px; height: 100%;">
                    <div flex style="font-size: 1.3em; color:#6c0404; font-weight: bold; text-align: center; padding:0px 20px 0px 20px;">
                        <paper-input floatingLabel id="messageVSContent" multiline rows="4" label="{{messageToUser}}" required></paper-input>
                    </div>

                    <div layout horizontal style="padding:20px 20px 20px 20px;">
                        <div flex></div>
                        <votingsystem-button on-click="{{sendMessage}}">
                            <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl" />
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('send-message-dialog', {
            toUserNIF:'',
            certificateList:[],

            ready: function() {
                this.objectId = Math.random().toString(36).substring(7)
                window[this.objectId] = this
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.async(function() { this.$.xDialog.opened = this.opened});
            },
            show: function (toNIF, message, certificateList) {
                this.toUserNIF = toNIF
                this.$.messageVSContent.value = ""
                this.messageToUser = message
                this.certificateList = certificateList
                this.opened = true
            },
            close:function() {
               this.opened = false
            },
            sendMessage: function () {
                console.log("sendMessageVS")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.MESSAGEVS)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink(controller:'messageVS', absolute:true)}/"
                webAppMessage.signedMessageSubject = "<g:message code="sendEncryptedMessageSubject"/>"
                webAppMessage.signedContent = {operation:Operation.MESSAGEVS, toUserNIF:this.toUserNIF}
                webAppMessage.documentToEncrypt = {operation:Operation.MESSAGEVS,
                    messageContent:this.$.messageVSContent.value}
                webAppMessage.targetCertList = this.certificateList

                webAppMessage.contentType = 'application/messagevs'
                webAppMessage.callerCallback = this.objectId
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                this.opened = false
            },

            /*This method is called from JavaFX client. So we put referencens on global window */
            setClientToolMessage:function(appMessage) {
                console.log(this.tagName + " - " + this.id + " - setClientToolMessage: " + appMessage);
                this.fire('message-response', appMessage)
            }
        });
    </script>
</polymer-element>