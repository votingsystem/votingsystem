<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/simulation-listener']"/>">


<polymer-element name="user-base-form">
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
                        <div id="pageTitle" class="pageHeader"><h3><g:message code="initVicketUserBaseDataSimulationMsg"/></h3></div>
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
                            <input id="resetvicketUserBaseDataSimulationDataForm" type="reset" style="display:none;">
                            <fieldset id="userBaseData">
                                <legend style="font-size: 1.2em; font-weight: bold;"><g:message code="userBaseDataCaption"/></legend>
                                <div layout horizontal style="margin: 0px 0px 5px 0px;">
                                    <div>
                                        <label><g:message code="firstUserIndexMsg"/></label>
                                        <input type="number" id="firstUserIndex" min="1" value="1" readonly required
                                               class="userBaseDataInputNumber form-control"
                                               style="width:120px;margin:10px 20px 0px 7px;"
                                               title="<g:message code="firstUserIndexMsg"/>"
                                               placeholder="<g:message code="firstUserIndexMsg"/>"/>
                                    </div>
                                    <div style="margin: 0px 20px 0px 20px;">
                                        <label><g:message code="numUsersMsg"/></label>
                                        <input type="number" id="numUsers" min="0" value="1" required
                                               class="userBaseDataInputNumber form-control"
                                               style="width:120px;margin:10px 20px 0px 7px;"
                                               title="<g:message code="numRepresentativesMsg"/>"
                                               placeholder="<g:message code="numRepresentativesMsg"/>"/>
                                    </div>
                                    <div>
                                        <label><g:message code="serverURLbl"/></label>
                                        <input type="url" id="vicketServerURL"  class="form-control" style="width:280px;margin:10px 20px 0px 7px;" required
                                               value="http://vickets/Vickets/" title="<g:message code="vicketServerURLMsg"/>"
                                               placeholder="<g:message code="vicketServerURLMsg"/>"/>
                                    </div>
                                </div>
                            </fieldset>

                            <div>
                                <button type="submit" class="btn btn-warning" style="margin:15px 20px 20px 0px; float:right;">
                                    <g:message code="initVicketuserBaseDataButton"/>
                                </button>
                            </div>
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
        Polymer('user-base-form', {
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

                if(!this.isValidForm()) {
                    console.log("invalid form")
                    return
                }

                var userBaseData = {userIndex:this.$.firstUserIndex.value, numUsers: this.$.numUsers.value }

                var simulationData = {service:'vicketUserBaseDataSimulationService', status:"INIT_SIMULATION",
                    serverURL:this.$.vicketServerURL.value, userBaseData:userBaseData}

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
                if(!this.$.vicketServerURL.validity.valid) {
                    this.$.serverURL.classList.add("formFieldError")
                    this.messageToUser = '<g:message code="emptyFieldLbl"/>'
                    return false
                }
                return true
            }
        });
    </script>
</polymer-element>