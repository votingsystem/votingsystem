<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-page-tabs']"/>">

<%@ page import="org.votingsystem.model.UserVS; org.votingsystem.model.GroupVS" %>
<%
    def currentWeekPeriod = org.votingsystem.util.DateUtils.getCurrentWeekPeriod()
    def weekFrom =formatDate(date:currentWeekPeriod.getDateFrom(), formatName:'webViewDateFormat')
    def weekTo = formatDate(date:currentWeekPeriod.getDateTo(), formatName:'webViewDateFormat')
%>
<polymer-element name="groupvs-details" attributes="groupvs">
    <template>
        <style>
        .view { }
        </style>
        <div class="pageContenDiv" style="max-width: 1000px; padding: 0px 30px 150px 30px;">
            <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
            </div>
            <div layout horizontal center center-justified style="display:{{isAdminView?'block':'none'}}">
                <paper-button icon="create" class="button" label="<g:message code="editDataLbl"/>"
                              raisedButton on-click="{{editGroup}}"></paper-button>
                <paper-button icon="delete" class="button" label="<g:message code="cancelGroupVSLbl"/>"
                              raisedButton on-click="{{showConfirmCancelGroup}}"></paper-button>
                <paper-menu-button id="selectDepositPaperButton" valign="bottom" style="width: 0px;padding:0px;">
                    <core-selector id="coreSelector" selected="{{coreSelectorValue}}" valueattr="id" on-core-select="{{showDepositDialog}}">
                        <paper-item id="fromGroupToMember" label="<g:message code="makeDepositFromGroupVSToMemberLbl"/>"></paper-item>
                        <paper-item id="fromGroupToMemberGroup" label="<g:message code="makeDepositFromGroupVSToMemberGroupLbl"/>"></paper-item>
                        <paper-item id="fromGroupToAllMember" label="<g:message code="makeDepositFromGroupVSToAllMembersLbl"/>"</paper-item>
                    </core-selector>
                </paper-menu-button>
                <paper-button icon="credit-card" class="button" label="<g:message code="makeDepositFromGroupVSLbl"/>"
                              raisedButton on-click="{{openDepositDialogOptions}}"></paper-button>
            </div>


            <div layout horizontal center center-justified style="display:{{isUserView?'block':'none'}}">
                <button id="subscribeButton" type="submit" class="btn btn-default" onclick="subscribeToGroup();"
                        style="margin:15px 0px 0px 30px;color:#6c0404;">
                    <g:message code="subscribeGroupVSLbl"/> <i class="fa fa-sign-in"></i>
                </button>

                <button id="subscribeButton" type="submit" class="btn btn-default" onclick="subscribeToGroup();"
                        style="margin:15px 0px 0px 30px;color:#6c0404;">
                    <g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
                </button>
            </div>

            <h3><div class="pageHeader text-center">{{groupvs.name}}</div></h3>
            <div id="tagsDiv" style="padding:0px 0px 0px 30px; display:{{groupvs.tags.length == 0?'none':'block'}}">
                <div style=" display: table-cell; font-size: 1.1em; font-weight: bold; vertical-align: middle;"><g:message code='tagsLbl'/>:</div>
                <div id="selectedTagDiv" class="btn-group btn-group-sm" style="margin:0px 0px 15px 0px; padding: 5px 5px 0px 5px; display: table-cell;">
                    <template repeat="{{tag in groupvs.tags}}">
                        <a class="btn btn-default" href="#" role="button" style="margin:0px 10px 0px 0px;">{{tag.name}}</a>
                    </template>
                </div>
            </div>



            <div style="margin: 5px 0 15px 0;">
                <div class="eventContentDiv" style="">
                    <votingsystem-html-echo html="{{groupvs.description}}"></votingsystem-html-echo>
                </div>
                <div class="row" style="width:1000px;">
                    <div id="" style="margin:0px 30px 0px 10px; font-size: 0.85em; color:#888;" class="col-sm-4 text-left">
                        <b><g:message code="IBANLbl"/>: </b>{{groupvs.IBAN}}
                    </div>
                    <div id="" style="margin:0px 40px 0px 0px; font-size: 0.85em; float:right; color:#888;" class="col-sm-6 text-right">
                        <b><g:message code="groupRepresentativeLbl"/>: </b>{{groupvs.representative.firstName}} {{groupvs.representative.lastName}}
                    </div>
                </div>
            </div>

            <div class="text-center" style="font-size: 1.2em;font-weight: bold; color:#6c0404; padding: 0px 0 0 0; ">
                <g:message code="transactionsCurrentWeekPeriodMsg" args="${[weekFrom, weekTo]}"/>
            </div>

            <group-page-tabs id="groupTabs" groupvs="{{groupvs}}" style="width: 1000px;"></group-page-tabs>

            <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;
                    display:{{(!isAdminView && !isUserView) ? 'block':'none'}}">
                <g:message code="clientToolNeededMsg"/>.
                <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/>
            </div>
        </div>

        <div layout horizontal center center-justified style="position:absolute; top:80px; width: 100%; max-width: 1200px; margin: 0px auto 0px auto;">
            <div>
                <vicket-deposit-dialog id="depositDialog"></vicket-deposit-dialog>
            </div>
        </div>
    </template>
    <script>
        Polymer('groupvs-details', {
            isSelected: false,
            groupvs: null,
            groupvsChanged:function() {
                if(("admin" == menuType || "superadmin" == menuType) && 'ACTIVE' == this.groupvs.state) this.isAdminView = true
                else {
                    this.isAdminView = false
                    if("user" == menuType && 'ACTIVE' == this.groupvs.state) this.isUserView = true
                    else this.isUserView = false
                }
                this.$.groupTabs.groupvs = this.groupvs
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
                    document.querySelector("#coreSelector").selected = null
                }
            },
            openDepositDialogOptions:function() {
                this.$.selectDepositPaperButton.opened = true
            },
            showConfirmCancelGroup: function () {
                showMessageVS("<g:message code="cancelGroupVSDialogMsg"/>".format(this.groupvs.name),
                    "<g:message code="confirmOperationMsg"/>", 'cancel_group', true)
            },
            editGroup :  function() {
                console.log(this.tagName + " - editGroup")
                window.location.href = "${createLink( controller:'groupVS', action:'edit', absolute:true)}/" + this.groupvs.id + "?menu=admin"
            },
            ready :  function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#_votingsystemMessageDialog").addEventListener('message-accepted', function(e) {
                    console.log("message-accepted - cancelgroup: " + e.detail)
                    if('cancel_group' == e.detail) {
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
                                showMessageVS(this.appMessageJSON.message, caption, resultCallbackId)
                            }
                        }}

                        webAppMessage.callerCallback = this.objectId
                        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                        this.appMessageJSON = null
                    }
                });
            }
        })
    </script>
</polymer-element>