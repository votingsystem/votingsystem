<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-menu-button', file: 'paper-menu-button.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/vicket-deposit-form']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-page-tabs']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-selector', file: 'core-selector.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-ripple', file: 'paper-ripple.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-user']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">

<%
    def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
    def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
    def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
%>
<polymer-element name="groupvs-details" attributes="selectedItem subpage">
<template>
    <style shim-shadowdom>
    .view { :host {position: relative;} }
    .menuButton #menu{
        overflow: auto;
        background: white;
        padding: 0px;
        border: #6c0404;
    }
    </style>
    <g:include view="/include/styles.gsp"/>
    <core-signals on-core-signal-messagedialog-accept="{{messagedialog}}" on-core-signal-messagedialog-closed="{{messagedialogClosed}}"
                  on-core-signal-uservs-selected="{{showUserDetails}}" ></core-signals>
    <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
         transitions="cross-fade-all">
    <section id="page1">
    <div class="pageContentDiv" style="max-width: 1000px; min-width:800px; margin:0px auto 0px auto;"  cross-fade>
        <div layout horizontal center center-justified>
            <template if="{{subpage}}">
                <votingsystem-button isFab on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                    <i class="fa fa-arrow-left"></i></votingsystem-button>
            </template>

            <div layout vertical flex>
                <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
                </div>

                <div style="display:{{isAdminView && isClientToolConnected? 'block':'none'}}">
                    <div layout horizontal center center-justified style="margin:0 0 0 0;">
                        <div id="groupConfigOptionsViv">
                            <paper-menu-button id="groupConfigOptions" class="menuButton" valign="bottom" style="width: 0px;padding:0px;">
                                <core-selector target="{{$.groupOptions}}" id="groupOptionsSelector" valueattr="id" on-core-select="{{configGroup}}">
                                    <div id="groupOptions" style=" border: 1px solid #6c0404;">
                                        <paper-item id="editGroup" label="<g:message code="editDataLbl"/>">
                                            <div flex></div><i class="fa fa-pencil-square-o"></i></paper-item>
                                        <paper-item id="cancelGroup" label="<g:message code="cancelGroupVSLbl"/>">
                                            <div flex></div><i class="fa fa-trash-o"></i></paper-item>
                                    </div>
                                </core-selector>
                            </paper-menu-button>
                            <votingsystem-button on-click="{{openConfigGroupOptions}}">
                                <i class="fa fa-cogs" style="margin:0 7px 0 3px;"></i> <g:message code="configGroupvsLbl"/>
                            </votingsystem-button>
                        </div>

                        <div id="selectDepositOptionsViv">
                            <paper-menu-button id="selectDepositOptions" class="menuButton" valign="bottom" style="width: 0px;padding:0px;">
                                <core-selector target="{{$.depositOptions}}" id="coreSelector" valueattr="id" on-core-select="{{showDepositDialog}}">
                                    <div id="depositOptions" style=" border: 1px solid #6c0404;">
                                        <paper-item id="fromGroupToMember" label="<g:message code="makeDepositFromGroupVSToMemberLbl"/>"></paper-item>
                                        <paper-item id="fromGroupToMemberGroup" label="<g:message code="makeDepositFromGroupVSToMemberGroupLbl"/>"></paper-item>
                                        <paper-item id="fromGroupToAllMember" label="<g:message code="makeDepositFromGroupVSToAllMembersLbl"/>"</paper-item>
                                    </div>
                                </core-selector>
                            </paper-menu-button>
                            <votingsystem-button on-click="{{openDepositDialogOptions}}">
                                <i class="fa fa-money" style="margin:0 7px 0 3px;"></i>  <g:message code="makeDepositFromGroupVSLbl"/>
                            </votingsystem-button>
                        </div>

                    </div>
                </div>

                <div layout horizontal center center-justified style="margin:10px 0px 0px 30px;display:{{isUserView?'block':'none'}}">
                    <votingsystem-button on-click="{{subscribeToGroup}}">
                        <g:message code="subscribeGroupVSLbl"/> <i class="fa fa-sign-in"></i>
                    </votingsystem-button>
                    <votingsystem-button on-click="{{subscribeToGroup}}">
                        <g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
                    </votingsystem-button>
                </div>

                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;" groupvsId-data="{{groupvs.id}}">{{groupvs.name}}</div>
                    </div>
                    <div id="tagsDiv" style="padding:7px 0px 0px 7px; display:{{groupvs.tags.length > 0?'block':'none'}}">
                        <div style="font-size: 0.9em; font-weight: bold;color:#888;"><g:message code='tagsLbl'/></div>
                        <div layout horizontal>
                            <template repeat="{{tag in groupvs.tags}}">
                                <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 0px 0px;padding:3px;">{{tag.name}}</a>
                            </template>
                        </div>
                    </div>
                </div>
                <div layout horizontal>
                    <div id="" style="margin:0 0 0 0; font-size: 0.75em; color:#888;">
                        <b><g:message code="representativeLbl"/>: </b>{{groupvs.representative.firstName}} {{groupvs.representative.lastName}}
                    </div>
                    <div flex></div>
                    <div id="" style="margin:0 0 0 0; font-size: 0.75em; color:#888;">
                        <b><g:message code="IBANLbl"/>: </b>{{groupvs.IBAN}}
                    </div>
                </div>
            </div>
        </div>

        <div class="eventContentDiv" style="">
            <votingsystem-html-echo html="{{groupvs.description}}"></votingsystem-html-echo>
        </div>

        <div style="margin: 25px 0 0 0; min-height: 300px;">
            <div  style="text-align:center; font-size: 1.2em;font-weight: bold; color: #888;margin: 0 0 10px 0;">
                <g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom]}"/>
            </div>

            <group-page-tabs id="groupTabs" groupvs="{{groupvs}}" style=""></group-page-tabs>
        </div>
        <template if="{{!isClientToolConnected}}">
            <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
                <g:message code="clientToolNeededMsg"/>.
                <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/>
            </div>
        </template>
    </div>
    </section>

    <section id="page2">
        <div class="pageContentDiv" cross-fade>
            <vicket-deposit-form id="depositForm" subpage></vicket-deposit-form>
        </div>
    </section>
    </core-animated-pages>

    <groupvs-user id="userDescription"></groupvs-user>

