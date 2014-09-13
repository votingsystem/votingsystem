<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">


<polymer-element name="representative-request-accreditations-dialog">
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
                            <g:message code="requestRepresentativeAcreditationsLbl"/>
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div><g:message code="accreditationRequestMsg"/></div>

                <div style="margin:0px 0px 0px 20px;">
                    <label>${message(code:'dateRequestLbl')}</label>
                    <div id="dateRequest">
                        <g:datePicker name="dateRequest" value="${new Date().plus(2)}" precision="day" relativeYears="[0..1]"/>
                    </div>
                </div>
                <div style="margin:15px 0px 20px 0px">
                    <input type="email" id="emailRequest" style="width:350px; margin:0px auto 0px auto;" required
                           title='<g:message code='enterEmailLbl'/>' class="form-control"
                           placeholder='<g:message code='emailInputLbl'/>'/>
                </div>
                <div layout horizontal>
                    <div flex></div>
                    <div>
                        <votingsystem-button on-click="{{submit}}">
                            <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('representative-request-accreditations-dialog', {
            representativeFullName:null,
            publish: {
                representative: {value: {}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(representative) {
                this.representative = representative
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                this.$.xDialog.opened = true
            },
            submit: function() {
                var dateRequest = getDatePickerValue('dateRequest', this.$.dateRequest)
                console.log("requestAccreditations - dateRequest: " + dateRequest)
                if(!this.$.emailRequest.validity.valid) {
                    showMessageVS('<g:message code="emailERRORMsg"/>', '<g:message code="errorLbl"/>')
                    return false
                }

                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
                webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
                webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
                webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_ACCREDITATIONS_REQUEST,
                    representativeNif:this.representative.nif, email:this.$.emailRequest.value,
                    representativeName:this.representativeFullName, selectedDate: dateRequest.formatWithTime()}
                webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
                webAppMessage.serviceURL = "${createLink(controller:'representative', action:'accreditations', absolute:true)}"
                webAppMessage.signedMessageSubject = '<g:message code="requestRepresentativeAcreditationsLbl"/>'
                webAppMessage.email = this.$.emailRequest.value

                var objectId = Math.random().toString(36).substring(7)
                window[objectId] = {setClientToolMessage: function(appMessage) {
                    console.log("requestAccreditationsCallback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    var caption = '<g:message code="operationERRORCaption"/>'
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='operationOKCaption'/>"
                    } else if (ResponseVS.SC_CANCELLED== appMessageJSON.statusCode) {
                        caption = "<g:message code='operationCANCELLEDLbl'/>"
                    }
                    var msg = appMessageJSON.message
                    showMessageVS(msg, caption)
                    }.bind(this)}

                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                this.close()
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>