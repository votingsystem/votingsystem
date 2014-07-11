<polymer-element name="send-message-dialog">
    <template>
        <style>
        .card {
            position: relative;
            display: inline-block;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
            border: 1px solid #ccc;
        }
        paper-button.button {
            background-color: #f9f9f9;
            color: #6c0404;
            border: 1px solid #ccc;
            margin:10px;
            vertical-align: middle;
            line-height: 24px;
            height: 35px;
        }
        </style>
        <div class="card" style="width:400px; display: {{isVisible?'block':'none'}}">
            <div layout horizontal center center-justified style="background: #6c0404;">
                <div flex style="font-size: 1.3em; margin:0px 0px 0px 30px;font-weight: bold; color:#f9f9f9;display:{{messageToUser? 'block':'none'}}">
                    <g:message code="sendMessageVSDialogCaption"/>
                </div>
                <div>
                    <core-icon-button on-click="{{accept}}" icon="close" style="fill:#f9f9f9;"></core-icon-button>
                </div>
            </div>
            <div style="font-size: 1.3em; color:#6c0404; font-weight: bold; text-align: center; padding:30px 20px 30px 20px;">
                <p style="text-align: center;  font-size: 1.2em;display:{{messageToUser == null?'none':'block'}}">{{messageToUser}}</p>
                <label class="control-label" ><g:message code="sendMessageVSMsg"/></label>
                <textarea id="messageVSContent" class="form-control" rows="4"></textarea>
            </div>

            <div layout horizontal>
                <div flex></div>
                <div class="button raised accept" on-click="{{sendMessage}}"
                     style="border: 1px solid #ccc; margin:0px 10px 10px 5px;">
                    <div ><g:message code="acceptLbl" /></div>
                    <paper-ripple fit></paper-ripple>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer('send-message-dialog', {
            isVisible:false,
            toUserNIF:'',
            certificateList:[],

            ready: function() {
                this.objectId = Math.random().toString(36).substring(7)
                window[this.objectId] = this
            },

            show: function (toNIF, message, certificateList) {
                this.toUserNIF = toNIF
                this.$.messageVSContent.value = ""
                this.messageToUser = message
                this.certificateList = certificateList
                this.isVisible = true
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
                this.isVisible = false
            },

            /*This method is called from JavaFX client. So we put referencens on global window */
            setClientToolMessage:function(appMessage) {
                console.log(this.tagName + " - " + this.id + " - setClientToolMessage: " + appMessage);
                this.fire('message-response', appMessage)
            }
        });
    </script>
</polymer-element>