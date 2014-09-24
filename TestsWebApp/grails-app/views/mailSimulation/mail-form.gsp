<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/simulation-listener']"/>">


<polymer-element name="mail-form">
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
                        <div id="pageTitle" class="pageHeader"><h3><g:message code="initEncryptionProtocolSimulationMsg"/></h3></div>
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
                        <form id="encryptionProtocolSimulationDataForm" on-submit="{{submitForm}}">
                            <input id="resetForm" type="reset" style="display:none;">

                            <div>
                                <label><g:message code="numRequestsProjectedLbl"/></label>
                                <input type="number" id="numRequestsProjected" min="1" value="1" required
                                       style="width:110px;margin:0px 20px 0px 3px;"
                                       title="<g:message code="numRequestsProjectedLbl"/>"
                                       placeholder="<g:message code="numRequestsProjectedLbl"/>"/>
                                <label ><g:message code="maxPendingResponsesLbl"/></label>
                                <input type="number" id="maxPendingResponses" min="1" value="10" required
                                       style="width:110px;margin:10px 20px 0px 3px;"
                                       title="<g:message code="maxPendingResponsesLbl"/>"
                                       placeholder="<g:message code="maxPendingResponsesLbl"/>"/>
                            </div>


                            <div style="margin:10px 0px 10px 0px">
                                <label><g:message code="smtpHostNameMsg"/></label>
                                <input id="smtpHostName" style="width:500px; margin:0px 0px 0px 3px;"
                                       value="localhost"
                                       title="<g:message code="smtpHostNameMsg"/>"
                                       placeholder="<g:message code="smtpHostNameMsg"/>"/>
                            </div>

                            <div style="margin:10px 0px 10px 0px">
                                <label><g:message code="pop3HostNameMsg"/></label>
                                <input id="pop3HostName" style="width:500px; margin:0px 0px 0px 3px;" required
                                       value="localhost"
                                       title="<g:message code="pop3HostNameMsg"/>"
                                       placeholder="<g:message code="pop3HostNameMsg"/>"/>
                            </div>


                            <div style="margin:10px 0px 10px 0px">
                                <label><g:message code="userNameMsg"/></label>
                                <input id="userName" style="width:500px; margin:0px 0px 0px 3px;" required
                                       value="voting_system_access_control"
                                       title="<g:message code="userNameMsg"/>"
                                       placeholder="<g:message code="userNameMsg"/>"/>
                            </div>

                            <div style="margin:10px 0px 10px 0px">
                                <label><g:message code="domainNameMsg"/></label>
                                <input id="domainName" style="width:500px; margin:0px 0px 0px 3px;" required
                                       value="sistemavotacion.org"
                                       title="<g:message code="domainNameMsg"/>"
                                       placeholder="<g:message code="domainNameMsg"/>"/>
                            </div>

                            <div style="margin:10px 0px 10px 0px">
                                <label><g:message code="passwordMsg"/></label>
                                <input id="password" style="width:500px; margin:0px 0px 0px 3px;" required
                                       value="123456"
                                       title="<g:message code="passwordMsg"/>"
                                       placeholder="<g:message code="passwordMsg"/>"/>
                            </div>

                            <div style="margin:10px 0px 10px 10px;  height: 50px; ">
                                <div class="checkBox" style="vertical-align: middle;">
                                    <input type="checkbox" id="timerCheckBox" on-click="{{withTimer}}"/><label><g:message code="simulationTimerDataMsg"/></label>
                                </div>
                                <template if="{{isWithTimer}}">
                                    <div style="vertical-align: middle;display: {{isWithTimer?'block':'none'}}">
                                        <input type="time" id="timerData" style="" pattern="^([01]\d|2[0-3]):?([0-5]\d)" required
                                               title="<g:message code="simulationTimerDataMsg"/>"
                                               placeholder="<g:message code="simulationTimerDataMsg"/>"/>
                                    </div>
                                </template>
                            </div>

                            <div layout horizontal style="margin:15px 0 0 0;">
                                <div flex></div>
                                <div>
                                    <button type="submit" class="btn btn-default">
                                        <g:message code="initMailProtocolSimulationButton"/>
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
                         pagetitle="<g:message code='listeningEncryptionProtocolSimulationMsg'/>" subpage vertical layout></simulation-listener>
                </div>
            </section>
        </core-animated-pages>
    </template>
    <script>
        Polymer('mail-form', {
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
                var simulationData = {service:"mailSimulationService", status:"INIT_SIMULATION",
                    smtpHostName:this.$.smtpHostName.value,
                    pop3HostName:this.$.pop3HostName.value,
                    userName: this.$.userName.value,
                    domainName: this.$.domainName.value,
                    password: this.$.password.value,
                    maxPendingResponses: this.$.maxPendingResponses.value,
                    numRequestsProjected: this.$.numRequestsProjected.value,
                    timer:timerDataValue}

                console.log(this.tagName + " - simulationData: " + JSON.stringify(simulationData))
                this.$.simulationListener.simulationData = simulationData;
                this.page = 1;
            }
        });
    </script>
</polymer-element>