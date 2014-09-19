<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">


<polymer-element name="representative-cancel-dialog">
    <template>
        <votingsystem-dialog id="xDialog" class="dialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
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
                width: 500px;
            }
            </style>
            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            <g:message code="removeRepresentativeLbl"/>
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <p style="text-align: center;"><g:message code="removeRepresentativeMsg"/></p>
                </div>
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex></div>
                    <div>
                        <votingsystem-button on-click="{{accept}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('representative-cancel-dialog', {
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function() {
                this.$.xDialog.opened = true
            },
            accept: function() {
                console.log(this.tagName + " - removeRepresentative")
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_REVOKE)
                webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_REVOKE}
                webAppMessage.serviceURL = "${createLink(controller:'representative', action:'revoke', absolute:true)}"
                webAppMessage.signedMessageSubject = '<g:message code="removeRepresentativeMsgSubject"/>'
                webAppMessage.setCallback(function(appMessage) {
                    console.log(this.tagName + "removeRepresentativeCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    var caption = '<g:message code="operationERRORCaption"/>'
                    var msg = appMessageJSON.message
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='operationOKCaption'/>"
                        msg = "<g:message code='removeRepresentativeOKMsg'/>";
                    } else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
                        caption = "<g:message code='operationCANCELLEDLbl'/>"
                    }
                    showMessageVS(msg, caption)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                this.close()
            },

            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>