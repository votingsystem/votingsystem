<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-html-echo', file: 'vs-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog-transition.html')}">

<polymer-element name="representation-state" attributes="opened">
    <template>
        <g:include view="/include/styles.gsp"/>
        <paper-dialog id="xDialog" layered backdrop class="votingsystemMessageDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <style no-shim>
            .votingsystemMessageDialog {
                top: 150px;
                box-sizing: border-box; -moz-box-sizing: border-box; font-family: Arial, Helvetica, sans-serif;
                font-size: 13px; overflow: auto; background: #f9f9f9; max-width: 500px; width: 400px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
            }
            </style>
            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="userRepresentativeLbl"/></div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div style="color:#02227a;text-align: center; margin:10px 0 10px 0;">{{representationState.lastCheckedDateMsg}}</div>
                <div style="font-size: 1.2em; color:#ba0011; font-weight: bold; text-align: center;
                    padding:10px 20px 10px 20px; border-bottom: 1px solid #ba0011; border-top: 1px solid #ba0011;">
                    {{representationState.stateMsg}}
                    <div id="delegationTimeInfoDiv" horizontal layout style="margin:10px 0 0 0;display: none;font-size: 0.9em">
                        <b><g:message code="validFromLbl"/>:</b>
                        {{representationState.dateFrom}}
                        <span style="margin: 0 0 0 20px;"><b><g:message code="toLbl"/>:</b></span>
                        {{representationState.dateTo}}
                    </div>
                </div>
                <div id="representativeInfo" style="margin:10px 0 10px 0;">
                    <div style="color:#6c0404;text-align: center;">{{representativeFullName}}</div>
                    <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center;
                    padding:10px 20px 10px 20px; word-wrap:break-word;">
                        <vs-html-echo html="{{representationState.representative.description}}"></vs-html-echo>
                    </div>
                </div>
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <paper-button id="cancellationButton" raised on-click="{{cancelAnonymousDelegation}}" style="display: none;">
                        <g:message code="cancelAnonymousRepresentationMsg"/>
                    </paper-button>
                    <div flex></div>
                    <div style="display:{{isConfirmMessage?'block':'none'}};">
                        <paper-button raised on-click="{{accept}}">
                            <i class="fa fa-check"></i> <g:message code="acceptLbl"/>
                        </paper-button>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer('representation-state', {
            ready: function() {
                console.log(this.tagName + " - ")
            },
            openedChanged: function() {
                this.$.xDialog.opened = this.opened
                if(this.opened) {
                    var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_STATE)
                    var result = VotingSystemClient.call(webAppMessage)
                    this.representationState = toJSON(decodeURIComponent(escape(window.atob(result))));
                    this.message = JSON.stringify(representationState.representative)
                    if(this.representationState.representative) {
                        this.representativeFullName = this.representationState.representative.firstName + " " +
                                this.representationState.representative.lastName
                    } else this.$.representativeInfo.display = 'none'
                    if(this.representationState.state === "WITH_ANONYMOUS_REPRESENTATION") {
                        this.$.delegationTimeInfoDiv.style.display = 'block'
                        if(this.representationState.isWithAnonymousDelegation) {
                            this.$.cancellationButton.style.display = 'block'
                        }
                    }
                }
            },
            cancelAnonymousDelegation: function() {
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED)
                webAppMessage.serviceURL = "${createLink(controller:'representative', action:'cancelAnonymousDelegation', absolute:true)}"
                webAppMessage.signedMessageSubject = '<g:message code="cancelAnonymousDelegationMsgSubject"/>'
                webAppMessage.setCallback(function(appMessage) {
                    console.log("selectRepresentativeCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    var caption = '<g:message code="operationERRORCaption"/>'
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='operationOKCaption'/>"
                    }
                    showMessageVS(appMessageJSON.message, caption)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            accept: function() {
                this.close()
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>
