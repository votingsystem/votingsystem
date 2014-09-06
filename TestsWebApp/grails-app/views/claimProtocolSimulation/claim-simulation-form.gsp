<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/simulation-listener.gsp']"/>">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">


<polymer-element name="claim-simulation-form">
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
                <div class="pageContentDiv" style="width:100%;max-width: 1000px; padding:0px 20px 0px 20px;">

                    <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
                        <div id="pageTitle" class="pageHeader"><h3><g:message code="initClaimProtocolSimulationMsg"/></h3></div>
                        <div  id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
                            <button id="testButton" type="button" class="btn btn-default" style="margin:0px 0px 0px 30px;">
                                <g:message code="goToResultViewMsg"/>
                            </button>
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

                    <div id="formDataDiv">
                        <form id="claimProtocolSimulationDataForm" on-submit="{{submitForm}}">
                            <input type="hidden" autofocus="autofocus" />
                            <input id="resetClaimProtocolSimulationDataForm" type="reset" style="display:none;">

                            <div style="display: block;">
                                <label><g:message code="numRequestsProjectedLbl"/></label>
                                <input type="number" id="numRequestsProjected" min="1" value="1" required
                                       style="width:130px;margin:0px 20px 0px 3px;"
                                       title="<g:message code="numRequestsProjectedLbl"/>"
                                       placeholder="<g:message code="numRequestsProjectedLbl"/>"
                                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                                       onchange="this.setCustomValidity('')">
                                <label><g:message code="maxPendingResponsesLbl"/></label>
                                <input type="number" id="maxPendingResponses" min="1" value="10" required
                                       style="width:130px;margin:10px 20px 0px 3px;"
                                       title="<g:message code="maxPendingResponsesLbl"/>"
                                       placeholder="<g:message code="maxPendingResponsesLbl"/>"
                                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                                       onchange="this.setCustomValidity('')">
                            </div>

                            <div>
                                <label><g:message code="eventStateOnFinishLbl"/></label>
                                <select id="eventStateOnFinishSelect" style="margin:0px 20px 0px 0px;"
                                        title="<g:message code="setEventStateLbl"/>">
                                    <option value=""> - <g:message code="eventAsDateRangeLbl"/> - </option>
                                    <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="eventCancelledLbl"/> - </option>
                                    <option value="DELETED" style="color:#cc1606;"> - <g:message code="eventDeletedLbl"/> - </option>
                                </select>
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
                                       placeholder="<g:message code="accessControlURLMsg"/>"
                                       oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                                       onchange="this.setCustomValidity('')"/>
                            </div>

                            <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>

                            <div layout horizontal center id="backupDiv" style="margin:10px 0px 10px 10px; height: 50px;">
                                <div class="checkBox">
                                    <input type="checkbox" id="requestBackup"/><label><g:message code="requestBackupLbl"/></label>
                                </div>
                                <div id="emailDiv"style="display:none;">
                                    <input type="email" id="emailRequestBackup" style="width:300px;" required
                                           title="<g:message code="emailRequestBackupMsg"/>"
                                           placeholder="<g:message code="emailLbl"/>"
                                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                                           onchange="this.setCustomValidity('')">
                                </div>
                            </div>

                            <div style="margin:0px 0px 0px 0px; overflow: hidden;width: 100%;">
                                <button id="addClaimFieldButton" type="button" class="btn btn-default" style="margin:15px 20px 20px 0px; float:right;">
                                    <g:message code="addClaimFieldLbl"/>
                                </button>
                            </div>

                            <fieldset id="fieldsBox" class="fieldsBox" style="display:none;">
                                <legend id="fieldsLegend"><g:message code="claimsFieldLegend"/></legend>
                                <div id="fields"></div>
                            </fieldset>
                            <div style="position: relative; overflow:hidden; ">
                                <button id="submitButton" type="submit" class="btn btn-default" style="margin:15px 20px 20px 0px; width:450px; float:right;">
                                    <g:message code="initClaimProtocolSimulationButton"/>
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </section>
            <section id="page2">
                <div cross-fade>
                    <simulation-listener id="simulationListener" page="{{subpage}}" subpage vertical layout></simulation-listener>
                </div>
            </section>
        </core-animated-pages>
    </template>
    <script>
        Polymer('claim-simulation-form', {
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            closeSimulationListener:function(e, detail, sender) {
                console.log(this.tagName + " - closeSimulationListener")
                this.page = 0;
            },
            submitForm: function() {
                console.log("submitForm")

            }
        });
    </script>
</polymer-element>