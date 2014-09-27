<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-form']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-page-tabs']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-item', file: 'core-item.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-selector', file: 'core-selector.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dropdown-menu', file: 'paper-dropdown-menu.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-user']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">

<%
    def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
    def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
%>
<polymer-element name="groupvs-details" attributes="selectedItem subpage">
<template>
    <style shim-shadowdom>
        body /deep/ paper-dropdown-menu.narrow { max-width: 200px; width: 300px; }
        .optionsIcon {margin:0 7px 0 3px; color:#6c0404;}
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
                <div style="margin: 20px 0 0 0;" title="<g:message code="backLbl"/>" >
                    <paper-fab icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                </div>
            </template>

            <div layout vertical flex>
                <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
                </div>

                <div style="display:{{isAdminView && isClientToolConnected? 'block':'none'}}">
                    <div layout horizontal center center-justified style="margin:0 0 20px 0;">
                        <div layout horizontal center center-justified>
                            <i class="fa fa-cogs optionsIcon"></i>
                            <paper-dropdown-menu id="configGroupDropDown" valueattr="label"
                                         label="<g:message code="configGroupvsLbl"/>" style="width: 200px;">
                                <core-selector target="{{$.groupOptions}}" valueattr="id" on-core-select="{{configGroup}}">
                                    <div id="groupOptions" style="padding:0 10px 0 10px;">
                                        <core-item id="editGroup" label="<g:message code="editDataLbl"/>"></core-item>
                                        <core-item id="cancelGroup" label="<g:message code="cancelGroupVSLbl"/>"></core-item>
                                    </div>
                                </core-selector>
                            </paper-dropdown-menu>
                        </div>

                        <div layout horizontal center center-justified style="margin:0 0 0 60px;">
                            <i class="fa fa-money optionsIcon"></i>
                            <paper-dropdown-menu id="selectTransactionVSDropDown" valueattr="label"
                                             label="<g:message code="makeTransactionVSFromGroupVSLbl"/>" style="width: 300px;">
                                <core-selector target="{{$.transactionvsOptions}}" valueattr="id" on-core-select="{{showTransactionVSDialog}}">
                                    <div id="transactionvsOptions" style="padding:0 10px 0 10px;">
                                        <core-item id="fromGroupToMember" label="<g:message code="makeTransactionVSFromGroupVSToMemberLbl"/>"></core-item>
                                        <core-item id="fromGroupToMemberGroup" label="<g:message code="makeTransactionVSFromGroupVSToMemberGroupLbl"/>"></core-item>
                                        <core-item id="fromGroupToAllMember" label="<g:message code="makeTransactionVSFromGroupVSToAllMembersLbl"/>"></core-item>
                                    </div>
                                </core-selector>
                            </paper-dropdown-menu>
                        </div>
                    </div>
                </div>

                <div layout horizontal center center-justified style="margin:10px 0px 0px 30px;display:{{isUserView?'block':'none'}}">
                    <votingsystem-button on-click="{{subscribeToGroup}}">
                        <g:message code="subscribeGroupVSLbl"/> <i class="fa fa-sign-in"></i>
                    </votingsystem-button>
                    <votingsystem-button on-click="{{subscribeToGroup}}">
                        <g:message code="makeTransactionVSLbl"/> <i class="fa fa-money"></i>
                    </votingsystem-button>
                </div>

                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;" groupvsId-data="{{groupvs.userVS.id}}">{{groupvs.userVS.name}}</div>
                    </div>
                    <div id="tagsDiv" style="padding:7px 0px 0px 7px; display:{{groupvs.userVS.tags.length > 0?'block':'none'}}">
                        <div style="font-size: 0.9em; font-weight: bold;color:#888;"><g:message code='tagsLbl'/></div>
                        <div layout horizontal>
                            <template repeat="{{tag in groupvs.userVS.tags}}">
                                <a class="btn btn-default" style="font-size: 0.7em; margin:0px 5px 0px 0px;padding:3px;">{{tag.name}}</a>
                            </template>
                        </div>
                    </div>
                </div>
                <div layout horizontal>
                    <div id="" style="margin:0 0 0 0; font-size: 0.75em; color:#888;">
                        <b><g:message code="representativeLbl"/>: </b>{{groupvs.userVS.representative.firstName}} {{groupvs.userVS.representative.lastName}}
                    </div>
                    <div flex></div>
                    <div id="" style="margin:0 0 0 0; font-size: 0.75em; color:#888;">
                        <b><g:message code="IBANLbl"/>: </b>{{groupvs.userVS.IBAN}}
                    </div>
                </div>
            </div>
        </div>

        <div class="eventContentDiv" style="">
            <votingsystem-html-echo html="{{groupvs.userVS.description}}"></votingsystem-html-echo>
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
            <transactionvs-form id="transactionvsForm" subpage></transactionvs-form>
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
            this.$.transactionvsForm.addEventListener('operation-finished', function (e) {
                this.page = 0;
            }.bind(this))
        },
        messagedialog:function(e, detail, sender) {
            console.log("messagedialog signal - cancelgroup: " + detail)
            if('cancel_group' == detail) {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_CANCEL)
                webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'cancel',absolute:true)}/" + this.groupvs.userVS.id
                webAppMessage.signedMessageSubject = "<g:message code="cancelGroupVSSignedMessageSubject"/>"
                webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_CANCEL, groupvsName:this.groupvs.userVS.name,
                    id:this.groupvs.userVS.id}
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                webAppMessage.setCallback(function(appMessage) {
                    this.appMessageJSON = JSON.parse(appMessage)
                    if(this.appMessageJSON != null) {
                        var caption = '<g:message code="groupCancelERRORLbl"/>'
                        if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                            caption = "<g:message code='groupCancelOKLbl'/>"
                        }
                        showMessageVS(this.appMessageJSON.message, caption, this.tagName)
                    }
                }.bind(this))
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
            var groupvsRepresentative = {id:this.groupvs.userVS.representative.id, nif:this.groupvs.userVS.representative.nif}
            var groupVSData = {id:this.groupvs.userVS.id, name:this.groupvs.userVS.name , representative:groupvsRepresentative}
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_SUBSCRIBE)
            webAppMessage.serviceURL = "${createLink( controller:'groupVS', absolute:true)}/" + this.groupvs.userVS.id + "/subscribe"
            webAppMessage.signedMessageSubject = "<g:message code="subscribeToVicketGroupMsg"/>"
            webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_SUBSCRIBE, groupvs:groupVSData}
            webAppMessage.contentType = 'application/x-pkcs7-signature'
            webAppMessage.setCallback(function(appMessage) {
                console.log("subscribeToGroupCallback - message: " + appMessage);
                var appMessageJSON = JSON.parse(appMessage)
                var caption
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) caption = "<g:message code='groupSubscriptionOKLbl'/>"
                else caption = '<g:message code="groupSubscriptionERRORLbl"/>'
                var msg = appMessageJSON.message
                showMessageVS(msg, caption)
            }.bind(this))
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        },
        groupvsChanged:function() {
            if(("admin" == menuType || "superadmin" == menuType) && 'ACTIVE' == this.groupvs.userVS.state) this.isAdminView = true
            else {
                this.isAdminView = false
                if("user" == menuType && 'ACTIVE' == this.groupvs.userVS.state) this.isUserView = true
                else this.isUserView = false
            }
            this.$.groupTabs.groupvs = this.groupvs
            if('ACTIVE' == this.groupvs.userVS.state) {

            } else if('PENDING' == this.groupvs.userVS.state) {
                this.$.pageHeader.style.color = "#fba131"
                this.$.messagePanel.classList.add("groupvsPendingBox");
                this.$.messagePanel.innerHTML = "<g:message code="groupvsPendingLbl"/>"
                this.$.messagePanel.style.display = 'block'
            } else if('CANCELLED' == this.groupvs.userVS.state) {
                this.$.pageHeader.style.color = "#6c0404"
                this.$.messagePanel.classList.add("groupvsClosedBox");
                this.$.messagePanel.innerHTML = "<g:message code="groupvsClosedLbl"/>"
                this.$.messagePanel.style.display = 'block'
                this.isAdminView = false
            }
        },
        configGroup:function(e) {
            //e.detail.isSelected = false
            if('cancelGroup' == e.detail.item.id) {
                showMessageVS("<g:message code="cancelGroupVSDialogMsg"/>".format(this.groupvs.userVS.name),
                        "<g:message code="confirmOperationMsg"/>", 'cancel_group', true)
            }else if('editGroup' == e.detail.item.id) {
                var editorURL = "${createLink( controller:'groupVS', action:'edit', absolute:true)}/" + this.groupvs.userVS.id + "?menu=admin"
                this.fire('core-signal', {name: "innerpage", data: editorURL});
            }
            this.$.configGroupDropDown.selected = ""
            this.$.configGroupDropDown.selectedItem = null
        },
        showTransactionVSDialog:function(e) {
            console.log("showTransactionVSDialog")
            //e.detail.isSelected
            if('fromGroupToMember' == e.detail.item.id) {
                this.$.transactionvsForm.init(Operation.TRANSACTIONVS_FROM_GROUP_TO_MEMBER, this.groupvs.userVS.name,
                        this.groupvs.userVS.IBAN , this.groupvs.userVS.id)
            } else if('fromGroupToMemberGroup' == e.detail.item.id) {
                this.$.transactionvsForm.init(Operation.TRANSACTIONVS_FROM_GROUP_TO_MEMBER_GROUP, this.groupvs.userVS.name,
                        this.groupvs.userVS.IBAN, this.groupvs.userVS.id)
            } else if('fromGroupToAllMember' == e.detail.item.id) {
                this.$.transactionvsForm.init(Operation.TRANSACTIONVS_FROM_GROUP_TO_ALL_MEMBERS, this.groupvs.userVS.name,
                        this.groupvs.userVS.IBAN, this.groupvs.userVS.id)
            }
            this.page = 1;
            this.$.selectTransactionVSDropDown.selected = ""
            this.$.selectTransactionVSDropDown.selectedItem = null
        },
        back:function() {
            this.fire('core-signal', {name: "groupvs-details-closed", data: this.groupvs.userVS.id});
        },
        showUserDetails:function(e, detail, sender) {
            console.log(this.tagName + " - showUserDetails")
            this.$.userDescription.show("${createLink(controller: 'groupVS')}/" + this.groupvs.userVS.id + "/user", detail)
        }
    })
</script>
</polymer-element>