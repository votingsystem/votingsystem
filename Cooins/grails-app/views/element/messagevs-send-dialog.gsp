<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="paper-toast" file="paper-toast.html"/>


<polymer-element name="messagevs-send-dialog" attributes="opened">
    <template>
        <paper-dialog id="xDialog" autoCloseDisabled layered backdrop title="<g:message code="sendMessageVSDialogCaption"/>"
                      on-core-overlay-open="{{onCoreOverlayOpen}}" style="margin:auto;">
            <style></style>
            <g:include view="/include/styles.gsp"/>
                <div>
                    <div layout horizontal center center-justified>
                        <div flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                            <div style="text-align: center;">
                                <g:message code="sendEncryptedMessageSubject"/>
                            </div>
                        </div>
                        <div style="position: absolute; top: 0px; right: 0px;">
                            <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                        </div>
                    </div>
                    <div layout vertical style="">
                        <div vertical flex layout style="padding: 10px 10px 10px 10px; min-height:260px; ">
                            {{messageToUser}}
                            <div flex style="font-size: 1.3em; color:#6c0404; text-align: center;">
                                <textarea rows="12" cols="50" maxlength="{{messageVSMaxLength}}" value="{{messageVS}}"></textarea>
                            </div>
                            <div layout horizontal style="padding:20px 20px 0 20px;">
                                <div flex></div>
                                <paper-button raised on-click="{{sendMessage}}">
                                    <i class="fa fa-check"></i> <g:message code="acceptLbl" />
                                </paper-button>
                            </div>
                        </div>
                    </div>
                </div>
        </paper-dialog>
        <paper-toast id="toast" role="alert" text="<g:message code="messageSendedLbl"/>"></paper-toast>
    </template>
    <script>
        Polymer('messagevs-send-dialog', {
            uservs:{},
            messageVSMaxLength:500,
            sendMessageTemplateMsg:"<g:message code="uservsMessageVSLbl"/>",
            ready: function() { },
            messageVSChanged: function() {
                if(this.messageVS.length > this.messageVSMaxLength) this.messageVS = this.messageVS.substring(0,
                        this.messageVSMaxLength);
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.async(function() { this.$.xDialog.opened = this.opened});
            },
            show: function (uservs) {
                this.uservs = uservs;
                this.messageVS = ""
                this.messageToUser = this.sendMessageTemplateMsg.format(this.uservs.name),
                this.opened = true
            },
            close:function() {
               this.opened = false
            },
            sendMessage: function () {
                console.log("sendMessageVS")
                if(this.messageVS.trim() == "") return
                var webAppMessage = new WebAppMessage(Operation.MESSAGEVS)
                webAppMessage.message = this.messageVS
                webAppMessage.nif = this.uservs.nif
                webAppMessage.contentType = 'application/messagevs'
                webAppMessage.setCallback(function(appMessage) {
                    console.log(this.tagName + " - " + this.id + " - sendMessageVS callback: " + appMessage);
                    this.fire('message-response', appMessage)
                }.bind(this))
                //alert(JSON.stringify(webAppMessage))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                this.opened = false
                this.$.toast.show()
            }
        });
    </script>
</polymer-element>