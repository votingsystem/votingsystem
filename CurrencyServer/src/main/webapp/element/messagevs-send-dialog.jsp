<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/paper-toast/paper-toast.html" rel="import"/>

<dom-module name="messagevs-send-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            ${msg.sendMessageLbl}
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>
                    <div class="vertical flex layout" style="padding: 10px 10px 10px 10px; min-height:260px; ">
                        <span>[[messageToUser]]</span>
                        <div class="flex" style="font-size: 1.3em; color:#6c0404; text-align: center;display: block;">
                            <textarea rows="12" cols="60" maxlength="300" value="{{messageVS::input}}" style="max-width: 100%; "></textarea>
                        </div>
                        <div class="layout horizontal" style="padding:20px 20px 0 20px;">
                            <div class="flex"></div>
                            <button on-click="sendMessage">
                                <i class="fa fa-check" style="color: #388746;"></i> ${msg.acceptLbl}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <paper-toast id="toast" role="alert" text="${msg.messageSendedLbl}"></paper-toast>
    </template>
    <script>
        Polymer({
            is:'messagevs-send-dialog',
            properties: {
                user:{type:Object},
                messageVS:{type:String},
                sendMessageTemplateMsg:{type:String, value:"${msg.userMessageVSLbl}"}
            },
            ready: function() { },
            show: function (user) {
                this.user = user;
                this.messageVS = ""
                this.messageToUser = this.sendMessageTemplateMsg.format(this.user.name),
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            close:function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            },
            sendMessage: function () {
                console.log("sendMessageVS")
                if(this.messageVS.trim() == "") return
                var operationVS = new OperationVS(Operation.MESSAGEVS)
                operationVS.message = this.messageVS
                operationVS.nif = this.user.nif
                operationVS.user = this.user
                operationVS.setCallback(function(appMessageJSON) {
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) this.$.toast.show()
                    else  alert(appMessageJSON.message, '${msg.errorLbl}')
                    console.log(this.tagName + " - " + this.id + " - sendMessageVS callback");
                }.bind(this))
                VotingSystemClient.setMessage(operationVS);
                this.close()
            }
        });
    </script>
</dom-module>