<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/reason-dialog']"/>">
<link rel="import" href="${resource(dir: '/bower_components/vs-dialog', file: 'vs-dialog.html')}">


<polymer-element name="groupvs-user" attributes="userId subscriptionDataURLPrefix opened">
    <template>
        <vs-dialog id="xDialog" class="uservsDialog" on-core-overlay-open="{{onCoreOverlayOpen}}" style="width: 540px;">
            <g:include view="/include/styles.gsp"/>
        <core-ajax id="ajax" auto url="{{url}}" response="{{subscriptionData}}" handleAs="json" method="get"
                   contentType="json" on-core-response="{{ajaxResponse}}"></core-ajax>
        <div layout vertical>
            <div id="" style="width: 450px;margin:auto; padding: 15px;">
                <div layout horizontal style="font-size: 0.8em;">
                    <div style="font-weight: bold;color:#888;" flex>NIF: {{subscriptionData.uservs.NIF}}</div>
                    <template if="{{subscriptionData.uservs.IBAN}}">
                        <div style="font-weight: bold;color:#888;">IBAN: {{subscriptionData.uservs.IBAN}}</div>
                    </template>
                </div>

                <div id="nameDiv" style="font-size: 1.2em;font-weight: bold; margin:5px 0px 5px 0px;">{{subscriptionData.uservs.name}}</div>
                <div id="contentDiv" style=""><g:message code="subscriptionRequestDateLbl"/>:
                    <span id="dateCreatedDiv"> {{subscriptionData.dateCreated}}</span></div>
            </div>
            <div layout horizontal center center-justified style="margin:10px 0 0 0; font-size: 1em;">
                <template if="{{isClientToolConnected}}">
                    <paper-button raised type="button" on-click="{{activateUser}}"
                                         style="margin:0 10px;display:{{isActive?'none':'block'}}">
                        <i class="fa fa-thumbs-o-up"></i> <g:message code="activateUserLbl"/>
                    </paper-button>
                    <paper-button raised on-click="{{initCancellation}}"
                                         style="margin:0 10px;display:{{(isActive && 'admin' == menuType) && !isCancelled?'block':'none'}} ">
                        <i class="fa fa-thumbs-o-down"></i> <g:message code="deActivateUserLbl"/>
                    </paper-button>
                </template>
                <paper-button raised type="submit" on-click="{{goToUserVSPage}}" style="margin:0 10px">
                    <i class="fa fa-user"></i> <g:message code="userVSPageLbl"/>
                </paper-button>
            </div>
            <div id="receipt" style="display:none;"> </div>
        </div>
        <div style="position: absolute; width: 100%; top:0px;left:0px;">
            <div layout horizontal center center-justified style="padding:0px 0px 0px 0px;margin:0px auto 0px auto;">
                <reason-dialog id="reasonDialog" caption="<g:message code="cancelSubscriptionFormCaption"/>" opened="false"
                                   messageToUser="<g:message code="cancelSubscriptionFormMsg"/>"></reason-dialog>
            </div>
        </div>
        </vs-dialog>
    </template>
    <script>
        Polymer('groupvs-user', {
            isClientToolConnected:false,
            ready :  function(e) {
                this.menuType = menuType
                this.isClientToolConnected = window['isClientToolConnected']
                this.$.reasonDialog.addEventListener('on-submit', function (e) {
                    console.log("deActivateUser")
                    var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_DEACTIVATE)
                    webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'deActivateUser',absolute:true)}"
                    webAppMessage.signedMessageSubject = "<g:message code="deActivateGroupUserMessageSubject"/>" + " '" + this.subscriptionData.groupvs.name + "'"
                    webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_DEACTIVATE,
                        groupvs:{name:this.subscriptionData.groupvs.name, id:this.subscriptionData.groupvs.id},
                        uservs:{name:this.subscriptionData.uservs.name, NIF:this.subscriptionData.uservs.NIF}, reason:e.detail}
                    webAppMessage.contentType = 'application/x-pkcs7-signature'
                    webAppMessage.setCallback(function(appMessage) {
                        console.log("deActivateUserCallback - message: " + appMessage);
                        var appMessageJSON = toJSON(appMessage)
                        var caption = '<g:message code="deActivateUserERRORLbl"/>'
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='deActivateUserOKLbl'/>"
                        }
                        var msg = appMessageJSON.message
                        showMessageVS(msg, caption)
                    }.bind(this))
                    VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                }.bind(this))
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            goToUserVSPage:function() {
                loadURL_VS("${createLink( controller:'userVS', action:"", absolute:true)}/" + this.userId)
                this.$.xDialog.opened = false
            },
            openedChanged:function() {
                this.async(function() { this.$.xDialog.opened = this.opened});
            },
            show:function(baseURL, userId) {
                this.subscriptionDataURLPrefix = baseURL
                this.userId = userId
                this.$.xDialog.opened = true
            },
            userIdChanged:function() {
                this.$.ajax.url = this.subscriptionDataURLPrefix + "/" + this.userId + "?mode=simplePage&menu=" + menuType
            },
            activateUser : function(e) {
                console.log("activateUser")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_ACTIVATE)
                webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'activateUser',absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code="activateGroupUserMessageSubject"/>" + " '" +
                        this.subscriptionData.groupvs.name + "'"
                webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_ACTIVATE,
                    groupvs:{name:this.subscriptionData.groupvs.name, id:this.subscriptionData.groupvs.id},
                    uservs:{name:this.subscriptionData.uservs.name, NIF:this.subscriptionData.uservs.NIF}}
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                webAppMessage.setCallback(function(appMessage) {
                    console.log("activateUserCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="activateUserERRORLbl"/>'
                        if (ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='activateUserOKLbl'/>"
                            this.opened = false
                            this.fire('core-signal', {name: "refresh-uservs-list", data: {uservs: this.userId}});
                        }
                        showMessageVS(appMessageJSON.message, caption)
                    }
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            initCancellation : function(e) {
                this.$.reasonDialog.toggle();
            },
            ajaxResponse:function() {
                this.isActive = false
                this.isPending = false
                this.isCancelled = false
                if('ACTIVE' == this.subscriptionData.state) {
                    this.isActive = true
                    this.caption = "<g:message code="userStateActiveLbl"/>"
                } else if('PENDING' == this.subscriptionData.state) {
                    this.isPending = true
                    this.caption = "<g:message code="userStatePendingLbl"/>"
                } else if('CANCELLED' == this.subscriptionData.state) {
                    this.isCancelled = true
                    this.caption = "<g:message code="userStateCancelledLbl"/>"
                }
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>