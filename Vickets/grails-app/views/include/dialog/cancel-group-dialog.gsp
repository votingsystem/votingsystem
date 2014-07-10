<polymer-element name="cancel-group-dialog">
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
        <div class="card" style="width:400px; padding:20px">
            <div layout horizontal center center-justified style="margin:10px 10px 20px 10px; font-size: 1.3em; font-weight: bold; color:#6c0404;">
                <div flex><g:message code="confirmOperationMsg"/></div>
                <core-icon-button icon="close" style="fill:#6c0404;" on-tap="{{close}}"></core-icon-button>
            </div>
            <div class="center" style="font-size: 1.3em; color:#888; font-weight: bold;">
                <p style="text-align: center;  font-size: 1.1em;">
                    <g:message code="cancelGroupVSDialogMsg" args="${[groupvsMap.name]}"/>
                </p>
            </div>
            <div layout horizontal style="margin:10px 20px 0px 0px;">
                <div flex></div>
                <paper-button raisedButton class="button" label="<g:message code="acceptLbl"/>"
                              on-click="{{accept}}" style=""></paper-button>
            </div>
        </div>
    </template>
    <script>
        Polymer('cancel-group-dialog', {
            appMessageJSON:null,

            ready: function() {
                this.style.display = 'none'
                this.randomStr = Math.random().toString(36).substring(7)
                window[this.randomStr] = this
                document.querySelector("#_votingsystemMessageDialog").addEventListener('message-accepted', function() {
                    if(this.appMessageJSON != null && ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                        window.location.href = updateMenuLink(appMessageJSON.URL)
                    }
                })
            },

            close: function() {
                this.style.display = 'none'
            },

            show: function() {
                console.log(this.tagName + " - show")
                this.style.display = 'block'
            },

            setClientToolMessage: function(appMessage) {
                this.appMessageJSON = toJSON(appMessage)
                if(this.appMessageJSON != null) {
                    var caption = '<g:message code="groupCancelERRORLbl"/>'
                    if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                        caption = "<g:message code='groupCancelOKLbl'/>"
                        callBackResult = cancelGroupResultOKCallback
                        groupURL = this.appMessageJSON.URL
                    }
                    showMessageVS(this.appMessageJSON.message, caption)
                }
            },
            accept: function(message, caption) {
                console.log(this.tagName + " - accept")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_CANCEL)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'cancel',absolute:true)}/${groupvsMap.id}"
                webAppMessage.signedMessageSubject = "<g:message code="cancelGroupVSSignedMessageSubject"/>"
                webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_CANCEL, groupvsName:"${groupvsMap.name}", id:${groupvsMap.id}}
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                webAppMessage.callerCallback = this.randomStr
                console.log(this.tagName + "this.randomStr: " + this.randomStr)
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }

        });
    </script>
</polymer-element>