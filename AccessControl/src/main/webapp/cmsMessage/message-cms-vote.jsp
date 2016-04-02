<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-cms-vote">
    <template>
        <style>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .timeStampMsg {
            color:#aaaaaa; font-size:1.1em; margin:0 0 15px 0;font-style:italic;
        }
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="pageHeader"><h3>${msg.voteReceipt}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.timeStampDateLbl}: </b><span>{{timeStampDate}}</span>
                </div>
                <div hidden="{{!messageToUser}}">
                    <div  layout horizontal center center-justified  class="messageToUser">
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                </div>
                <div><b>${msg.eventVSLbl}: </b>
                    <a href="{{cmsMessageContent.eventURL}}?mode=simplePage">{{cmsMessageContent.eventURL}}</a>
                </div>
                <div><b>${msg.optionSelectedLbl}: </b><span>{{cmsMessageContent.optionSelected.content}}</span></div>
                <div class="layout horizontal">
                    <div class="flex"></div>
                    <div hidden="{{!isClientToolConnected}}" class="flex horizontal layout end-justified" style="margin:10px 0px 10px 0px;">
                        <button on-click="checkReceipt">
                            <i class="fa fa-x509Certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'message-cms-vote',
            properties: {
                cmsMessageContent:{type:Object, value:{}, observer:'cmsMessageContentChanged'}
            },

            messageToUser:null,
            timeStampDate:null,
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            cmsMessageContentChanged:function() {
                this.messageToUser = null
                if('SEND_VOTE' != this.cmsMessageContent.operation )
                    this.messageToUser = '${msg.cmsTypeErrorMsg}' + " - " + this.cmsMessageContent.operation
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                operationVS.message = this.cmsMessage
                vs.client.processOperation(operationVS);
            }
        });
    </script>
</dom-module>
