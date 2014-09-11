<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">

<polymer-element name="select-representative-dialog">
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
                    <div flex style="font-size: 1.5em; margin:0px 0px 0px 30px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="saveAsRepresentativeLbl"/></div>
                    </div>
                    <div>
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>

                <div style="display:{{step == 'main'?'block':'none'}}"}}">
                    <p><g:message code="delegationIntroLbl"/></p>

                    <div style="font-size:1.2em;font-weight: bold;"><g:message code="anonymousDelegationLbl"/></div>
                    <div><g:message code="anonymousDelegationMsg"/></div>
                    <div style="margin: 0 0 0 40px;">
                        <div id="moreDetailsMsgDiv" class="linkVS" on-click="{{toggleDetails}}">
                            <template if="{{showingDetails}}">
                                <g:message code="hideDetailsMsg"/>
                            </template>
                            <template if="{{!showingDetails}}">
                                <g:message code='showAnoymousDelegationDetailsMsg'/>
                            </template>
                        </div>
                        <template if="{{showingDetails}}">
                            <div><g:message code='anonymousDelegationMoreMsg'/></div>
                        </template>
                    </div>

                    <div style="margin:20px 0 0 0;font-size:1.2em;font-weight: bold;"><g:message code="publicDelegationLbl"/></div>
                    <div><g:message code="publicDelegationMsg"/></div>

                    <div style="margin:20px 0 0 0;"><g:message code="selectRepresentationCheckboxMsg"/>:</div>
                    <div>
                        <div style="margin:0 0 0 40px;">
                            <input type="radio" name="delegationType" id="anonymousDelegationCheckbox" on-click="{{setDelegationType}}"/>
                            <label><g:message code="anonymousDelegationCheckboxLbl"/></label>
                        </div>
                        <div style="margin:0 0 0 40px;">
                            <input type="radio" name="delegationType" id="publicDelegationCheckbox" checked="checked" on-click="{{setDelegationType}}"/>
                            <label><g:message code="publicDelegationCheckboxLbl"/></label>
                        </div>
                    </div>
                </div>
                <div style="display:{{step == 'confirm'?'block':'none'}}"}}">
                    <votingsystem-html-echo id="delegationMsg"></votingsystem-html-echo>
                    <template if="{{isAnonymousDelegation == true}}">
                        <div style="margin:25px 0 25px 0;">
                            <votingsystem-html-echo html="{{anonymousDelegationMsg}}"></votingsystem-html-echo>
                        </div>
                    </template>
                </div>


                    <div style="margin:10px 0; display:{{isAnonymousDelegation == true && step == 'main'?'block':'none'}}">
                        <label><g:message code="numWeeksAnonymousDelegationMsg"/></label>
                        <input type="number" id="numWeeksAnonymousDelegation" min="1" value="" max="52" required
                               style="width:120px;margin:10px 20px 0px 7px;" class="form-control"
                               title="<g:message code="numWeeksAnonymousDelegationMsg"/>">
                    </div>

                <div style="display: {{step == 'main'?'block':'none'}};">
                    <div layout horizontal style="margin:0px 20px 0px 0px;">
                        <div flex></div>
                        <div style="margin:10px 0px 10px 0px;">
                            <votingsystem-button on-click="{{showConfirm}}" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                            </votingsystem-button>
                        </div>
                    </div>
                </div>

                <div style=" display: {{step == 'confirm'?'block':'none'}}">
                    <div layout horizontal style="margin:0px 20px 0px 0px;">
                        <div flex></div>
                        <div style="margin:10px 0px 10px 0px;">
                            <votingsystem-button on-click="{{submit}}" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                            </votingsystem-button>
                        </div>
                        <div style="margin:10px 0px 10px 0px;">
                            <votingsystem-button on-click="{{cancel}}" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-times" style="margin:0 7px 0 3px;"></i> <g:message code="cancelLbl"/>
                            </votingsystem-button>
                        </div>
                    </div>
                </div>

                <div style="display: {{step == 'anonymousDelegationResponse'?'block':'none'}}">
                    <p style="text-align: center; font-size: 1.2em;">
                        {{anonymousDelegationResponseMsg}}
                    </p>
                    <p style="text-align: center; font-size: 1.2em;">
                        <g:message code="anonymousDelegationReceiptMsg"/>
                    </p>
                    <button type="button" class="btn btn-default" on-click="{{saveAnonymousDelegation}}">
                        <g:message code="saveReceiptLbl"/>
                    </button>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('select-representative-dialog', {
            step:'main',
            showingDetails:false,
            isAnonymousDelegation:false,
            anonymousDelegationMsg:null,
            representative:null,
            anonymousLbl:"<g:message code='anonymousLbl'/>",
            hashReceiptAnonymousDelegation:null,
            anonymousDelegationResponseMsg:null,
            representativeFullName:null,
            publicLbl:"<g:message code='publicLbl'/>",

            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
                console.log(this.tagName + " - ready")
            },
            onCoreOverlayOpen:function(e) { },
            toggleDetails:function(e) {
                this.showingDetails = !this.showingDetails
            },
            show: function(representative) {
                this.representative = representative
                this.$.numWeeksAnonymousDelegation.value = ""
                this.step = 'main'
                this.isAnonymousDelegation = false
                this.$.publicDelegationCheckbox.checked = true
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                this.$.xDialog.opened = true
                console.log(this.tagName + " - step: " + this.step)
            },
            saveAnonymousDelegation: function() {
                console.log(this.tagName + " - saveAnonymousDelegation - hashReceiptAnonymousDelegation: " + this.hashReceiptAnonymousDelegation)
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SAVE_RECEIPT_ANONYMOUS_DELEGATION)
                webAppMessage.message = document.getElementById("receipt").innerHTML.trim()
                //webAppMessage.message = document.getElementById("receipt").innerHTML.trim()
                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) { }.bind(this)}
                webAppMessage.callerCallback = objectId

                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            submit: function() {
                console.log("submit")
                var representativeOperation
                if(!this.$.anonymousDelegationCheckbox.checked && !this.$.publicDelegationCheckbox.checked) {
                    showMessageVS("<g:message code='selectRepresentationCheckboxMsg'/>", "<g:message code='errorLbl'/>")
                } else {
                    if(this.isAnonymousDelegation) {
                        representativeOperation = Operation.ANONYMOUS_REPRESENTATIVE_SELECTION
                        if(!this.$.numWeeksAnonymousDelegation.validity.valid) {
                            this.messageToUser = '<g:message code="numberFieldLbl"/>'
                            return
                        }
                    } else {
                        representativeOperation = Operation.REPRESENTATIVE_SELECTION
                    }
                    var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, representativeOperation)
                    webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                    webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                    webAppMessage.signedContent = {operation:representativeOperation,
                        representativeNif:this.representative.nif,
                        representativeName:this.representativeFullName,
                        weeksOperationActive:this.$.numWeeksAnonymousDelegation.value}
                    webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                    webAppMessage.serviceURL = "${createLink(controller:'representative', action:'delegation', absolute:true)}"
                    webAppMessage.signedMessageSubject = '<g:message code="representativeDelegationMsgSubject"/>'

                    var objectId = Math.random().toString(36).substring(7)
                    window[objectId] = {setClientToolMessage: function(appMessage) {
                        console.log("selectRepresentativeCallback - message: " + appMessage);
                        var appMessageJSON = toJSON(appMessage)
                        var caption = '<g:message code="operationERRORCaption"/>'
                        var msg = appMessageJSON.message
                        if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                            if(this.isAnonymousDelegation) {
                                this.step = 'anonymousDelegationResponse'
                                this.hashReceiptAnonymousDelegation = appMessageJSON.message
                                this.anonymousDelegationResponseMsg = "<g:message code='selectedRepresentativeMsg'/>".format(this.representativeFullName)
                                return
                            } else {
                                caption = "<g:message code='operationOKCaption'/>"
                                msg = "<g:message code='selectedRepresentativeMsg'/>".format(this.representativeFullName)
                                this.close()
                            }
                        } else if (ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
                            caption = "<g:message code='anonymousDelegationActiveErrorCaption'/>"
                            msg = appMessageJSON.message + "<br/>" +
                                    "<g:message code='downloadReceiptMsg'/>".format(appMessageJSON.URL)
                        }
                        showMessageVS(msg, caption)
                    }.bind(this)}
                    webAppMessage.callerCallback = objectId
                    VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                }
            },

            setDelegationType:function() {
                if(this.$.anonymousDelegationCheckbox.checked) {
                    this.isAnonymousDelegation = true
                } else this.isAnonymousDelegation = false
                console.log(this.tagName + " - setDelegationType - isAnonymousDelegation: " + this.isAnonymousDelegation)
            },
            close: function() {
                this.$.xDialog.opened = false
            },
            showConfirm: function() {
                this.messageToUser = null
                var msgTemplate = "<g:message code='selectRepresentativeConfirmMsg'/>";
                if(this.isAnonymousDelegation) {
                    if(!this.$.numWeeksAnonymousDelegation.validity.valid) {
                        showMessageVS('<g:message code="numWeeksAnonymousDelegationMsg"/>', '<g:message code="errorLbl"/>')
                        return
                    }
                    var weeksMsgTemplate = "<g:message code='numWeeksResultAnonymousDelegationMsg'/>";
                    this.$.delegationMsg.html = msgTemplate.format(this.anonymousLbl, this.representative.name)
                    this.anonymousDelegationMsg = weeksMsgTemplate.format(this.anonymousLbl, this.$.numWeeksAnonymousDelegation.value)
                } else this.$.delegationMsg.html = msgTemplate.format(this.publicLbl, this.representative.name)
                this.step = 'confirm'
            },
            cancel: function() {
                if(this.step == 'main') {
                    this.close()
                }else if(this.step == 'confirm') {
                    this.step = 'main'
                }
            }
        });
    </script>
</polymer-element>
