<%@ page import="org.votingsystem.model.TypeVS" %>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">

<polymer-element name="transactionvs-data" attributes="transactionvs smimeMessage isClientToolConnected timeStampDate">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic;  }
        .IBANLink{ text-decoration: underline; color: #0000ee; cursor: pointer; }
        </style>
        <div layout vertical style="margin: 0px auto; max-width:800px; min-width: 300px; min-height: 200px;">

            <div class="pageHeader"  layout horizontal center center-justified
                 style="margin:0 0 10px 0;font-size: 1.2em;">{{caption}}
            </div>
            <template if="{{transactionvs.tags.length > 0}}">
                <core-tooltip label="<g:message code="tagLbl"/>" position="top">
                    <div layout horizontal center center-justified style="margin: 3px 0 0 0;">
                        <div style="margin:0 10px 0 0; color: #888;"><i class="fa fa-tag"></i></div>
                        <template repeat="{{tag in transactionvs.tags}}">
                            <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 5px 0px;padding:3px;">{{tag.name}}</a>
                        </template>
                    </div>
                </core-tooltip>
            </template>

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
            <div style=""><b><g:message code="subjectLbl"/>: </b>{{transactionvs.subject}}</div>

            <div style=""><b><g:message code="amountLbl"/>: </b>{{transactionvs.amount}} {{transactionvs.currency}}</div>

            <div horizontal layout>
                <div style=""><b><g:message code="dateLbl"/>: </b>{{transactionvs.dateCreated}}</div>
                <template if="{{transactionvs.validTo}}">
                    <div style="margin: 0 0 0 15px;"><b><g:message code="validToLbl"/>: </b>{{transactionvs.validTo}}</div>
                </template>
            </div>

            <template if="{isSenderVisible}}">
                <div style="margin-left: 20px;">
                    <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold; margin:10px 0px 0px 0px;color: #621;">
                        <g:message code="senderLbl"/></div>
                    <div id="fromUserDiv">
                        <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs | getFromUserName}}</div>
                        <div on-click="{{showFromUserIBAN}}"  class="IBANLink"><b><g:message code="IBANLbl"/>: </b>{{transactionvs | getFromUserIBAN}}</div>
                    </div>
                </div>
            </template>

            <div style="margin:20px 0px 0px 20px;display:{{isReceptorVisible || receptorMsg ?'block':'none'}}">
                <div style="font-size: 1.1em; text-decoration: underline;font-weight: bold;color: #621">{{receptorLbl}}</div>
                <div>
                    <template if="{{!receptorMsg}}">
                        <div style=""><b><g:message code="nameLbl"/>: </b>{{transactionvs.toUserVS.name}}</div>
                        <div on-click="{{showToUserIBAN}}" class="IBANLink"><b><g:message code="IBANLbl"/>: </b>{{transactionvs.toUserVS.IBAN}}</div>
                    </template>
                    <template if="{{receptorMsg}}">{{receptorMsg}}</template>
                </div>
            </div >
        </div>
        <template if="{{isClientToolConnected}}">
            <div layout horizontal style="margin:15px 20px 0px 0px;">
                <div flex></div>
                <div style="margin:10px 0px 10px 0px;">
                    <votingsystem-button on-click="{{checkReceipt}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-certificate" style="margin:0 5px 0 2px;"></i>  <g:message code="checkSignatureLbl"/>
                    </votingsystem-button>
                </div>
            </div>
        </template>
    </template>
    <script>
        Polymer('transactionvs-data', {
            publish: {
                transactionvs: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
            receptorLbl:null,
            caption:null,
            isReceptorVisible:true,
            isSenderVisible:true,
            receptorMsg:null,
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('votingsystem-clienttoolconnected', function() {
                    this.isClientToolConnected = true
                }.bind(this))
            },
            getFromUserName: function (transactionvs) {
                var result
                if(transactionvs.fromUserVS) {
                    if(transactionvs.fromUserVS.sender.fromUser != null) result = transactionvs.fromUserVS.sender.fromUser
                    else result = transactionvs.fromUserVS.name
                }
                return result
            },
            getFromUserIBAN: function (transactionvs) {
                var result
                if(transactionvs.fromUserVS) {
                    if(transactionvs.fromUserVS.sender.fromUserIBAN != null) result = transactionvs.fromUserVS.sender.fromUserIBAN
                    else result = transactionvs.fromUserVS.IBAN
                }
                return result
            },
            transactionvsChanged:function() {
                this.messageToUser = null
                this.receptorMsg = null
                this.isReceptorVisible = true
                this.isReceptorVisible = true
                if(this.transactionvs.toUserIBAN != null && this.transactionvs.toUserIBAN.length > 1) {
                    this.receptorLbl = '<g:message code="receptorsLbl"/>'
                } else this.receptorLbl = '<g:message code="receptorLbl"/>'
                console.log(this.tagName + " - transactionvsChanged - transactionvs: " + JSON.stringify(this.transactionvs))
                switch (this.transactionvs.type) {
                    case 'FROM_BANKVS':
                        this.caption = "<g:message code="transactionVSFromBankVS"/>"
                        break;
                    case 'FROM_GROUP_TO_ALL_MEMBERS':
                        this.isReceptorVisible = false
                        this.caption = "<g:message code="transactionVSFromGroupToAllMembers"/>"
                        this.receptorMsg = "<g:message code="transactionVSGroupVSReceptorsMsg"/>".format(
                                this.transactionvs.numChildTransactions)
                        break;
                    case 'FROM_GROUP_TO_MEMBER':
                        this.caption = "<g:message code="transactionVSFromGroupToMember"/>"
                        break;
                    case 'TRANSACTIONVS_FROM_GROUP_TO_MEMBER_GROUP':
                        this.caption = "<g:message code="transactionVSFromGroupToMemberGroup"/>"
                        break;
                    case 'VICKET_INIT_PERIOD':
                        this.caption = "<g:message code="vicketInitPeriodLbl"/>"
                        this.$.fromUserDiv.innerHTML = "<g:message code="systemLbl"/>"
                        break;
                    case 'VICKET_SEND':
                        this.caption = "<g:message code="vicketSendLbl"/>"
                        this.isSenderVisible = false
                        break;
                    case 'VICKET_REQUEST':
                        this.caption = "<g:message code="vicketRequestLbl"/>"
                        this.isSenderVisible = false
                        this.isReceptorVisible = false
                        break;
                    case 'FROM_BANKVS':
                        this.caption = "<g:message code="transactionVSFromBankVS"/>"
                        break;
                    default:
                        this.caption = this.transactionvs.type

                }
            },
       // {"fromUserVS":{"nif":null,"name":"Grupo sab 29 - 16:44","type":"GROUP","id":7,"sender":{"fromUserIBAN":"ES0878788989450000000007","fromUser":null}},
       // "dateCreated":"27 sep 2014 21:16","validTo":"29 sep 2014 00:00","id":5,"subject":"Asunto sábado españa - 21:16","type":"FROM_GROUP_TO_MEMBER","amount":"200.00",
       // "currency":"EUR","messageSMIMEURL":"http://vickets:8086/Vickets/messageSMIME/35","numChildTransactions":1,"tags":[{"id":3,"name":"HIDROGENO"}]}
        showToUserInfo:function(e) {
                var groupURL = "${createLink(uri:'/groupVS')}/" + e.target.templateInstance.model.transactionvs.toUserVS.id
                console.log(this.tagName + "- showToUserInfo - groupURL: " + groupURL)
            },
            showFromUserInfo:function(group) {
                var groupURL = "${createLink(uri:'/groupVS')}/" +  e.target.templateInstance.model.transactionvs.fromUserVS.id
                console.log(this.tagName + "- showFromUserInfo - groupURL: " + groupURL)
            },
            showFromUserIBAN:function(e) {
                var serviceURL =  "${createLink( controller:'userVS')}/IBAN/" + this.getFromUserIBAN(e.target.templateInstance.model.transactionvs)
                window.open(serviceURL, '_blank');
            },
            showToUserIBAN:function(e) {
                var serviceURL =  "${createLink( controller:'userVS')}/IBAN/" + this.transactionvs.toUserVS.IBAN
                window.open(serviceURL, '_blank');
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.OPEN_RECEIPT)
                if(this.smimeMessage == null) {
                    webAppMessage.serviceURL = this.transactionvs.messageSMIMEURL
                    webAppMessage.operation = Operation.OPEN_RECEIPT_FROM_URL
                } else webAppMessage.message = this.smimeMessage
                webAppMessage.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage);
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>