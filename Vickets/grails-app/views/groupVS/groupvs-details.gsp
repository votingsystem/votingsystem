<link rel="import" href="${resource(dir: '/bower_components/paper-menu-button', file: 'paper-menu-button.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-deposit-dialog.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-page-tabs.gsp']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-selector', file: 'core-selector.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-ripple', file: 'paper-ripple.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/groupvs-user.gsp']"/>">

<%
    def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
    def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
    def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
%>
<polymer-element name="groupvs-details" attributes="selectedItem subpage">
<template>
    <style shim-shadowdom>
    .view { :host {position: relative;} }
    </style>
    <g:include view="/include/styles.gsp"/>
    <core-signals on-core-signal-messagedialog-accept="{{messagedialog}}" on-core-signal-messagedialog-closed="{{messagedialogClosed}}"
                  on-core-signal-uservs-selected="{{showUserDetails}}" ></core-signals>

    <div class="pageContentDiv" style="max-width: 1000px; margin:0px auto 0px auto;"  cross-fade>
        <div layout horizontal center center-justified>
            <template if="{{subpage}}">
                <votingsystem-button isFab on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                    <i class="fa fa-arrow-left"></i></votingsystem-button>
            </template>

            <div layout vertical flex>
                <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
                </div>
                <div layout horizontal center center-justified style="margin:10px 0px 0px 30px; display:{{isAdminView?'block':'none'}}">

                    <paper-menu-button id="groupConfigOptions" valign="bottom" style="width: 0px;padding:0px;">
                        <core-selector target="{{$.groupOptions}}" id="groupOptionsSelector" valueattr="id" on-core-select="{{configGroup}}">
                            <div id="groupOptions" style=" border: 1px solid #6c0404;">
                                <paper-item id="editGroup" label="<g:message code="editDataLbl"/>">
                                    <div flex></div><i class="fa fa-pencil-square-o"></i></paper-item>
                                <paper-item id="cancelGroup" label="<g:message code="cancelGroupVSLbl"/>">
                                    <div flex></div><i class="fa fa-trash-o"></i></paper-item>
                            </div>
                        </core-selector>
                    </paper-menu-button>

                    <template if="{{isClientToolConnected}}">
                        <votingsystem-button on-click="{{openConfigGroupOptions}}">
                            <g:message code="configGroupvsLbl"/> <i class="fa fa-cogs"></i>
                        </votingsystem-button>
                    </template>


                    <paper-menu-button id="selectDepositOptions" valign="bottom" style="width: 0px;padding:0px;">
                        <core-selector target="{{$.depositOptions}}" id="coreSelector" valueattr="id" on-core-select="{{showDepositDialog}}">
                            <div id="depositOptions" style=" border: 1px solid #6c0404;">
                                <paper-item id="fromGroupToMember" label="<g:message code="makeDepositFromGroupVSToMemberLbl"/>"></paper-item>
                                <paper-item id="fromGroupToMemberGroup" label="<g:message code="makeDepositFromGroupVSToMemberGroupLbl"/>"></paper-item>
                                <paper-item id="fromGroupToAllMember" label="<g:message code="makeDepositFromGroupVSToAllMembersLbl"/>"</paper-item>
                            </div>
                        </core-selector>
                    </paper-menu-button>
                    <votingsystem-button on-click="{{openDepositDialogOptions}}">
                        <g:message code="makeDepositFromGroupVSLbl"/> <i class="fa fa-money"></i>
                    </votingsystem-button>
                </div>

                <div layout horizontal center center-justified style="margin:10px 0px 0px 30px;display:{{isUserView?'block':'none'}}">
                    <votingsystem-button on-click="{{subscribeToGroup}}">
                        <g:message code="subscribeGroupVSLbl"/> <i class="fa fa-sign-in"></i>
                    </votingsystem-button>
                    <votingsystem-button on-click="{{subscribeToGroup}}">
                        <g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
                    </votingsystem-button>
                </div>

                <div layout horizontal center center-justified style="margin:5px 0px 5px 0px;">
                    <div flex id="pageHeader" class="pageHeader text-center">
                        <h3 groupvsId-data="{{groupvs.id}}">{{groupvs.name}}</h3>
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
            </div>
        </div>

        <div class="eventContentDiv" style="">
            <votingsystem-html-echo html="{{groupvs.description}}"></votingsystem-html-echo>
        </div>
        <div layout horizontal style="width:1000px;">
            <div id="" style="margin:0px 40px 0px 0px; font-size: 0.75em; float:right; color:#888;">
                <b><g:message code="groupRepresentativeLbl"/>: </b>{{groupvs.representative.firstName}} {{groupvs.representative.lastName}}
            </div>
            <div id="" style="margin:0px 30px 0px 10px; font-size: 0.75em; color:#888;">
                <b><g:message code="IBANLbl"/>: </b>{{groupvs.IBAN}}
            </div>
        </div>

        <div style="border-top: 1px solid #ccc; display:block; padding-top: 15px;margin-top: 25px; min-height: 300px;">
            <div class="text-center" style="font-size: 1.2em;font-weight: bold; color:#6c0404; padding: 0px 0 0 0; ">
                <!--<g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom, weekTo]}"/>-->
                <g:message code="weekMovementsLbl"/>
            </div>

            <group-page-tabs id="groupTabs" groupvs="{{groupvs}}" style=""></group-page-tabs>
        </div>
        <template if="{{!isClientToolConnected}}">
            <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;
            display:{{(!isAdminView && !isUserView) ? 'block':'none'}}">
                <g:message code="clientToolNeededMsg"/>.
                <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/>
            </div>
        </template>
    </div>

    <groupvs-user id="userDescription"></groupvs-user>

    <vicket-deposit-dialog id="depositDialog"></vicket-deposit-dialog>

</template>
<script>
    Polymer('groupvs-details', {
        isSelected: false,
        subpage:false,
        publish: {
            groupvs: {value: {}}
        },
        ready :  function() {
            console.log(this.tagName + " - ready - subpage: " + this.subpage)
            this.isClientToolConnected = window['isClientToolConnected']
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
                if(appMessageJSON != null) {
                    var caption
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) caption = "<g:message code='groupSubscriptionOKLbl'/>"
                    else caption = '<g:message code="groupSubscriptionERRORLbl"/>'
                    var msg = appMessageJSON.message
                    showMessageVS(msg, caption)
                }
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
            if(e.detail.isSelected) {
                if('fromGroupToMember' == e.detail.item.id) {
                    this.$.depositDialog.show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER, this.groupvs.name, this.groupvs.IBAN,
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', this.groupvs.id)
                } else if('fromGroupToMemberGroup' == e.detail.item.id) {
                    this.$.depositDialog.show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP, this.groupvs.name, this.groupvs.IBAN,
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', this.groupvs.id)
                } else if('fromGroupToAllMember' == e.detail.item.id) {
                    this.$.depositDialog.show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS, this.groupvs.name, this.groupvs.IBAN,
                            '${formatDate(date:currentWeekPeriod.getDateTo(), format:"yyyy/MM/dd HH:mm:ss")}', this.groupvs.id)
                }
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