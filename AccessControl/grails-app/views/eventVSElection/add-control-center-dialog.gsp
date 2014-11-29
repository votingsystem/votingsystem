<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-icon-button" file="core-icon-button.html"/>
<vs:webresource dir="vs-html-echo" file="vs-html-echo.html"/>
<vs:webresource dir="paper-shadow" file="paper-shadow.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>

<polymer-element name="add-control-center-dialog" attributes="opened">
    <template>
        <paper-dialog id="xDialog" layered backdrop on-core-overlay-open="{{onCoreOverlayOpen}}">
            <g:include view="/include/styles.gsp"/>
            <style no-shim>
            .dialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 550px;
            }
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
            </style>
            <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" contentType="json"></core-ajax>
            <core-xhr id="ajax1" ></core-xhr>
            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:0px 0px 0px 30px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="controlCenterLbl"/></div>
                    </div>
                    <div>
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div style="display:{{messageToUser? 'block':'none'}}">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{messageToUser}}</div>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>

                <div id="addControlCenterDialogMessageDiv" class='text-center'
                     style="color: #6c0404; font-size: 1.2em;font-weight: bold; margin-bottom: 15px;"></div>

                <div id="formDiv">
                    <p style="text-align: center;">
                        <g:message code="controlCenterDescriptionMsg"/>
                    </p>
                    <input type="url" id="controlCenterURL" style="width:450px; margin:0px auto 0px auto;"
                           class="form-control" placeholder="<g:message code="controlCenterURLLbl"/>" required/>
                </div>

                <div id="checkControlCenterProgressDiv" style="display:{{checking? 'block':'none'}};">
                    <p style='text-align: center;'><g:message code="checkingControlCenterLbl"/></p>
                    <progress style='display:block;margin:0px auto 10px auto;'></progress>
                </div>
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex></div>
                    <div style="margin:10px 0px 10px 0px;">
                        <paper-button raised on-click="{{checkControlCenter}}" style="margin: 0px 0px 0px 5px;">
                            <g:message code="acceptLbl"/> <i class="fa fa-check"></i>
                        </paper-button>
                    </div>
                </div>


            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer('add-control-center-dialog', {
            ajaxOptions:{method:'get'},
            ready: function() {
                this.$.controlCenterURL.onkeypress = function(event){
                    var chCode = ('charCode' in event) ? event.charCode : event.keyCode;
                    if (chCode == 13) this.checkControlCenter()
                }.bind(this)
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.$.xDialog.opened = this.opened
                if(!this.opened) {
                    this.$.controlCenterURL.value = ""
                    this.messageToUser = null
                    this.checking = false
                    this.url = null
                }
            },
            show: function() {
                this.opened = true;
            },
            close: function() {
                this.opened = false;
            },
            checkControlCenter:function() {
                this.messageToUser = null
                if(!this.$.controlCenterURL.validity.valid){
                    this.messageToUser = "<g:message code="invalidURLMsg"/>"
                    return
                }
                this.checking = true
                //<core-ajax> has problems in WebKit with CORS requests
                //this.url = this.$.controlCenterURL.value

                this.ajaxOptions.url = this.$.controlCenterURL.value
                this.ajaxOptions.callback = this.ajaxResponse.bind(this)
                this.$.ajax1.request(this.ajaxOptions)

            },
            ajaxResponse: function(xhrResponse, xhr) {
                //console.log(this.tagName + " - ajax-response - newURL: " + this.$.ajax.url + " - status: " + e.detail.xhr.status)
                console.log(this.tagName + " - ajax-response - newURL: "  + this.ajaxOptions.url + " - status: " + xhr.status + " - xhrResponse: " + xhrResponse)
                this.responseData = toJSON(xhrResponse)
            },
            responseDataChanged:function() {
                console.log( "this.responseData: " + this.responseData)
                this.url = ""
                if("CONTROL_CENTER" == this.responseData.serverType) {
                    this.associateControlCenter(this.responseData.serverURL)
                } else {
                    console.log( "Server type wrong -> " + this.responseData.serverType);
                    this.checking = false
                    this.messageToUser = "<g:message code="controlCenterURLERRORMsg"/>"
                }
            },
            associateControlCenter:function(controlCenterURL) {
                console.log(this.tagName + " associateControlCenter - controlCenterURL: " + controlCenterURL);
                var webAppMessage = new WebAppMessage(Operation.CONTROL_CENTER_ASSOCIATION)
                var signatureContent = {
                    serverURL:controlCenterURL,
                    operation:Operation.CONTROL_CENTER_ASSOCIATION}
                webAppMessage.signedContent = signatureContent
                webAppMessage.signedMessageSubject = '<g:message code="addControlCenterMsgSubject"/>'
                webAppMessage.serviceURL = "${createLink( controller:'subscriptionVS', absolute:true)}"
                webAppMessage.setCallback(function(appMessage) {
                    console.log("activateUserCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    var caption = '<g:message code="operationERRORCaption"/>'
                    var msg = appMessageJSON.message
                    if(ResponseVS.SC_OK ==  appMessageJSON.statusCode) {
                        caption = '<g:message code="operationOKCaption"/>'
                        msg = "<g:message code='operationOKMsg'/>";
                    }
                    showMessageVS(msg, caption)
                    this.checking = false
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
            }
        });
    </script>
</polymer-element>
