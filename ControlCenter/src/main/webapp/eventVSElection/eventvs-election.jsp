<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="eventvs-election">
    <template>
        <style>
            .numVotesClass{
                font-size: 3em;
                font-style: italic;
                color: #888;
            }
        </style>
        <div style="margin: 0px 30px;">
            <div hidden="{{!isActive}}" style='color: #888;'>{{getElapsedTime(eventvs.dateFinish)}}</div>
            <div class="layout horizontal center center-justified" style="width:100%;">
                <div class="flex" style="text-align: center">
                    <div id="pageTitle" data-eventvs-id$="{{eventvs.id}}" class="pageHeader">{{eventvs.subject}}</div>
                </div>
            </div>
            <div class$="{{eventStateRowClass}}">
                <div class="flex" style="display: block;">
                    <div hidden="{{!isPending}}" style="font-size: 1.3em; font-weight:bold;color:#fba131;">${msg.eventVSPendingMsg}</div>
                    <div hidden="{{!isTerminated}}" style="font-size: 1.3em; font-weight:bold;color:#cc1606;">${msg.eventVSFinishedLbl}</div>
                    <div hidden="{{!isCanceled}}" style="font-size: 1.3em; font-weight:bold;color:#cc1606;">${msg.eventVSCancelledLbl}</div>
                </div>
                <div hidden="{{isActive}}" style="margin:0px 30px 0px 30px; color: #888;"><b>${msg.dateLbl}: </b>
                    {{getDate(eventvs.dateBegin)}}</div>
            </div>
            <div>
                <div class="eventContentDiv"></div>

                <div class="horizontal layout center center-justified">
                    <div id="eventAuthorDiv" class="flex" style="margin:0px 20px 0 20px; color:#888; font-size: 0.85em;">
                        <b>${msg.byLbl}:</b> <span>{{eventvs.userVS}}</span>
                    </div>
                    <div style="font-size: 1.1em;">
                        <a href="{{eventvs.url}}" target="_blank">${msg.accessControlLbl}</a>
                    </div>
                </div>

                <div>
                    <div on-click="update" style="font-size: 1.4em; font-weight: bold; text-decoration: underline; color:#888;
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
    </template>
    <script>
        Polymer({
            is:'eventvs-election',
            properties: {
                url:{type:String, observer:'getHTTP'},
                eventvs:{type:Object, observer:'eventvsChanged'}
            },
            ready: function() {
                console.log(this.tagName + "- ready")
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
            getOptionClass: function(item) {
                return item
            },
            eventvsChanged:function() {
                console.log("eventvsChanged - eventvs: " + this.eventvs.state)
                this.optionVSSelected = null
                this.dateFinish = new Date(this.eventvs.dateFinish)
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
                d3.xhr(contextURL + "/rest/eventVSElection/id/" + this.eventvs.id + "/stats")
                        .header("Content-Type", "application/json").get(function(err, rawData){
                            this.fieldsEventVS = toJSON(rawData.response).fieldsEventVS
                            this.async(function (targetURL) { d3.select(this).selectAll(".numVotesClass").style("display", "block")}.bind(this))
                        }.bind(this)
                );
                d3.xhr(contextURL + "/rest/eventVSElection/id/" + this.eventvs.id + "/stats")
                        .header("Content-Type", "application/json").get(function(err, rawData){
                    if('TERMINATED' == this.eventvs.state) {
                        this.fieldsEventVS = toJSON(rawData.response).fieldsEventVS
                        d3.select(this).selectAll(".numVotesClass").style("display", "block")
                    }
                }.bind(this));
                d3.select(this).select(".eventContentDiv").html(this.decodeBase64(this.eventvs.content))
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