<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/simulation-listener']"/>">


<polymer-element name="manifest-simulation-form">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
        </style>
        <core-signals on-core-signal-simulation-listener-closed="{{closeSimulationListener}}"></core-signals>
        <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
                             transitions="cross-fade-all" style="display:{{loading?'none':'block'}}">
            <section id="page1">
                <div class="pageContentDiv" style="width:100%; padding:0px 20px 0px 20px;">

                    <div layout horizontal center center-justified style="margin: 15px 0px 15px 0px;width: 100%;">
                        <div id="pageTitle" class="pageHeader"><h3><g:message code="initManifestProtocolSimulationMsg"/></h3></div>
                    </div>

                    <div style="display:{{messageToUser? 'block':'none'}}">
                        <div class="messageToUser">
                            <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                                <div id="messageToUser">{{messageToUser}}</div>
                            </div>
                            <paper-shadow z="1"></paper-shadow>
                        </div>
                    </div>

                    <div id="formDataDiv">
                        <form id="claimProtocolSimulationDataForm" on-submit="{{submitForm}}">

                            <div>
                                <label><g:message code="numRequestsProjectedLbl"/></label>
                                <input type="number" id="numRequestsProjected" min="1" value="1" required
                                       style="width:130px;margin:0px 20px 0px 3px;"
                                       title="<g:message code="numRequestsProjectedLbl"/>"
                                       placeholder="<g:message code="numRequestsProjectedLbl"/>"/>
                                <label><g:message code="maxPendingResponsesLbl"/></label>
                                <input type="number" id="maxPendingResponses" min="1" value="10" required
                                       style="width:130px;margin:10px 20px 0px 3px;"
                                       title="<g:message code="maxPendingResponsesLbl"/>"
                                       placeholder="<g:message code="maxPendingResponsesLbl"/>"/>
                            </div>

                            <div layout horizontal center center-justified>
                                <div>
                                    <label><g:message code="eventStateOnFinishLbl"/></label>
                                    <select id="eventStateOnFinishSelect" style="margin:0px 20px 0px 0px;"
                                            title="<g:message code="setEventStateLbl"/>">
                                        <option value=""> - <g:message code="eventAsDateRangeLbl"/> - </option>
                                        <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="eventCancelledLbl"/> - </option>
                                        <option value="DELETED" style="color:#cc1606;"> - <g:message code="eventDeletedLbl"/> - </option>
                                    </select>
                                </div>

                                <label style="margin:0px 0px 0px 30px;">${message(code:'dateFinishLbl')}</label>
                                <div id="dateFinish">
                                    <g:datePicker name="dateFinish" value="${new Date().plus(2)}" precision="day" relativeYears="[0..1]"/>
                                </div>
                            </div>

                            <div style="margin:10px 0px 10px 0px">
                                <input type="text" name="subject" id="subject" style="width:350px"  required
                                       title="<g:message code="subjectLbl"/>"
                                       placeholder="<g:message code="subjectLbl"/>"/>
                                <input type="url" id="accessControlURL" style="width:300px; margin:0px 0px 0px 20px;" required
                                       value="http://sistemavotacion.org/AccessControl"
                                       title="<g:message code="accessControlURLMsg"/>"
                                       placeholder="<g:message code="accessControlURLMsg"/>"/>
                            </div>

                            <div id="textEditorDiv" style="padding:2px;">
                                <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
                            </div>

                            <div layout horizontal center id="backupDiv" style="margin:10px 0px 10px 10px; height: 50px;">
                                <div class="checkBox">
                                    <input type="checkbox" id="requestBackup"/><label><g:message code="requestBackupLbl"/></label>
                                </div>
                                <div id="emailDiv"style="display:none;">
                                    <input type="email" id="emailRequestBackup" style="width:300px;"
                                           title="<g:message code="emailRequestBackupMsg"/>"
                                           placeholder="<g:message code="emailLbl"/>"/>
                                </div>
                            </div>

                            <div layout horizontal>
                                <div flex></div>
                                <div>
                                    <button id="submitButton" type="submit" class="btn btn-default" style="margin:15px 20px 20px 0px; width:300px;">
                                        <g:message code="initManifestProtocolSimulationButton"/>
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </section>
            <section id="page2">
                <div cross-fade>
                    <simulation-listener id="simulationListener" page="{{subpage}}"
                         pagetitle="<g:message code='claimSimulationListenerTitle'/>" subpage vertical layout></simulation-listener>
                </div>
            </section>
        </core-animated-pages>
    </template>
    <script>
        Polymer('manifest-simulation-form', {
            ready: function() {
                console.log(this.tagName + " - ready")
                this.$.requestBackup.addEventListener('click', function() {
                    if(this.$.requestBackup.checked) this.$.emailDiv.style.display = 'block'
                    else this.$.emailDiv.style.display = 'none'
                }.bind(this));

                if(isChrome()) {
                    alert('<g:message code="editorChromeErrorMsg"/>')
                }
            },
            closeSimulationListener:function(e, detail, sender) {
                console.log(this.tagName + " - closeSimulationListener")
                this.page = 0;
            },
            submitForm: function(e) {
                e.preventDefault()
                if(!this.isValidForm()) {
                    console.log("submitForm form with errors")
                    return
                }
                var dateBeginStr = new Date().formatWithTime()
                var dateFinishStr = getDatePickerValue('dateFinish', this.$.dateFinish).formatWithTime()
                var event = {subject:this.$.subject.value, content:this.$.textEditor.getData(),
                    dateBegin:dateBeginStr, dateFinish:dateFinishStr}
                var simulationData = {service:"manifestSimulationService", status:Status.INIT_SIMULATION,
                    accessControlURL:this.$.accessControlURL.value,
                    maxPendingResponses: this.$.maxPendingResponses.value,
                    numRequestsProjected: this.$.numRequestsProjected.value,
                    dateBeginDocument: dateBeginStr,
                    dateFinishDocument: dateFinishStr,
                    whenFinishChangeEventStateTo:this.$.eventStateOnFinishSelect.options[this.$.eventStateOnFinishSelect.selectedIndex].value,
                    backupRequestEmail:this.$.emailRequestBackup.value,
                    event:event}
                console.log(this.tagName + " - simulationData: " + JSON.stringify(simulationData))
                this.$.simulationListener.simulationData = simulationData;
                this.page = 1;
            },

            isValidForm: function(e) {
                this.messageToUser = null
                this.$.textEditorDiv.classList.remove("formFieldError");
                var formElements = this.$.formDataDiv.children
                for(var i = 0; i < formElements.length; i++) {
                    formElements[i].classList.remove("formFieldError");
                }
                if(!this.$.accessControlURL.validity.valid) {
                    this.$.accessControlURL.classList.add("formFieldError")
                    this.messageToUser = '<g:message code="emptyFieldLbl"/>'
                    return false
                }

                this.$.accessControlURL.value = this.$.accessControlURL.value
                var dateFinish = getDatePickerValue('dateFinish', this.$.dateFinish)
                if(dateFinish < new Date()) {
                    this.messageToUser = '<g:message code="dateFinishBeforeTodayERRORMsg"/>'
                    return false
                }

                if(this.$.textEditor.getData() == 0) {
                    this.messageToUser = '<g:message code="eventContentEmptyERRORMsg"/>'

                    this.$.textEditorDiv.classList.add("formFieldError");
                    return false
                }
                return true
            }
        });
    </script>
</polymer-element>