<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-cms-representative-anonymousdelegation-request">
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
                    <div class="pageHeader"><h3>${msg.anonymousdelegationRequest}</h3></div>
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.timeStampDateLbl}: </b><span>{{timeStampDate}}</span>
                </div>
                <div hidden="{{!messageToUser}}" layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser"><span>{{messageToUser}}</span></div>
                    </div>
                </div>
                <div><b>${msg.weeksAnonymousDelegation}: </b><span>{{cmsMessageContent.weeksOperationActive}}</span></div>
                <div class="horizontal layout" style="margin:10px 0 0 0;"><b>${msg.validFromLbl}:</b> {{getDate(cmsMessageContent.dateFrom)}}
                    <span style="margin: 0 0 0 20px;"><b>${msg.toLbl}:</b></span> <span>{{getDate(cmsMessageContent.dateFrom)}}</span></div>
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
            is:'message-cms-representative-anonymousdelegation-request',
            properties: {
                cmsMessageContent:{type:Object, value:{}, observer:'cmsMessageContentChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready - " + document.querySelector("#voting_system_page"))
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            cmsMessageContentChanged:function() {
                this.messageToUser = null
                if('ANONYMOUS_SELECTION_CERT_REQUEST' != this.cmsMessageContent.operation )
                    this.messageToUser = '${msg.cmsTypeErrorMsg}' + " - " + this.cmsMessageContent.operation
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                operationVS.message = this.cmsMessage
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>