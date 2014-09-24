<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/simulation-listener']"/>">


<polymer-element name="timestamp-form">
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
                        <div id="pageTitle" class="pageHeader"><h3><g:message code="initMultiSignProtocolSimulationMsg"/></h3></div>
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
                        <form on-submit="{{submitForm}}">
                            <input id="resetForm" type="reset" style="display:none;">

                            <div>
                                <label><g:message code="numRequestsProjectedLbl"/></label>
                                <input type="number" id="numRequestsProjected" min="1" value="1" required
                                       style="width:110px;margin:0px 20px 0px 3px;"
                                       title="<g:message code="numRequestsProjectedLbl"/>"
                                       placeholder="<g:message code="numRequestsProjectedLbl"/>"/>
                                <label><g:message code="maxPendingResponsesLbl"/></label>
                                <input type="number" id="maxPendingResponses" min="1" value="10" required
                                       style="width:110px;margin:10px 20px 0px 3px;"
                                       title="<g:message code="maxPendingResponsesLbl"/>"
                                       placeholder="<g:message code="maxPendingResponsesLbl"/>"/>
                            </div>

                            <div style="margin:10px 0px 10px 0px">
                                <input type="number" name="eventId" id="eventId" min="1" value="1" style="width:350px"  required
                                       title="<g:message code="eventIdLbl"/>"
                                       placeholder="<g:message code="eventIdLbl"/>"/>
                                <input type="url" id="timeStampServerURL" style="width:300px; margin:0px 0px 0px 20px;" required
                                       value="http://www.sistemavotacion.org/TimeStampServer"
                                       title="<g:message code="timeStampServerURLMsg"/>"
                                       placeholder="<g:message code="timeStampServerURLMsg"/>"/>
                            </div>

                            <button id="submitButton" type="submit" class="btn btn-warning"
                                    style="margin:15px 20px 20px 0px; float:right;">
                                <g:message code="initTimeStampProtocolSimulationButton"/>
                            </button>
                        </form>
                    </div>
                </div>
            </section>
            <section id="page2">
                <div cross-fade class="pageContentDiv" style="width:100%; margin:30px 20px 0px 20px;">
                    <simulation-listener vertical layout subpage id="simulationListener" page="{{subpage}}"
                                         pagetitle="<g:message code='listeningtVicketUserBaseDataSimulationMsg'/>">
                    </simulation-listener>
                </div>
            </section>
        </core-animated-pages>
    </template>
    <script>
        Polymer('timestamp-form', {
            isWithTimer:false,
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            closeSimulationListener:function(e, detail, sender) {
                console.log(this.tagName + " - closeSimulationListener")
                this.page = 0;
            },
            withTimer:function() {
                this.isWithTimer = this.$.timerCheckBox.checked
            },
            submitForm: function(e) {
                e.preventDefault()
                var timerDataValue = null
                if(this.$.timerData != undefined && this.$.timerData.validity.valid) timerDataValue = this.$.timerData.value
                console.log("timerDataValue: " + timerDataValue)


                if(!this.isValidForm()) {
                    console.log("invalid form")
                    return
                }

                var simulationData = {service:"timeStampSimulationService", status:"INIT_SIMULATION",
                    serverURL:this.$.timeStampServerURL.value,
                    maxPendingResponses: this.$.maxPendingResponses.value,
                    numRequestsProjected: this.$.numRequestsProjected.value,
                    eventId:this.$.value}

                console.log(this.tagName + " - simulationData: " + JSON.stringify(simulationData))
                this.$.simulationListener.simulationData = simulationData;
                this.page = 1;
            },

            isValidForm: function(e) {
                this.messageToUser = null
                var formElements = this.$.formDataDiv.children
                for(var i = 0; i < formElements.length; i++) {
                    formElements[i].classList.remove("formFieldError");
                }
                if(!this.$.timeStampServerURL.validity.valid) {
                    this.$.serverURL.classList.add("formFieldError")
                    this.messageToUser = '<g:message code="emptyFieldLbl"/>'
                    return false
                }
                return true
            }
        });
    </script>
</polymer-element>