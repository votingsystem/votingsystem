<%@ page contentType="text/html; charset=UTF-8" %>

<link href="eventvs-admin-dialog.vsp" rel="import"/>
<link href="votevs-result-dialog.vsp" rel="import"/>

<dom-module name="eventvs-election">
    <template>
        <style>
            .numVotesClass{
                font-size: 3em;
                font-style: italic;
                color: #888;
            }
        </style>
        <div>
            <div hidden="{{adminMenuHidden}}" style="text-align: center;">
                <button type="submit" on-click="showAdminDialog" style="margin:15px 20px 15px 0px;">
                    ${msg.adminDocumentLinkLbl} <i class="fa fa fa-check"></i>
                </button>
            </div>

            <div style="margin: 0px 30px;">
                <div class="layout horizontal center center-justified" style="width:100%;">
                    <div class="flex horizontal layout center center-justified">
                        <div hidden="{{!isActive}}" style='color: #888;font-size: 0.9em;'>{{getElapsedTime(eventvs.dateFinish)}}</div>
                    </div>
                    <div style="text-align: center">
                        <div id="pageTitle" data-eventvs-id$="{{eventvs.id}}" class="pageHeader">{{eventvs.subject}}</div>
                    </div>
                    <div class="flex" on-click="showVoteResul">
                        <div hidden="{{!votevsResul}}" style="cursor: pointer;background: #ffeb3b;width: 50px; text-align: center;">
                            <i class="fa fa-envelope" style="margin:10px;color: #ba0011;font-size: 1.3em;"></i> </div>
                    </div>
                </div>
                <div class="horizontal layout">
                    <div class="flex" style="display: block;">
                        <div hidden="{{!isPending}}" style="font-size: 1.3em; font-weight:bold;color:#fba131;">${msg.eventVSPendingMsg}</div>
                        <div hidden="{{!isTerminated}}" style="font-size: 1.3em; font-weight:bold;color:#cc1606;">${msg.eventVSFinishedLbl}</div>
                        <div hidden="{{!isCanceled}}" style="font-size: 1.3em; font-weight:bold;color:#cc1606;">${msg.eventVSCancelledLbl}</div>
                    </div>
                    <div hidden="{{isActive}}" style="margin:0px 30px 0px 30px; color: #888;"><b>${msg.dateLbl}: </b>
                        {{getDate(eventvs.dateBegin)}}</div>
                </div>
                <div>
                    <div id="eventContentDiv"></div>

                    <div class="horizontal layout center center-justified">
                        <div hidden="{{!isTerminated}}" style="margin: 10px 0 0 0;">
                            <button on-click="getResults">
                                <i class="fa fa-bar-chart"></i> ${msg.getResultsLbl}
                            </button>
                        </div>
                        <div id="eventAuthorDiv" class="flex" style="margin:0px 20px 0 30px; color:#888; font-size: 0.85em;">
                            <b>${msg.byLbl}:</b> <span>{{eventvs.userVS}}</span>
                        </div>
                    </div>

                    <div>
                        <div>
                            <div hidden="{{!isActive}}">
                                <div style="font-size: 1.4em; font-weight: bold; text-decoration: underline; color:#888;
                                        margin: 20px 0 0 0;">${msg.pollFieldLegend}:</div>
                                <template is="dom-repeat" items="{{eventvs.fieldsEventVS}}">
                                    <div>
                                        <button on-click="showConfirmDialog"
                                                style="margin: 30px 0px 0px 5px;font-size: 1.2em; font-weight:bold;width: 100%; max-width: 500px; padding: 10px;">
                                            <span>{{item.content}}</span>
                                        </button>
                                    </div>
                                </template>
                            </div>
                            <div hidden="{{isActive}}">
                                <div style="font-size: 1.4em; font-weight: bold; text-decoration: underline; color:#888;
                                        margin: 20px 0 10px 0;">${msg.pollFieldLegend}:</div>
                                <template is="dom-repeat" items="{{fieldsEventVS}}">
                                    <div class="horizontal layout center center-justified">
                                        <div class="voteOption" style="font-size: 2em; font-weight: bold;">
                                            - {{item.content}}
                                        </div>
                                        <div class="numVotesClass flex" style="display: none;margin:0 0 0 20px;">{{item.numVotesVS}} ${msg.votesLbl}</div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <eventvs-election-vote-confirm-dialog id="confirmOptionDialog" on-option-confirmed="submitVote">
            </eventvs-election-vote-confirm-dialog>
        <eventvs-admin-dialog id="eventVSAdminDialog" eventvs="{{eventvs}}"></eventvs-admin-dialog>
        <votevs-result-dialog id="votevsResultDialog"></votevs-result-dialog>
    </template>
    <script>
        Polymer({
            is:'eventvs-election',
            properties: {
                url:{type:String, observer:'getHTTP'},
                eventvs:{type:Object, observer:'eventvsChanged'},
            },
            ready: function() {
                console.log(this.tagName + "- ready")
                sendSignalVS({caption:"${msg.pollLbl}"})
                document.querySelector("#voting_system_page").addEventListener('messagedialog-accept',
                        function(e) { this.messagedialogConfirmed(e) }.bind(this))
            },
            decodeBase64:function(base64EncodedString) {
                if(base64EncodedString == null) return null
                return decodeURIComponent(escape(window.atob(base64EncodedString)))
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            getElapsedTime: function(dateStamp) {
                return new Date(dateStamp).getElapsedTime() + " ${msg.toCloseLbl}"
            },
            eventvsChanged:function() {
                console.log("eventvsChanged - eventvs: " + this.eventvs.state)
                this.optionVSSelected = null
                this.dateFinish = new Date(this.eventvs.dateFinish)
                this.votevsResul = null;
                this.isActive = false
                this.isPending = false
                this.isTerminated = false
                this.isCanceled = false
                if('PENDING' == this.eventvs.state) {
                    this.isPending = true
                } else if ('TERMINATED' == this.eventvs.state) {
                    this.isTerminated = true
                } else if ('CANCELED' == this.eventvs.state) {
                    this.isCanceled = true
                } else this.isActive = true
                this.adminMenuHidden = true
                if('admin' === menuType) {
                    if(this.eventvs.state === 'ACTIVE' || this.eventvs.state === 'PENDING') this.adminMenuHidden = false
                }
                this.fieldsEventVS = this.eventvs.fieldsEventVS
                d3.xhr(contextURL + "/rest/eventVSElection/id/" + this.eventvs.id + "/stats")
                        .header("Content-Type", "application/json").get(function(err, rawData){
                        if('TERMINATED' == this.eventvs.state) {
                            this.fieldsEventVS = toJSON(rawData.response).fieldsEventVS
                            this.async(function () { d3.select(this).selectAll(".numVotesClass").style("display", "block")}.bind(this))
                        }
                    }.bind(this));
                d3.select(this).select("#eventContentDiv").html(this.decodeBase64(this.eventvs.content))
            },
            showAdminDialog:function() {
                this.$.eventVSAdminDialog.show()
            },
            showConfirmDialog: function(e) {
                console.log(this.tagName + " showConfirmDialog")
                if((clientTool === undefined)) {
                    alert("${msg.clientToolRequiredErrorMsg}", "${msg.errorLbl}");
                } else {
                    this.optionVSSelected = e.model.item
                    //alert(message, caption, callerId, isConfirmMessage)
                    alert(this.optionVSSelected.content, "${msg.confirmOptionDialogCaption}", this.tagName, true);
                }
            },
            messagedialogConfirmed:function(e) {
                if(this.tagName === e.detail) {
                    this.submitVote()
                }
            },
            getResults:function() {
                console.log("getResults")
                var fileURL = "${contextURL}/static/backup/" + this.dateFinish.urlFormat() + "/VOTING_EVENT_" + this.eventvs.id + ".zip"
                if(clientTool !== undefined) {
                    var operationVS = new OperationVS(Operation.FILE_FROM_URL)
                    operationVS.subject = '${msg.downloadingFileMsg}'
                    operationVS.documentURL = fileURL
                    operationVS.setCallback(function(appMessage) { this.showGetResultsResponse(appMessage) }.bind(this))
                    VotingSystemClient.setMessage(operationVS)
                } else window.location.href  = fileURL
            },
            showGetResultsResponse:function(appMessage) {
                var appMessageJSON = toJSON(appMessage)
                if(ResponseVS.SC_OK !== appMessageJSON.statusCode) alert(appMessageJSON.message, "${msg.errorLbl}")
                this.click()//hack to refresh screen
            },
            showVoteResul:function() {
                this.$.votevsResultDialog.show(this.votevsResul)
            },
            submitVote:function() {
                console.log("submitVote - eventvs.url: " + this.eventvs.url)
                var voteVS = {optionSelected:this.optionVSSelected, eventVSId:this.eventvs.id, eventURL:this.eventvs.url}
                var operationVS = new OperationVS(Operation.SEND_VOTE)
                this.eventvs.voteVS = voteVS
                operationVS.voteVS = voteVS
                operationVS.signedMessageSubject = '${msg.sendVoteMsgSubject}'
                operationVS.setCallback(function(appMessage) { this.submitVoteResponse(appMessage)}.bind(this))
                console.log(" - operationVS: " +  JSON.stringify(operationVS))
                VotingSystemClient.setMessage(operationVS);
            },
            submitVoteResponse:function(appMessage) {
                console.log(this.tagName + " - vote callback - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                appMessageJSON.eventVS = this.eventvs
                appMessageJSON.optionSelected = this.optionVSSelected.content
                this.votevsResul = appMessageJSON
                this.$.votevsResultDialog.show(appMessageJSON)
                this.click(); //hack to refresh screen
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.eventvs = toJSON(rawData.response)
                }.bind(this));
            }
            });
    </script>
</dom-module>