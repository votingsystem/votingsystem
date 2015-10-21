<%@ page contentType="text/html; charset=UTF-8" %>
<dom-module name="message-smime-viewer">
    <template>
        <iron-ajax auto id="ajax" url="{{url}}" handle-as="json" last-response="{{smimeMessageDto}}" method="get"
                   content-type="application/json"></iron-ajax>
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
                url:{type:String, value:null},
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
                app.loadMainContent(element)
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = window['isClientToolConnected']
                document.querySelector("#voting_system_page").addEventListener('votingsystem-client-connected',
                        function() {  this.isClientToolConnected = true }.bind(this))
            }
        });
    </script>
</dom-module>