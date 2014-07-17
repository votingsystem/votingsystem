<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/get-reason-dialog.gsp']"/>">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">

<polymer-element name="groupvs-user" attributes="userId subscriptionDataURLPrefix">
    <template>
        <style no-shim>
        .card {
            position: relative;
            display: inline-block;
            width: 300px;
            height: 200px;
            vertical-align: top;
            background-color: #fff;
            box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
            margin: 10px;
        }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{subscriptionData}}" handleAs="json" method="get"
                   contentType="json" on-core-response="{{ajaxResponse}}"></core-ajax>
        <div style="max-width: 600px; margin:0px auto 0px auto;">
            <div layout horizontal center center-justified style="width:100%;">
                <paper-fab icon="{{showFab ? 'arrow-back' : ''}}"  on-tap="{{back}}"
                           style="color:#f9f9f9;visibility:{{$.pages.selected != 0 ? 'visible':'hidden'}}"></paper-fab>
                <div flex id="messageDiv" class="text-center" style="font-size: 1.4em; color:#6c0404; font-weight: bold;"></div>
            </div>
            <div id="" style="border: 1px solid #6c0404; width: 500px;margin:auto; padding: 15px;">
                <div layout horizontal>
                    <div id="" style="font-weight: bold;">NIF: {{subscriptionData.uservs.NIF}}</div>
                    <div id="" style="margin:0px 0px 0px 15px; font-weight: bold;" flex>IBAN: {{subscriptionData.uservs.IBAN}}</div>
                </div>

                <div id="nameDiv" style="font-size: 1.2em;font-weight: bold;">{{subscriptionData.uservs.name}}</div>
                <div id="contentDiv" style=""><g:message code="subscriptionRequestDateLbl"/>:
                    <span id="dateCreatedDiv"> {{subscriptionData.dateCreated}}</span></div>
            </div>
            <div layout horizontal center center-justified>
                <votingsystem-button id="activateUserButton" type="button" on-click="{{activateUser}}"
                        style="margin:10px 0px 0px 0px;display:{{isActive?'none':'block'}}"><g:message code="activateUserLbl"/> <i class="fa fa-thumbs-o-up"></i>
                </votingsystem-button>
                <votingsystem-button id="deActivateUserButton" on-click="{{initCancellation}}"
                        style="margin:10px 0px 0px 10px;display:{{(isActive && 'admin' == menuType) && !isCancelled?'block':'none'}} ">
                    <g:message code="deActivateUserLbl"/> <i class="fa fa-thumbs-o-down"></i>
                </votingsystem-button>
                <votingsystem-button id="makeDepositButton" on-click="{{makeDeposit}}"
                        style="margin:10px 0px 0px 10px;display:{{(isPending || isCancelled ) ? 'none':'block'}} "><g:message code="makeDepositLbl"/> <i class="fa fa-money"></i>
                </votingsystem-button>
            </div>
            <div id="receipt" style="display:none;">

            </div>
        </div>
        <div style="position: absolute; width: 100%; top:0px;left:0px;">
            <div layout horizontal center center-justified style="padding:0px 0px 0px 0px;margin:0px auto 0px auto;">
                <get-reason-dialog id="reasonDialog" caption="<g:message code="cancelSubscriptionFormCaption"/>" opened="false"
                                   messageToUser="<g:message code="cancelSubscriptionFormMsg"/>"></get-reason-dialog>
            </div>
        </div>

    </template>
    <script>
        Polymer('groupvs-user', {
            ready :  function(e) {
                this.menuType = menuType
                var hostElement = this
                this.$.reasonDialog.addEventListener('on-submit', function (e) {
                    console.log("deActivateUser")
                    var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_DEACTIVATE)
                    webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                    webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                    webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'deActivateUser',absolute:true)}"
                    webAppMessage.signedMessageSubject = "<g:message code="deActivateGroupUserMessageSubject"/>" + " '" + hostElement.subscriptionData.groupvs.name + "'"
                    webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_DEACTIVATE,
                        groupvs:{name:hostElement.subscriptionData.groupvs.name, id:hostElement.subscriptionData.groupvs.id},
                        uservs:{name:hostElement.subscriptionData.uservs.name, NIF:hostElement.subscriptionData.uservs.NIF}, reason:e.detail}
                    webAppMessage.contentType = 'application/x-pkcs7-signature'
                    var objectId = Math.random().toString(36).substring(7)
                    window[objectId] = {setClientToolMessage: function(appMessage) {
                        console.log("deActivateUserCallback - message: " + appMessage);
                        var appMessageJSON = toJSON(appMessage)
                        if(appMessageJSON != null) {
                            var caption = '<g:message code="deActivateUserERRORLbl"/>'
                            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                                caption = "<g:message code='deActivateUserOKLbl'/>"
                            }
                            var msg = appMessageJSON.message
                            showMessageVS(msg, caption)   }}}
                    webAppMessage.callerCallback = objectId
                    webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                    VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                })
            },
            show:function(baseURL, userId, showFab) {
                this.subscriptionDataURLPrefix = baseURL
                this.userId = userId
                this.showFab = showFab
            },
            userIdChanged:function() {
                this.$.ajax.url = this.subscriptionDataURLPrefix + "/" + this.userId + "?mode=simplePage&menu=" + menuType
            },
            back:function() {
                this.fire('core-signal', {name: "uservs-details-closed", data: this.userId});
            },
            activateUser : function(e) {
                console.log("activateUser")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_ACTIVATE)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'activateUser',absolute:true)}"

                webAppMessage.signedMessageSubject = "<g:message code="activateGroupUserMessageSubject"/>" + " '" +
                        this.subscriptionData.groupvs.name + "'"
                webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_ACTIVATE,
                    groupvs:{name:this.subscriptionData.groupvs.name, id:this.subscriptionData.groupvs.id},
                    uservs:{name:this.subscriptionData.uservs.name, NIF:this.subscriptionData.uservs.NIF}}
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                var objectId = Math.random().toString(36).substring(7)
                webAppMessage.callerCallback = objectId
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("activateUserCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="activateUserERRORLbl"/>'
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='activateUserOKLbl'/>"
                            $("#messageDiv").text("<g:message code="userStateActiveLbl"/>")
                            $("#activateUserButton").css("display", "none")
                            $("#makeDepositButton").css("display", "visible")
                            $("#deActivateUserButton").css("display", "visible")
                        }
                        showMessageVS(appMessageJSON.message, caption)
                    }}}
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            initCancellation : function(e) {
                this.$.reasonDialog.toggle();
            },
            makeDeposit : function(e) {
                console.log("makeDeposit")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_USER_DEPOSIT)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.serviceURL = "${createLink(controller:'transaction', action:'deposit',absolute:true)}/" + this.subscriptionData.groupvs.id
                webAppMessage.signedMessageSubject = "<g:message code="makeUserGroupDepositMessageSubject"/>" + " '" + this.subscriptionData.groupvs.name + "'"
                webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_USER_DEPOSIT,
                    groupvsName:this.subscriptionData.groupvs.name , id:this.subscriptionData.groupvs.id}
                webAppMessage.contentType = 'application/x-pkcs7-signature'
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("makeDepositCallback - message: " + appMessage);
                    var appMessageJSON = JSON.parse(appMessage)
                    if(appMessageJSON != null) {
                        var caption = '<g:message code="makeDepositERRORLbl"/>'
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            caption = "<g:message code='makeDepositOKLbl'/>"
                        }
                        var msg = appMessageJSON.message
                        showMessageVS(msg, caption) }}}
                webAppMessage.callerCallback = objectId
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            ajaxResponse:function() {
                this.isActive = false
                this.isPending = false
                this.isCancelled = false
                if('ACTIVE' == this.subscriptionData.state) {
                    this.isActive = true
                    this.$.messageDiv.innerHTML = "<g:message code="userStateActiveLbl"/>"
                } else if('PENDING' == this.subscriptionData.state) {
                    this.isPending = true
                    this.$.messageDiv.innerHTML = "<g:message code="userStatePendingLbl"/>"
                } else if('CANCELLED' == this.subscriptionData.state) {
                    this.isCancelled = true
                    this.$.messageDiv.innerHTML = "<g:message code="userStateCancelledLbl"/>"
                }
            }
        });
    </script>
</polymer-element>