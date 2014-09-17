<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-socket', file: 'votingsystem-socket.html')}">

<polymer-element name="simulation-listener" attributes="subpage pagetitle">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
        </style>
        <div class="pageContentDiv" style="width: 1000px;">

            <div layout horizontal center center-justified style="width:100%;">
                <template if="{{subpage}}">
                    <votingsystem-button isFab on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                        <i class="fa fa-arrow-left"></i></votingsystem-button>
                </template>
                <div flex id="pageTitle" class="pageHeader" style="text-align: center;"><h3>{{pagetitle}}</h3></div>
            </div>

            <div style="display:{{isProcessing? 'block':'none'}}">
                <div layout vertical center center-justified  style="text-align:center;min-height: 150px;">
                    <progress style="margin:20px auto;"></progress>
                </div>
            </div>

            <template if="{{message}}">
                <div id="messageFromService" class="messageFromServiceBox">

                <div layout horizontal style="margin: 0 0 20px 0;">
                    <div layout horizontal center center-justified>
                        <label style="font-weight: bold;"><g:message code="statusCodelLbl"/>:</label>
                        <div flex style="margin: 0 20px 0 0;font-size: 2em;font-weight: bold;" id="statusCode">{{message.statusCode}}</div>
                    </div>
                    <div layout horizontal center center-justified style="margin: 0 20px 0 50px;">
                        <label style="font-weight: bold;"><g:message code="timDurationLbl"/>:</label>
                        <div flex style="font-family:digi;font-size: 2em;">{{message.simulationData.timeDuration}}</div>
                    </div>
                </div>
                <div style="display:{{message.message?'block':'none'}}">
                    <div layout horizontal>
                        <div style="font-weight: bold;margin: 0 7px 20px 0;"><g:message code="messageLbl"/>:</div>
                        <div id="message">{{message.message}}</div>
                    </div>
                </div>
                <div id="numRequestsDiv" style="margin: 0 0 15px 0;">
                    <label style="font-weight: bold;"><g:message code="numRequestsLbl"/>:</label><label>{{message.simulationData.numRequestsProjected}}</label>
                </div>
                <div id="numRequestsOKDiv" style="width: 700px; margin: 0 0 15px 0;">
                    <label style="font-weight: bold;"><g:message code="numRequestsOKLbl"/>:</label><label>{{message.simulationData.numRequestsOK}}</label>
                </div>
                <div id="numRequestsERRORDiv" style="width: 700px; margin: 0 0 15px 0;">
                    <label style="font-weight: bold;"><g:message code="numRequestsERRORLbl"/>:</label><label>{{message.simulationData.numRequestsERROR}}</label>
                </div>
                <div id="errorsDiv" style="display: {{message.simulationData.errorList.length >0 ?'block':'none'}}">
                    <label style="font-weight: bold;"><g:message code="errorsListLbl"/>:</label>
                    <div>{{message.simulationData.errorList}}</div>
                </div>

        </div>
            </template>

        </div>
        <votingsystem-socket id="socketvs"  socketservice="${grailsApplication.config.grails.socketServiceURL}">
        </votingsystem-socket>
    </template>
    <script>
        Polymer('simulation-listener', {
            publish: {
                simulationData: {value: {}}
            },
            subpage:false,
            isProcessing:false,
            simulationDataChanged: function() {
                console.log(this.tagName + " - simulationDataChanged")
                this.$.socketvs.sendMessage(JSON.stringify(this.simulationData))
                if(this.simulationData.status == "INIT_SIMULATION") {
                    this.isProcessing = true
                }
            },
            ready: function() {
                console.log(this.tagName + " - ready - subpage: " + this.subpage +
                        " - socketvs service: ${grailsApplication.config.grails.socketServiceURL}")
                this.$.socketvs.addEventListener('on-message', function(e) {
                    console.log(this.tagName + " - socketvs - message: " + e.detail)
                    this.processSocketMessage(e.detail)
                }.bind(this));
            },
            back:function() {
                this.fire('core-signal', {name: "simulation-listener-closed", data: null});
                this.message = null
            },
            submitClaim:function() {
                console.log("submitClaim")
            },
            processSocketMessage:function(socketMessage){
                this.message = socketMessage
                if(this.message.statusCode == ResponseVS.SC_PROCESSING) {
                    this.isProcessing = true
                }  else this.isProcessing = false
                if(this.message.status == "FINISH_SIMULATION") this.isProcessing = false
            }
        });
    </script>
</polymer-element>