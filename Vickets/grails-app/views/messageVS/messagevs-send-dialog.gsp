<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog-transition.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">

<polymer-element name="messagevs-send-dialog" attributes="opened">
    <template>
        <paper-dialog id="xDialog" layered backdrop title="<g:message code="sendMessageVSDialogCaption"/>"
                             on-core-overlay-open="{{onCoreOverlayOpen}}">
            <style> </style>
            <g:include view="/include/styles.gsp"/>
            <div layout vertical style=" min-height:300px;">
                <div flex layout vertical style="padding: 10px 10px 10px 10px; height: 100%;">
                    <div flex style="font-size: 1.3em; color:#6c0404; font-weight: bold; text-align: center; padding:0px 20px 0px 20px;">
                        <paper-input floatingLabel id="messageVSContent" multiline rows="4" label="{{messageToUser}}" required></paper-input>
                    </div>

                    <div layout horizontal style="padding:20px 20px 20px 20px;">
                        <div flex></div>
                        <paper-button raised on-click="{{sendMessage}}">
                            <i class="fa fa-check"></i> <g:message code="acceptLbl" />
                        </paper-button>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer('messagevs-send-dialog', {
            toUserNIF:'',
            certificateList:[],

            ready: function() {
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
                webAppMessage.serviceURL = "${createLink(controller:'messageVS', absolute:true)}/"
                webAppMessage.signedMessageSubject = "<g:message code="sendEncryptedMessageSubject"/>"
                webAppMessage.signedContent = {operation:Operation.MESSAGEVS, toUserNIF:this.toUserNIF}
                webAppMessage.documentToEncrypt = {operation:Operation.MESSAGEVS,
                    messageContent:this.$.messageVSContent.value}
                webAppMessage.targetCertList = this.certificateList
                webAppMessage.contentType = 'application/messagevs'
                webAppMessage.setCallback(function(appMessage) {
                    console.log(this.tagName + " - " + this.id + " - sendMessageVS callback: " + appMessage);
                    this.fire('message-response', appMessage)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                this.opened = false
            }
        });
    </script>
</polymer-element>