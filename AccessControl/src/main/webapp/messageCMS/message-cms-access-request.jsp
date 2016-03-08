<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-cms-access-request">
    <template>
        <style>
        .timeStampMsg {
            color:#aaaaaa; font-size:1.1em; margin:0 0 15px 0;font-style:italic;
        }
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="pageHeader"><h3>${msg.accessRequestLbl}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.timeStampDateLbl}: </b><span>{{timeStampDate}}</span>
                </div>
                <div><b>${msg.eventVSLbl}: </b>
                    <a href="{{cmsMessageContent.eventURL}}" target="_blank">{{cmsMessageContent.eventURL}}</a>
                </div>
                <div class="layout horizontal">
                    <div class="flex"></div>
                    <div hidden="{{!isClientToolConnected}}" class="flex horizontal layout end-justified" style="margin:10px 0px 10px 0px;">
                        <button on-click="checkReceipt">
                            <i class="fa fa-certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'message-cms-access-request',
            properties: {
                cmsMessageContent:{type:Object, value:{}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.timeStampDate = null
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                operationVS.message = this.cmsMessage
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>
