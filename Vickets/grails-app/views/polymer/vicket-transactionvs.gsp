<polymer-element name="vicket-transactionvs" attributes="transactionvs-data url subpage">
    <template>
        <style>
            .pageWidth {width: 1000px; margin: 0px auto 0px auto;}
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{transactionvs}}" handleAs="json" method="get" contentType="json"></core-ajax>
        <div class="" style="max-width:1000px; margin:0px auto 0px auto;">
            <div layout horizontal class="pageWidth">
                <template if="{{subpage != null}}">
                    <votingsystem-button isFab="true" on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                        <i class="fa fa-arrow-left"></i></votingsystem-button>
                </template>
                <h3 flex style="text-align: center;">{{transactionvs.description}}</h3>
            </div>
            <div layout vertical center center-justified>
                <div>
                    <template if="{{transactionvs.tags.length > 0}}">
                        <div id="tagsDiv" style="padding:0px 0px 0px 30px;">
                            <div style=" display: table-cell; font-size: 1.1em; font-weight: bold;"><g:message code='tagsLbl'/>:</div>
                            <div id="selectedTagDiv" style="margin:0px 0px 15px 0px; padding: 5px 5px 0px 5px; display: table-cell;" class="btn-group-xs">
                                <template repeat="{{tag in transactionvs.tags}}">
                                    <a class="btn btn-default" style="margin:0px 10px 0px 0px;">{{tag.name}}</a>
                                </template>
                            </div>
                        </div>
                    </template>
                    <div style=""><b><g:message code="subjectLbl"/>: </b>{{transactionvs.subject}}</div>
                    <div style=""><b><g:message code="amountLbl"/>: </b>{{transactionvs.amount}} {{transactionvs.currency}}</div>
                    <div style=""><b><g:message code="dateCreatedLbl"/>: </b>{{transactionvs.dateCreated}}</div>
                    <template if="{{transactionvs.validTo}}">
                        <div style=""><b><g:message code="validToLbl"/>: </b>{{transactionvs.validTo}}</div>
                    </template>
                    <div style="margin-left: 20px;">
                        <template if="{{transactionvs.fromUserVS}}">
                            <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;">
                                <g:message code="pagerLbl"/></div>
                            <div id="fromUserDiv">
                                <template if="{{'VICKET_SOURCE_INPUT' != transactionvs.type}}">
                                    <template if="{{'GROUP' == transactionvs.fromUserVS.type}}">
                                        <div style=""><b><g:message code="groupLbl"/>: </b>
                                            <a on-click="{{showFromUserInfo}}">{{transactionvs.fromUserVS.name}}</a>
                                        </div>
                                    </template>
                                    <template if="{{'GROUP' != transactionvs.fromUserVS.type}}">
                                        <div style=""><b><g:message code="nifLbl"/>: </b>{{transactionvs.fromUserVS.nif}}</div>
                                        <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs.fromUserVS.name}}</div>
                                    </template>
                                </template>
                                <template if="{{'VICKET_SOURCE_INPUT' == transactionvs.type && transactionvs.fromUserVS}}">
                                    <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs.fromUserVS.payer.fromUser}}</div>
                                    <div style=""><b><g:message code="IBANLbl"/>: </b>
                                        <a on-click="{{showInfoIBAN}}">{{transactionvs.fromUserVS.payer.fromUserIBAN}}</a></div>
                                </template>
                            </div>
                        </template>
                        <template if="{{transactionvs.fromUserVS == null}}">
                            <div style="font-weight: bold;"><g:message code="anonymousPagerLbl"/></div>
                        </template>
                    </div>

                    <template if="{{transactionvs.childTransactions}}">
                        <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;margin:10px 0px 0px 0px;">
                            <g:message code="receptorsLbl"/></div>
                        <div  layout flex horizontal wrap around-justified>
                            <template repeat="{{childTransactions in transactionvs.childTransactions}}">
                                <div class="btn btn-default" style="margin:5px 0px 0px 5px;">
                                    <div style=""><b><g:message code="nifLbl"/>: </b>{{childTransactions.toUserVS.nif}}</div>
                                    <div style=""><b><g:message code="nameLbl"/>: </b>{{childTransactions.toUserVS.name}}</div>
                                    <div style=""><b><g:message code="amountLbl"/>: </b>{{childTransactions.amount}} {{childTransactions.currency}}</div>
                                </div >
                            </template>
                        </div>
                    </template>

                    <template if="{{transactionvs.toUserVS}}">
                        <div style="margin:20px 0px 0px 20px;">
                            <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;"><g:message code="receptorLbl"/></div>
                            <template if="{{'GROUP' == transactionvs.toUserVS.type}}">
                                <div style=""><b><g:message code="groupLbl"/>: </b>
                                    <a on-click="{{showGroupInfo}}">
                                        {{transactionvs.toUserVS.name}}
                                    </a>
                                </div>
                            </template>
                            <template if="{{'GROUP' != transactionvs.toUserVS.type}}">
                                <div style=""><b><g:message code="nifLbl"/>: </b>{{transactionvs.toUserVS.nif}}</div>
                                <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs.toUserVS.name}}</div>
                            </template>
                        </div >
                    </template>

                    <div layout horizontal>
                        <div flex></div>
                        <votingsystem-button on-click="{{openReceipt}}">
                            <g:message code="openReceiptLbl"/> <i class="fa fa-cogs"></i>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer('vicket-transactionvs', {
            back:function() {
                this.fire('core-signal', {name: "vicket-transactionvs-closed", data: this.transactionvs.id});
            },
            showToUserInfo:function(e) {
                var groupURL = "${createLink(uri:'/groupVS')}/" + e.target.templateInstance.model.transactionvs.toUserVS.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = "${createLink(uri:'/groupVS')}/" +  e.target.templateInstance.model.transactionvs.fromUserVS.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showInfoIBAN:function(e) {
                var fromUserIBANInfoURL = "${createLink(uri:'/IBAN')}/from/" + e.target.templateInstance.model.transactionvs.fromUserVS.payer.fromUserIBAN
                console.log(this.tagName + " - showInfoIBAN - fromUserIBANInfoURL: " + fromUserIBANInfoURL)
            },
            ready: function() {console.log(this.tagName + " - ready")
                if(this['transactionvs-data'] != null) this.transactionvs = JSON.parse(this['transactionvs-data'])
                console.log(this.tagName + " - " + this.id + " - ready")
            },
            formatDate : function(dateValue) {

            },
            openReceipt: function () {
                console.log("openReceipt")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_RECEIPT)
                webAppMessage.message = this.transactionvs.receipt
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("openReceiptCallback - message: " + appMessage);
                    var appMessageJSON = JSON.parse(appMessage)
                    }}
                webAppMessage.callerCallback = objectId
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            saveReceipt:function () {
                console.log("saveReceipt")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SAVE_RECEIPT)
                webAppMessage.message = this.transactionvs.receipt
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("saveReceiptCallback - message from native client: " + appMessage);
                    var appMessageJSON = JSON.parse(appMessage)
                    }}
                webAppMessage.callerCallback = objectId
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>