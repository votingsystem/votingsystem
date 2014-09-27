<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/simulation-listener']"/>">


<polymer-element name="user-togroup-form">
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
                        <div id="pageTitle" class="pageHeader"><h3><g:message code="addUsersToGroupButton"/></h3></div>
                    </div>

                    <div style="display:{{messageToUser? 'block':'none'}}">
                        <div class="messageToUser">
                            <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                                <div id="messageToUser">{{messageToUser}}</div>
                            </div>
                            <paper-shadow z="1"></paper-shadow>
                        </div>
                    </div>

                    <form on-submit="{{submitForm}}">
                    <div layout horizontal center center-justified id="formDataDiv" style="margin:0 auto;">
                        <div style="margin:0 10px 0 0;">
                            <label class="" style=""><g:message code="groupIdMsg"/></label>
                            <input type="number" id="groupId" name="groupId" class="form-control" style="width: 150px;" required/>
                        </div>

                        <div style="margin:0 10px 0 0;">
                            <label class="" style=""><g:message code="numUsersLbl"/></label>
                            <input type="number" id="numUsers" name="numUsers" min="0" value="1" class="form-control"
                                   style="width: 150px;"/>
                        </div>
                        <div style="margin:0 10px 0 0;">
                            <label class="" style=""><g:message code="userIndexLbl"/></label>
                            <input type="number" id="userIndex" name="userIndex" min="0" value="1" class="form-control"
                                   style="width: 150px;"/>
                        </div>
                        <div style="margin:0 10px 0 0;">
                            <label class="" style=""><g:message code="vicketServerLbl"/></label>
                            <input type="url" id="vicketServerURL"  class="form-control" style=""
                                   value="http://vickets:8086/Vickets/" placeholder="<g:message code="vicketServerURLMsg"/>"/>
                        </div>
                    </div>
                    <div style=float:right;">
                        <button id="addUsersToGroupButton" type="submit" class="btn btn-warning" style="margin:15px 20px 20px 0px; float:right; ">
                            <g:message code="addUsersToGroupButton"/>
                        </button>
                    </div>
                    </form>
                </div>
            </section>
            <section id="page2">
                <div cross-fade class="pageContentDiv" style="width:100%; margin:30px 20px 0px 20px;">
                    <simulation-listener vertical layout subpage id="simulationListener" page="{{subpage}}"
                             pagetitle="<g:message code='listeningAddUsersToGroup'/>">
                    </simulation-listener>
                </div>
            </section>
        </core-animated-pages>
    </template>
    <script>
        Polymer('user-togroup-form', {
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

                var userBaseData = {numUsers: this.$.numUsers.value, userIndex:this.$.userIndex.value }
                var simulationData = {service:'vicketAddUsersToGroupSimulationService', status:"INIT_SIMULATION",
                    groupId:this.$.groupId.value, serverURL:this.$.vicketServerURL.value, userBaseData:userBaseData}


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