</template>
<script>
    Polymer('groupvs-details', {
        isSelected: false,
        subpage:false,
        publish: {
            groupvs: {value: {}}
        },
        isClientToolConnected:false,
        ready :  function() {
            console.log(this.tagName + " - ready - subpage: " + this.subpage)
            //this.isClientToolConnected = window['isClientToolConnected']
            this.isClientToolConnected = true
            window.onclick = function(event){
                this.$.selectDepositOptions.opened = false
                this.$.groupConfigOptions.opened = false
            }.bind(this)
            this.$.groupConfigOptionsViv.onclick = function(event){
                event.stopPropagation();
            }
            this.$.selectDepositOptionsViv.onclick = function(event){
                event.stopPropagation();
            }
            this.$.depositForm.addEventListener('operation-finished', function (e) {
                this.page = 0;
            }.bind(this))
        },
        messagedialog:function(e, detail, sender) {
            console.log("messagedialog signal - cancelgroup: " + detail)
            if('cancel_group' == detail) {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_CANCEL)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'cancel',absolute:true)}/" + this.groupvs.id
                webAppMessage.signedMessageSubject = "<g:message code="cancelGroupVSSignedMessageSubject"/>"
                webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_CANCEL, groupvsName:this.groupvs.name, id:this.groupvs.id}
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    this.appMessageJSON = JSON.parse(appMessage)
                    if(this.appMessageJSON != null) {
                        var caption = '<g:message code="groupCancelERRORLbl"/>'
                        if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                            caption = "<g:message code='groupCancelOKLbl'/>"
                        }
                        showMessageVS(this.appMessageJSON.message, caption, this.tagName)
                    }
                }}

                webAppMessage.callerCallback = this.objectId
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                this.appMessageJSON = null
            }
        },
        messagedialogClosed:function(e) {
            console.log("messagedialog signal - messagedialogClosed: " + e)
            if(this.tagName == e) {
                if(this.appMessageJSON != null && ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                    window.location.href = updateMenuLink(this.appMessageJSON.URL)
                }
            }
        },
        subscribeToGroup: function () {
            console.log("subscribeToGroup")
            var groupvsRepresentative = {id:this.groupvs.representative.id, nif:this.groupvs.representative.nif}
            var groupVSData = {id:this.groupvs.id, name:this.groupvs.name , representative:groupvsRepresentative}
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_SUBSCRIBE)
            webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
            webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
            webAppMessage.serviceURL = "${createLink( controller:'groupVS', absolute:true)}/" + this.groupvs.id + "/subscribe"
            webAppMessage.signedMessageSubject = "<g:message code="subscribeToVicketGroupMsg"/>"
            webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_SUBSCRIBE, groupvs:groupVSData}
            webAppMessage.contentType = 'application/x-pkcs7-signature'
            var objectId = Math.random().toString(36).substring(7)
            window[objectId] =  {setClientToolMessage: function(appMessage) {
                console.log("subscribeToGroupCallback - message: " + appMessage);
                var appMessageJSON = JSON.parse(appMessage)
                var caption
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) caption = "<g:message code='groupSubscriptionOKLbl'/>"
                else caption = '<g:message code="groupSubscriptionERRORLbl"/>'
                var msg = appMessageJSON.message
                showMessageVS(msg, caption)
            }}
            webAppMessage.callerCallback = objectId
            webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        },
        groupvsChanged:function() {
            if(("admin" == menuType || "superadmin" == menuType) && 'ACTIVE' == this.groupvs.state) this.isAdminView = true
            else {
                this.isAdminView = false
                if("user" == menuType && 'ACTIVE' == this.groupvs.state) this.isUserView = true
                else this.isUserView = false
            }
            this.$.groupTabs.groupvs = this.groupvs
            if('ACTIVE' == this.groupvs.state) {

            } else if('PENDING' == this.groupvs.state) {
                this.$.pageHeader.style.color = "#fba131"
                this.$.messagePanel.classList.add("groupvsPendingBox");
                this.$.messagePanel.innerHTML = "<g:message code="groupvsPendingLbl"/>"
                this.$.messagePanel.style.display = 'block'
            } else if('CANCELLED' == this.groupvs.state) {
                this.$.pageHeader.style.color = "#6c0404"
                this.$.messagePanel.classList.add("groupvsClosedBox");
                this.$.messagePanel.innerHTML = "<g:message code="groupvsClosedLbl"/>"
                this.$.messagePanel.style.display = 'block'
                this.isAdminView = false
            }

        },
        configGroup:function(e) {
            if(e.detail.isSelected) {
                if('cancelGroup' == e.detail.item.id) {
                    showMessageVS("<g:message code="cancelGroupVSDialogMsg"/>".format(this.groupvs.name),
                            "<g:message code="confirmOperationMsg"/>", 'cancel_group', true)
                } else if('editGroup' == e.detail.item.id) {
                    var editorURL = "${createLink( controller:'groupVS', action:'edit', absolute:true)}/" + this.groupvs.id + "?menu=admin"
                    //var editorURL = "${createLink(controller: 'groupVS', action: 'newGroup')}"
                    this.fire('core-signal', {name: "innerpage", data: editorURL});
                }
                this.$.coreSelector.selected = null
            }
        },
        showDepositDialog:function(e) {
            console.log("showDepositDialog")
            if(e.detail.isSelected) {
                if('fromGroupToMember' == e.detail.item.id) {
                    this.$.depositForm.init(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER, this.groupvs.name, this.groupvs.IBAN,
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', this.groupvs.id)
                } else if('fromGroupToMemberGroup' == e.detail.item.id) {
                    this.$.depositForm.init(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP, this.groupvs.name, this.groupvs.IBAN,
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', this.groupvs.id)
                } else if('fromGroupToAllMember' == e.detail.item.id) {
                    this.$.depositForm.init(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS, this.groupvs.name, this.groupvs.IBAN,
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', this.groupvs.id)
                }

                this.page = 1;

                this.$.coreSelector.selected = null
            }
        },
        back:function() {
            this.fire('core-signal', {name: "groupvs-details-closed", data: this.groupvs.id});
        },
        openDepositDialogOptions:function() {
            this.$.selectDepositOptions.opened = true
        },
        openConfigGroupOptions:function() {
            this.$.groupConfigOptions.opened = true
        },
        showUserDetails:function(e, detail, sender) {
            console.log(this.tagName + " - showUserDetails")
            this.$.userDescription.show("${createLink(controller: 'groupVS')}/" + this.groupvs.id + "/user", detail)
        }
    })
</script>
</polymer-element>