<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/simulation-listener']"/>">


<polymer-element name="deposit-form">
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
                            <input type="hidden" autofocus="autofocus" />
                            <input id="resetvicketDepositSimulationDataForm" type="reset" style="display:none;">
                            <fieldset id="Deposit">
                                <legend style="font-size: 1.2em; font-weight: bold;"><g:message code="depositCaption"/></legend>
                                <div style="margin: 0px 0px 5px 0px;">
                                    <div layout horizontal>
                                        <input type="number" id="receptorId" style="width:180px; margin:0 10px 0 0;" required
                                               title="<g:message code="userIdLbl"/>" class="form-control"
                                               placeholder="<g:message code="userIdLbl"/>"/>
                                        <input type="text" id="subject" style="width:280px;" required
                                               title="<g:message code="transactionSubjectMsg"/>" class="form-control"
                                               placeholder="<g:message code="transactionSubjectMsg"/>"/>

                                        <input type="url" id="vicketServerURL" style="margin:0px 0px 0px 20px;width:280px;" required
                                               value="http://vickets/Vickets/" title="<g:message code="vicketServerURLMsg"/>"
                                               placeholder="<g:message code="vicketServerURLMsg"/>" class="form-control"/>
                                    </div>
                                    <div style="margin:10px 0px 0px 0px;">
                                        <div layout horizontal>
                                            <label style="margin:0px 0px 0px 0px;"><g:message code="depositAmount"/></label>
                                            <input type="number" id="depositAmount" min="0" value="1" required
                                                   class="DepositInputNumber form-control"
                                                   style="width:150px;margin:0px 20px 0px 7px;"
                                                   title="<g:message code="depositAmount"/>"
                                                   placeholder="<g:message code="depositAmount"/>"/>
                                            <select id="currencySelect" style="margin:0px 20px 0px 0px; width:280px;"
                                                    class="form-control" title="<g:message code="currencyLbl"/>">
                                                <option value="<g:message code="euroLbl"/>"> - <g:message code="euroLbl"/> - </option>
                                            </select>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <button id="submitButton" type="submit" class="btn btn-warning" style="margin:25px 20px 20px 0px; float:right;">
                                <g:message code="initVicketDepositButton"/>
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
        Polymer('deposit-form', {
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

                var currencyElement = document.getElementById("currencySelect");
                var currenSelected = currencyElement.options[currencyElement.selectedIndex].value;
                //var strUser = currencyElement.options[currencyElement.selectedIndex].text;

                var simulationData = {service:'vicketDepositSimulationService', status:"INIT_SIMULATION",
                    receptorId:this.$.receptorId.value,
                    serverURL:this.$.vicketServerURL.value,  depositAmount: this.$.depositAmount.value,
                    subject:this.$.subject.value, currency:currenSelected}

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
                    this.$.vicketServerURL.classList.add("formFieldError")
                    this.messageToUser = '<g:message code="emptyFieldLbl"/>'
                    return false
                }
                return true
            }
        });
    </script>
</polymer-element>