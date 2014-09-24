<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/simulation-listener.gsp']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-add-option-dialog']"/>">


<polymer-element name="election-simulation-form">
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

                    <div layout horizontal center center-justified style="margin: 15px 0px 15px 0px; width: 100%;">
                        <div id="pageTitle" class="pageHeader"><h3><g:message code="initElectionProtocolSimulationMsg"/></h3></div>
                    </div>

                    <div style="display:{{messageToUser? 'block':'none'}}">
                        <div>
                            <div class="messageToUser"  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                                <div id="messageToUser">{{messageToUser}}</div>
                            </div>
                            <paper-shadow z="1"></paper-shadow>
                        </div>
                    </div>

                    <div id="formDataDiv">
                        <form id="electionProtocolSimulationDataForm" on-submit="{{submitForm}}">

                            <fieldset id="userBaseData">
                                <legend style="font-size: 1.2em"><g:message code="userBaseDataCaption"/></legend>
                                <div style="margin: 0px 0px 5px 0px;">
                                    <label><g:message code="firstUserIndexMsg"/></label>
                                    <input type="number" id="firstUserIndex" min="1" value="1" readonly required
                                           class="userBaseDataInputNumber"
                                           style="width:120px;margin:10px 20px 0px 7px;"
                                           title="<g:message code="firstUserIndexMsg"/>"
                                           placeholder="<g:message code="firstUserIndexMsg"/>"/>

                                </div>
                                <div style="margin: 0px 0px 5px 0px;">
                                    <label><g:message code="numUsersWithoutRepresentativeMsg"/></label>
                                    <input type="number" id="numUsersWithoutRepresentative" min="0" value="1" required
                                           class="userBaseDataInputNumber"
                                           style="width:120px;margin:10px 20px 0px 7px;"
                                           title="<g:message code="numRepresentativesMsg"/>"
                                           placeholder="<g:message code="numRepresentativesMsg"/>"/>


                                    <label><g:message code="numUsersWithoutRepresentativeWithVoteMsg"/></label>
                                    <input type="number" id="numUsersWithoutRepresentativeWithVote" min="0" value="1" required
                                           class="userBaseDataInputNumber"
                                           style="width:120px;margin:10px 20px 0px 7px;"
                                           title="<g:message code="numRepresentativesWithVoteMsg"/>"
                                           placeholder="<g:message code="numRepresentativesWithVoteMsg"/>"/>
                                </div>
                                <div style="margin: 0px 0px 5px 0px;">
                                    <label><g:message code="numRepresentativesMsg"/></label>
                                    <input type="number" id="numRepresentatives" min="0" value="1" required
                                           class="userBaseDataInputNumber"
                                           style="width:120px;margin:10px 20px 0px 7px;"
                                           title="<g:message code="numRepresentativesMsg"/>"
                                           placeholder="<g:message code="numRepresentativesMsg"/>"/>


                                    <label><g:message code="numRepresentativesWithVoteMsg"/></label>
                                    <input type="number" id="numRepresentativesWithVote" min="0" value="1" required
                                           class="userBaseDataInputNumber"
                                           style="width:120px;margin:10px 20px 0px 7px;"
                                           title="<g:message code="numRepresentativesWithVoteMsg"/>"
                                           placeholder="<g:message code="numRepresentativesWithVoteMsg"/>"/>
                                </div>

                                <div style="margin: 0px 0px 5px 0px;">
                                    <label><g:message code="numUsersWithRepresentativeMsg"/></label>
                                    <input type="number" id="numUsersWithRepresentative" min="0" value="1" required
                                           class="userBaseDataInputNumber"
                                           style="width:120px;margin:10px 20px 0px 7px;"
                                           title="<g:message code="numUsersWithRepresentativeMsg"/>"
                                           placeholder="<g:message code="numUsersWithRepresentativeMsg"/>"/>

                                    <label><g:message code="numUsersWithRepresentativeWithVoteMsg"/></label>
                                    <input type="number" id="numUsersWithRepresentativeWithVote" min="0" value="1" required
                                           class="userBaseDataInputNumber"
                                           style="width:120px;margin:10px 20px 0px 7px;"
                                           title="<g:message code="numUsersWithRepresentativeWithVoteMsg"/>"/>
                                </div>
                            </fieldset>

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

                            <div class="fieldsBox" style="display:{{eventvsOptionList.length == 0? 'none':'block'}}">
                                <fieldset>
                                    <legend><g:message code="eventvsFieldsLegend"/></legend>
                                    <div layout vertical>
                                        <template repeat="{{eventvsOption in eventvsOptionList}}">
                                            <div>
                                                <a class="btn btn-default" on-click="{{removeEventVSOption}}" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                                    <g:message code="deleteLbl"/> <i class="fa fa-minus"></i></a>
                                                {{eventvsOption}}
                                            </div>
                                        </template>
                                    </div>
                                </fieldset>
                            </div>


                            <div layout horizontal>
                                <div flex></div>
                                <button id="addElectionFieldButton" type="button" class="btn btn-default"
                                        style="margin:15px 20px 20px 0px;" on-click="{{addElectionField}}">
                                    <g:message code="addElectionFieldLbl"/>
                                </button>
                            </div>

                            <div layout horizontal>
                                <div flex></div>
                                <div>
                                    <button id="submitButton" type="submit" class="btn btn-default" style="margin:15px 20px 20px 0px; width:300px;">
                                        <g:message code="initElectionProtocolSimulationButton"/>
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
                                 pagetitle="<g:message code='electionSimulationListenerTitle'/>" subpage vertical layout></simulation-listener>
                </div>
            </section>
        </core-animated-pages>
        <eventvs-add-option-dialog id="eventvsOptionDialog"></eventvs-add-option-dialog>
    </template>
    <script>
        Polymer('election-simulation-form', {
            eventvsOptionList : [],
            messageToUser:null,
            testPage: function() {
                this.page = 1;
            },
            ready: function() {
                if(isChrome()) {
                    alert('<g:message code="editorChromeErrorMsg"/>')
                }
                console.log(this.tagName + " - ready")
                this.$.requestBackup.addEventListener('click', function() {
                    if(this.$.requestBackup.checked) this.$.emailDiv.style.display = 'block'
                    else this.$.emailDiv.style.display = 'none'
                }.bind(this));
                this.$.eventvsOptionDialog.addEventListener('on-submit', function(e) {
                    this.eventvsOptionList.push(e.detail)
                }.bind(this));
                this.$.numRepresentativesWithVote.addEventListener('change', this.checkRangeRepresentatives.bind(this), false);
                this.$.numRepresentatives.addEventListener('change', this.checkRangeRepresentatives.bind(this), false);
                this.$.numUsersWithRepresentative.addEventListener('change', this.checkRangeUsersWithRepresentative.bind(this), false);
                this.$.numUsersWithRepresentativeWithVote.addEventListener('change', this.checkRangeUsersWithRepresentative.bind(this), false);
            },
            checkRangeRepresentatives: function() {
                console.log("checkRangeRepresentatives")
                var representativeWithVote = Number(document.getElementById('numRepresentativesWithVote').value)
                var representatives = Number(document.getElementById('numRepresentatives').value)
                if (representativeWithVote > representatives) {
                    this.messageToUser = "<g:message code='representativeRangeErrorMsg'/>"
                    return false
                } else {
                    this.messageToUser = null
                    return true
                }
            },
            checkRangeUsersWithRepresentative: function() {
                console.log("checkRangeUsersWithRepresentative")
                var usersWithRepresentative = Number(document.getElementById('numUsersWithRepresentative').value)
                var usersWithRepresentativeWithVote = Number(document.getElementById('numUsersWithRepresentativeWithVote').value)
                if (usersWithRepresentativeWithVote > usersWithRepresentative) {
                    this.messageToUser = "<g:message code='usersWithRepresentativeRangeErrorMsg'/>"
                } else this.messageToUser = null
            },
            closeSimulationListener:function(e, detail, sender) {
                console.log(this.tagName + " - closeSimulationListener")
                this.page = 0;
            },
            removeEventVSOption: function(e) {
                var eventvsOption = e.target.templateInstance.model.eventvsOption
                console.log("removePollOption")
                for(optionIdx in this.eventvsOptionList) {
                    console.log("option: " +  this.eventvsOptionList[optionIdx] + " - eventvsOption: " + eventvsOption)
                    if(eventvsOption == this.eventvsOptionList[optionIdx]) {
                        this.eventvsOptionList.splice(optionIdx, 1)
                    }
                }
            },
            submitForm: function(e) {
                e.preventDefault()
                if(!this.isValidForm()) {
                    console.log("submitForm form with errors")
                    return
                }
                var dateBeginStr = new Date().formatWithTime()
                var dateFinishStr = getDatePickerValue('dateFinish', this.$.dateFinish).formatWithTime()
                var eventVSOptions = []
                for(optionIdx in this.eventvsOptionList) {
                    eventVSOptions.push({content:this.eventvsOptionList[optionIdx]})
                }

                var event = {subject:this.$.subject.value, content:this.$.textEditor.getData(),
                    dateBegin:dateBeginStr, dateFinish:dateFinishStr, fieldsEventVS:eventVSOptions}

                var userBaseData = {userIndex:this.$.firstUserIndex.value,
                    numUsersWithoutRepresentative: this.$.numUsersWithoutRepresentative.value,
                    numUsersWithoutRepresentativeWithVote: this.$.numUsersWithoutRepresentativeWithVote.value,
                    numRepresentatives: this.$.numRepresentatives.value,
                    numRepresentativesWithVote: this.$.numRepresentativesWithVote.value,
                    numUsersWithRepresentative: this.$.numUsersWithRepresentative.value,
                    numUsersWithRepresentativeWithVote: this.$.numUsersWithRepresentativeWithVote.value
                }

                var simulationData = {service:"electionSimulationService", status:Status.INIT_SIMULATION,
                    userBaseData:userBaseData,
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

            addElectionField: function(e) {
                this.$.eventvsOptionDialog.show()
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
                if(this.eventvsOptionList.length < 2) {
                    this.messageToUser = '<g:message code="missingOptionsERRORMsg"/>'
                    this.$.addElectionFieldButton.classList.add("formFieldError");
                    return false
                }
                if(!this.checkRangeRepresentatives()) return false
                return true
            }
        });
    </script>
</polymer-element>