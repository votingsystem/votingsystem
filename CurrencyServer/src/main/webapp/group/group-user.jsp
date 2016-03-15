<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/reason-dialog.vsp" rel="import"/>

<dom-module name="group-user">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div style="height: 400px;">
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{subscriptionDto.user.name}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>
                    <div class="horizontal layout" style="font-size: 0.8em; margin: 5px 0 10px 0;">
                        <div style="font-weight: bold;color:#888;" class="flex">NIF: <span>{{subscriptionDto.user.nif}}</span></div>
                        <div hidden="{{!subscriptionDto.user.iban}}" style="font-weight: bold;color:#888;">
                            IBAN: <span>{{subscriptionDto.user.iban}}</span></div>
                    </div>
                    <div id="contentDiv">${msg.subscriptionRequestDateLbl}:
                        <span id="dateCreatedDiv">{{getDate(subscriptionDto.dateCreated)}}</span></div>
                </div>
                <div class="horizontal layout center center-justified" style="margin:10px 0 30px 0;">
                    <div hidden="{{!isClientToolConnected}}">
                        <div hidden="{{isActive}}">
                            <button hidden="{{!isAdmin}}" on-click="activateUser">
                                <i class="fa fa-thumbs-o-up"></i> ${msg.activateUserLbl}
                            </button>
                        </div>
                        <div hidden="{{!isActive}}">
                            <button hidden="{{!isAdmin}}" on-click="initCancellation">
                                <i class="fa fa-thumbs-o-down"></i> ${msg.deActivateUserLbl}
                            </button>
                        </div>
                    </div>
                    <button class="btnvs" on-click="goToUserPage">
                        <i class="fa fa-user"></i> ${msg.userPageLbl}
                    </button>
                </div>
                <div id="receipt" style="display:none;"> </div>
            </div>
        </div>
        <div style="position: absolute; width: 100%; top:0px;left:0px;">
            <div class="layout horizontal center center-justified" style="padding:0px 0px 0px 0px;margin:0px auto 0px auto;">
                <reason-dialog id="reasonDialog" caption="${msg.cancelSubscriptionFormCaption}" ></reason-dialog>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'group-user',
            properties: {
                url:{type:String, observer:'getHTTP'},
                subscriptionDto: {type:Object, observer:'subscriptionDtoChanged'},
                isClientToolConnected: {type:Boolean},
                isActive: {type:Boolean},
                isAdmin: {type:Boolean}
            },
            ready:function(e) {
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available

                document.querySelector("#voting_system_page").addEventListener('on-submit-reason',
                        function(e) {
                            console.log("deActivateUser")
                            var operationVS = new OperationVS(Operation.CURRENCY_GROUP_USER_DEACTIVATE)
                            operationVS.serviceURL = vs.contextURL + "/rest/group/deActivateUser"
                            operationVS.signedMessageSubject = "${msg.deActivateGroupUserMessageSubject}" + " '" + this.subscriptionDto.group.name + "'"
                            operationVS.jsonStr = JSON.stringify({groupId:this.subscriptionDto.group.id,
                                groupName:this.subscriptionDto.group.name, userName:this.subscriptionDto.user.name,
                                userNIF:this.subscriptionDto.user.nif, reason:e.detail})
                            operationVS.setCallback(function(appMessage) {
                                this.deActivateResponse(appMessage)
                            }.bind(this))
                            VotingSystemClient.setMessage(operationVS);
                        }.bind(this))
            },
            deActivateResponse:function(appMessageJSON) {
                console.log(this.tagName + " - deActivateResponse");
                var caption = '${msg.deActivateUserERRORLbl}'
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "${msg.deActivateUserOKLbl}"
                    document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('refresh-user-list', {user: this.userId}))
                    this.close()
                }
                alert(appMessageJSON.message, caption)
            },
            getDate:function(timStamp) {
                return new Date(timStamp).getDayWeekAndHourFormat()
            },
            goToUserPage:function() {
                page.show(vs.contextURL + "/rest/user/id/" + this.userId)
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            },
            subscriptionDtoChanged:function() {
                console.log(this.tagName + "subscriptionDtoChanged - subscriptionDto: " + this.subscriptionDto)
                this.isActive = false
                this.isPending = false
                this.isCancelled = false
                this.isAdmin = false
                if('ACTIVE' == this.subscriptionDto.state) {
                    this.isActive = true
                    this.caption = "${msg.userStateActiveLbl}"
                } else if('PENDING' == this.subscriptionDto.state) {
                    this.isPending = true
                    this.caption = "${msg.userStatePendingLbl}"
                } else if('CANCELED' == this.subscriptionDto.state) {
                    this.isCancelled = true
                    this.caption = "${msg.userStateCancelledLbl}"
                }
                this.isAdmin = ('admin' === menuType || 'superuser' === menuType)
            },
            show:function(baseURL, userId) {
                if(baseURL && userId) {
                    this.subscriptionDataURLPrefix = baseURL
                    this.userId = userId
                    this.url = this.subscriptionDataURLPrefix + "/" + this.userId
                }
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            activateUser : function(e) {
                console.log("activateUser")
                var operationVS = new OperationVS(Operation.CURRENCY_GROUP_USER_ACTIVATE)
                operationVS.serviceURL = vs.contextURL + "/rest/group/activateUser"
                operationVS.signedMessageSubject = "${msg.activateGroupUserMessageSubject}" + " '" +
                        this.subscriptionDto.group.name + "'"
                operationVS.jsonStr = JSON.stringify({operation:Operation.CURRENCY_GROUP_USER_ACTIVATE,
                    groupId:this.subscriptionDto.group.id, groupName:this.subscriptionDto.group.name,
                    userName:this.subscriptionDto.user.name, userNIF:this.subscriptionDto.user.nif})
                operationVS.setCallback(function(appMessage) { this.activateResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            activateResponse: function(appMessageJSON) {
                console.log(this.tagName + " - activateResponse")
                if(appMessageJSON != null) {
                    var caption = '${msg.activateUserERRORLbl}'
                    if (ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "${msg.activateUserOKLbl}"
                        document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('refresh-user-list', {user: this.userId}))
                        this.close()
                    }
                    alert(appMessageJSON.message, caption)
                }
            },
            initCancellation : function(e) {
                this.$.reasonDialog.show("${msg.cancelSubscriptionFormMsg}");
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.subscriptionDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>