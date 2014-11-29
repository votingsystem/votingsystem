<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-icon-button" file="core-icon-button.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>


<polymer-element name="representative-request-votinghistory-dialog">
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
            </style>
            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            <g:message code="requestVotingHistoryLbl"/>
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div><g:message code="representativeHistoryRequestMsg"/></div>

                <label><g:message code="selectDateRangeMsg"/></label>

                <div layout horizontal style="margin:10px 0px 0px 20px;">
                    <div>
                        <label><g:message code="firstDaterangeLbl"/></label>
                        <div id="dateFrom">
                            <g:datePicker name="dateFrom" value="${new Date().minus(7)}" precision="day" relativeYears="[0..5]"/>
                        </div>
                    </div>
                    <div style="margin:0px 0px 0px 20px;">
                        <label>${message(code:'dateToLbl')}</label>
                        <div id="dateTo">
                            <g:datePicker name="dateTo" value="${new Date()}" precision="day" relativeYears="[0..5]"/>
                        </div>
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
                        <paper-button raised on-click="{{submit}}">
                            <i class="fa fa-check"></i> <g:message code="acceptLbl"/>
                        </paper-button>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer('representative-request-votinghistory-dialog', {
            representativeFullName:null,
            publish: {
                representative: {value: {}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(representative) {
                this.$.xDialog.opened = true
                this.representative = representative
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
            },
            submit: function() {
                var dateFrom = getDatePickerValue('dateFrom', this.$.dateFrom)
                var dateTo = getDatePickerValue('dateTo', this.$.dateTo)
                this.$.dateFrom.classList.remove("formFieldError");
                this.$.dateTo.classList.remove("formFieldError");
                console.log("requestVotingHistory - dateFrom: " + dateFrom + " - dateTo: " + dateTo)
                if(dateFrom > dateTo) {
                    showMessageVS('<g:message code="dateRangeERRORMsg"/>', '<g:message code="errorLbl"/>')
                    this.$.dateFrom.classList.add("formFieldError");
                    this.$.dateTo.classList.add("formFieldError");
                    return false
                }

                if(!this.$.emailRequest.validity.valid) {
                    showMessageVS('<g:message code="emailERRORMsg"/>', '<g:message code="errorLbl"/>')
                    return false
                }
                var webAppMessage = new WebAppMessage(Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST)
                webAppMessage.signedContent = {operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
                    representativeNif:this.representative.nif,
                    representativeName:this.representativeFullName, dateFrom:dateFrom.formatWithTime(),
                    dateTo:dateTo.formatWithTime(), email:this.$.emailRequest.value}
                webAppMessage.serviceURL = "${createLink(controller:'representative', action:'history', absolute:true)}"
                webAppMessage.signedMessageSubject = '<g:message code="requestVotingHistoryLbl"/>'
                webAppMessage.email = this.$.emailRequest.value
                webAppMessage.setCallback(function(appMessage) {
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