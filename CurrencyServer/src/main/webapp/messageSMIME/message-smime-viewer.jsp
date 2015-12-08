<%@ page contentType="text/html; charset=UTF-8" %>
<dom-module name="message-smime-viewer">
    <template>
        <div id="messageViewer" class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
        </div>
    </template>
    <script>
        Polymer({
            is:'message-smime-viewer',
            properties: {
                smimeMessageDto:{type:Object, value:{}, observer:'smimeMessageChanged'},
                MESSAGE_SMIME:{type:Object, value:null},
                CURRENCY_GROUP_NEW:{type:Object, value:null},
                FROM_BANKVS:{type:Object, value:null},
                CURRENCY_REQUEST:{type:Object, value:null},
                FROM_GROUP_TO_ALL_MEMBERS:{type:Object, value:null},
                CURRENCY_CHANGE:{type:Object, value:null},
                viewer: {type:String, value: ""},
                url:{type:String, observer:'getHTTP'},
                isClientToolConnected: {type:Boolean, value: false}
            },
            smimeMessageChanged: function() {
                switch (this.smimeMessageDto.viewer) {
                    case "message-smime":
                        break;
                    case "message-smime-groupvs-new":
                        break;
                    case "message-smime-transactionvs-from-bankvs":
                        if(!this.FROM_BANKVS) {
                            Polymer.Base.importHref(contextURL + '/messageSMIME/message-smime-transactionvs-from-bankvs.vsp', function(e) {
                                console.log(this.tagName + " - message-smime-transactionvs-from-bankvs: " + this.FROM_BANKVS)
                                this.FROM_BANKVS = document.createElement('message-smime-transactionvs-from-bankvs');
                                this.loadMainContent(this.FROM_BANKVS)
                            }.bind(this));
                        } else this.loadMainContent(this.FROM_BANKVS)
                        break;
                    case "message-smime-transactionvs-currency-request":
                        break;
                    case "message-smime-transactionvs":
                        break;
                    case "message-smime-transactionvs-currency-change":
                        break;
                }
            },
            loadMainContent: function(element) {
                element.smimeMessageDto = this.smimeMessageDto
                vs.loadMainContent(element)
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined)
                document.querySelector("#voting_system_page").addEventListener('votingsystem-client-msg',
                        function() {  this.isClientToolConnected = true }.bind(this))
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.smimeMessageDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>