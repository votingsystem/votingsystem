<%@ page import="org.votingsystem.model.TypeVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-data']"/>">

<polymer-element name="message-smime" attributes="signedDocument smimeMessage isClientToolConnected timeStampDate">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .timeStampMsg {
            color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic;
        }
        </style>
        <div layout vertical style="margin: 0px auto; max-width:800px;">
            <div class="pageHeader"  layout horizontal center center-justified
                 style="margin:0 0 10px 0;font-size: 1.2em;">{{caption}}</div>
            <div class="timeStampMsg" style="display:{{timeStampDate ? 'block':'none'}}">
                <b><g:message code="timeStampDateLbl"/>: </b>{{timeStampDate}}
            </div>
            <div style="display:{{messageToUser? 'block':'none'}}">
                <div  layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </div>

            <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
            <div style=""><b><g:message code="subjectLbl"/>: </b>{{signedDocument.subject}}</div>
            <div horizontal layout>
                <div style=""><b><g:message code="amountLbl"/>: </b>{{signedDocument.amount}} {{signedDocument.currencyCode}}</div>
                <template if="{{signedDocument.isTimeLimited}}">
                    <div class="pageHeader" style="margin: 0 0 0 20px;"><b>
                        ${message(code: 'timeLimitedLbl', null).toUpperCase()}</b>
                    </div>
                </template>
            </div>


            <div id="fromUserDivContainer" style="margin-left: 20px;">
                <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;">
                    <g:message code="senderLbl"/></div>
                <div id="fromUserDiv">
                    <div style=""><b><g:message code="nameLbl"/>: </b>{{signedDocument.fromUser}}</div>
                    <div style=""><b><g:message code="IBANLbl"/>: </b>{{signedDocument.fromUserIBAN}}</div>
                </div>
            </div>
            <div style="margin:20px 0px 0px 20px;display:{{isReceptorVisible?'block':'none'}}">
                <div style="font-size: 1.2em; text-decoration: underline;font-weight: bold;">{{receptorLbl}}</div>
                <div layout horizontal>
                    <div><b><g:message code="IBANLbl"/>: </b></div>
                    <div layout vertical>
                        <template repeat="{{IBAN in signedDocument.toUserIBAN}}">
                            <div on-click="{{showToUserIBAN}}" style="text-decoration: underline; color: #0000ee; cursor: pointer;">{{IBAN}}</div>
                        </template>
                    </div>
                </div>
            </div >


            <template if="{{signedDocument.operation == 'VICKET'}}">
                <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold;margin:5px 0 0 0;color: #621">
                    {{receptorLbl}}</div>
                <div><b>{{toUserName}}</b></div>
                <div layout horizontal>
                    <div><b><g:message code="IBANLbl"/>: </b>{{IBAN}}</div>
                </div>
            </template>



            <template if="{{signedDocument.tags.length > 0}}">
                <div layout horizontal center center-justified style="margin: 15px 0 0 0;">
                    <template repeat="{{tag in signedDocument.tags}}">
                        <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag.name}}</a>
                    </template>
                </div>
            </template>
        </div>
        <template if="{{isClientToolConnected}}">
            <div layout horizontal style="margin:0px 20px 0px 0px;">
                <div flex></div>
                <div style="margin:10px 0px 10px 0px;">
                    <votingsystem-button on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-certificate" style="margin:0 5px 0 2px;"></i>  <g:message code="checkSignatureLbl"/>
                    </votingsystem-button>
                </div>
            </div>
        </template>
        <votingsystem-dialog id="xDialog" style="top: 30px; left:100px;" on-core-overlay-open="{{onCoreOverlayOpen}}"  title="<g:message code="userLbl"/>">
            <uservs-data id="uservsData"></uservs-data>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('message-smime', {
            publish: {
                signedDocument: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
            receptorLbl:null,
            caption:null,
            isReceptorVisible:true,
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('votingsystem-clienttoolconnected', function() {
                    this.isClientToolConnected = true
                }.bind(this))
            },
            attached: function () {
                console.log(this.tagName + " - attached")
                this.fire('attached', null);
            },
            signedDocumentChanged:function() {
                this.messageToUser = null
                this.isReceptorVisible = true
                if(this.signedDocument.toUserIBAN != null && this.signedDocument.toUserIBAN.length > 1) {
                    this.receptorLbl = '<g:message code="receptorsLbl"/>'
                } else this.receptorLbl = '<g:message code="receptorLbl"/>'
                switch (this.signedDocument.operation) {
                    case 'FROM_BANKVS':
                        this.caption = "<g:message code="transactionVSFromBankVS"/>"
                        break;
                    case 'FROM_GROUP_TO_ALL_MEMBERS':
                        this.isReceptorVisible = false
                        this.caption = "<g:message code="transactionVSFromGroupToAllMembers"/>"
                        break;
                    case 'FROM_GROUP_TO_MEMBER':
                        this.caption = "<g:message code="transactionVSFromGroupToMember"/>"
                        break;
                    case 'FROM_GROUP_TO_MEMBER_GROUP':
                        this.caption = "<g:message code="transactionVSFromGroupToMemberGroup"/>"
                        break;
                    case 'VICKET_INIT_PERIOD':
                        this.caption = "<g:message code="vicketInitPeriodLbl"/>"
                        this.$.fromUserDiv.innerHTML = "<g:message code="systemLbl"/>"
                        this.$.fromUserDiv.classList.add("pageHeader");
                        break;
                    case 'VICKET_REQUEST':
                        this.caption = "<g:message code="vicketRequestLbl"/>"
                        this.signedDocument.amount = this.signedDocument.totalAmount
                        break;
                    case 'VICKET':
                    case 'VICKET_SEND':
                        this.caption = "<g:message code="anonymousTransactionVSLbl"/>"
                        this.$.fromUserDivContainer.style.display = 'none'
                        this.isReceptorVisible = false
                        this.IBAN = this.signedDocument.toUserIBAN
                        this.toUserName = this.signedDocument.toUserName
                        this.receptorLbl = '<g:message code="receptorLbl"/>'
                        break;
                    default:
                        this.caption = this.signedDocument.operation

                }
                console.log("========= this.signedDocument: " + JSON.stringify(this.signedDocument))
// {"totalAmount":"10","fromUserVS":{"nif":"07553172H","name":"Name7553172H"},"serverURL":"http://vickets:8086/Vickets",
// "tagVS":"HIDROGENO","UUID":"de52635d-7357-49bc-b7d1-5ea58687fbef","operation":"VICKET_REQUEST","currencyCode":"EUR"}
            },
            showToUserInfo:function(e) {
                var groupURL = "${createLink(uri:'/groupVS')}/" + e.target.templateInstance.model.signedDocument.toUserVS.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = "${createLink(uri:'/groupVS')}/" +  e.target.templateInstance.model.signedDocument.fromUserVS.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showInfoIBAN:function(e) {
                var fromUserIBANInfoURL = "${createLink(uri:'/IBAN')}/from/" + e.target.templateInstance.model.signedDocument.fromUserVS.sender.fromUserIBAN
                console.log(this.tagName + " - showInfoIBAN - fromUserIBANInfoURL: " + fromUserIBANInfoURL)
            },
            showToUserIBAN:function(e) {
                console.log(this.tagName + " - showToUserIBAN - " + e)
                loadURL_VS("${createLink( controller:'userVS')}/IBAN/" + e.target.templateInstance.model.IBAN)
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_SMIME)
                webAppMessage.message = this.smimeMessage
                webAppMessage.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>