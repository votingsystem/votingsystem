<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/iron-media-query/iron-media-query.html" rel="import"/>
<link href="../resources/bower_components/paper-tabs/paper-tabs.html" rel="import"/>
<link href="eventvs-election-stats.vsp" rel="import"/>

<dom-module name="eventvs-election">
    <template>
        <style>
            paper-tabs, paper-toolbar {
                background-color: #ba0011;
                color: #fff;
                box-shadow: 0px 3px 6px rgba(0, 0, 0, 0.2);
            }
        </style>
        <iron-media-query query="max-width: 600px" query-matches="{{smallScreen}}"></iron-media-query>
        <iron-ajax auto url="{{url}}" last-response="{{eventvs}}" handle-as="json" content-type="application/json"></iron-ajax>
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
                <div class="eventContentDiv">
                    <vs-html-echo html="{{decodeBase64(eventvs.content)}}"></vs-html-echo>
                </div>

                <div class="horizontal layout center center-justified">
                    <div id="eventAuthorDiv" class="flex" style="margin:0px 20px 0 20px; color:#888; font-size: 0.85em;">
                        <b>${msg.byLbl}:</b> <span>{{eventvs.userVS}}</span>
                    </div>
                    <div style="font-size: 1.1em;">
                        <a href="{{eventvs.url}}" target="_blank">${msg.accessControlLbl}</a>
                    </div>
                </div>

                <div class="horizontal layout" hidden="{{!smallScreen}}" style="margin: 20px 0 0 0;">
                    <paper-tabs selected="{{selectedTab}}" style="width: 100%; margin: 0 0 10px 0;">
                        <paper-tab>${msg.pollFieldLegend}</paper-tab>
                        <paper-tab>${msg.resultsLbl}</paper-tab>
                    </paper-tabs>
                </div>

                <div class="horizontal layout">
                    <div hidden="{{optionsDivHidden}}" style="width: 100%; display: block;">
                        <div>
                            <div style="font-size: 1.4em; font-weight: bold; text-decoration: underline; color:#888;
                                        margin: 20px 0 10px 0;">${msg.pollFieldLegend}:</div>
                            <template is="dom-repeat" items="{{eventvs.fieldsEventVS}}">
                                <div class="voteOption" style="width: 90%;margin: 0px auto 15px auto;
                                            font-size: 1.3em; font-weight: bold;">
                                    - <span>{{item.content}}</span>
                                </div>
                            </template>
                        </div>
                    </div>
                    <div id="statsDiv" hidden="{{statsDivHidden}}" class="vertical layout center center-justified">
                        <eventvs-election-stats id="electionStats" eventvs-id="{{eventvs.id}}"></eventvs-election-stats>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'eventvs-election',
            properties: {
                eventvs:{type:Object, observer:'eventvsChanged'},
                smallScreen:{type:Boolean, value:false, observer:'smallScreenChanged'},
                selectedTab:{type:Number, value:0, observer:'selectedTabChanged'}
            },
            ready: function() {
                console.log(this.tagName + "- ready")
                this.statsDivHidden = false
            },
            fireSignal:function() {
                this.fire('iron-signal', {name: "vs-innerpage", data: {caption:"${msg.pollLbl}"}});
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
            selectedTabChanged:function() {
                console.log("selectedTabChanged - selectedTab: " + this.selectedTab)
                if(this.selectedTab === 0) {
                    this.optionsDivHidden = false
                    this.statsDivHidden = true
                } else {
                    this.optionsDivHidden = true
                    this.statsDivHidden = false
                }
                if(!this.smallScreen) {
                    this.optionsDivHidden = false
                    this.statsDivHidden = false
                }
            },
            smallScreenChanged:function() {
                console.log("smallScreenChanged - smallScreen: " + this.smallScreen)
                this.selectedTabChanged()
                if(this.smallScreen) {
                    this.eventStateRowClass = "vertical layout flex"
                } else this.eventStateRowClass = "horizontal layout flex"
            },
            eventvsChanged:function() {
                this.$.electionStats
                console.log("eventvsChanged - eventvs: " + this.eventvs.state)
                this.optionVSSelected = null
                this.dateFinish = new Date(this.eventvs.dateFinish)
                this.isActive = false
                this.isPending = false
                this.isTerminated = false
                this.isCanceled = false
                if('PENDING' == this.eventvs.state) {
                    this.isPending = true
                    this.$.statsDiv.style.display = 'none'
                } else if ('TERMINATED' == this.eventvs.state) {
                    this.isTerminated = true
                } else if ('CANCELED' == this.eventvs.state) {
                    this.isCanceled = true
                } else this.isActive = true
                this.adminMenuHidden = true
                if('admin' === menuType) {
                    if(this.eventvs.state === 'ACTIVE' || this.eventvs.state === 'PENDING') this.adminMenuHidden = false
                }
                this.$.electionStats.eventvsId = this.eventvs.id
            }
        });
    </script>
</dom-module>