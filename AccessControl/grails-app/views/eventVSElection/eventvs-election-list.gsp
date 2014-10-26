<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-html-echo', file: 'vs-html-echo.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election.gsp']"/>">
<link rel="import" href="${resource(dir: '/bower_components/vs-pager', file: 'vs-pager.html')}">

<polymer-element name="eventvs-election-list" attributes="url eventvstype">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style no-shim>
        .card {
            position: relative;
            display: inline-block;
            width: 300px;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            margin: 10px;
        }
        </style>
        <core-ajax id="ajax" url="{{url}}" response="{{eventsVSMap}}" handleAs="json"
                   contentType="json" on-core-complete="{{ajaxComplete}}"></core-ajax>
        <core-signals on-core-signal-eventvs-election-closed="{{closeEventVSDetails}}"></core-signals>
        <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
                             transitions="cross-fade-all" style="display:{{loading?'none':'block'}}">
            <section id="page1">
                <div cross-fade>
                    <div layout horizontal center center-justified>
                        <template if="{{(eventvstype == 'election')}}">
                            <select id="eventVSStateSelect" style="margin:10px auto 0px auto;color:black; width: 300px;"
                                    on-change="{{eventVSStateSelect}}" class="form-control">
                                <option value="ACTIVE" style="color:#388746;"> - <g:message code="selectOpenPollsLbl"/> - </option>
                                <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingPollsLbl"/> - </option>
                                <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedPollsLbl"/> - </option>
                            </select>
                        </template>
                        <template if="{{eventvstype == 'claim'}}">
                            <select id="eventsStateSelect" style="margin:0px auto 0px auto;color:black; width: 300px;" class="form-control">
                                <option value="ACTIVE" style="color:#388746;"> - <g:message code="selectOpenClaimsLbl"/> - </option>
                                <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingClaimsLbl"/> - </option>
                                <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedClaimsLbl"/> - </option>
                            </select>
                        </template>
                        <template if="{{eventvstype == 'manifest'}}">
                            <select id="eventsStateSelect" style="margin:0px auto 0px auto;color:black; width: 300px;" class="form-control">
                                <option value="ACTIVE" style="color:#388746;"> - <g:message code="selectOpenManifestsLbl"/> - </option>
                                <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingManifestsLbl"/> - </option>
                                <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedManifestsLbl"/> - </option>
                            </select>
                        </template>
                    </div>
                    <div layout flex horizontal wrap around-justified>
                        <template repeat="{{eventvs in eventsVSMap.eventVS}}">
                            <div on-tap="{{showEventVSDetails}}" class='card eventDiv linkvs {{ eventvs.state | getEventVSClass }}'>
                                <div class='eventSubjectDiv'>
                                    <p style='margin:0px 0px 0px 0px;text-align:center;'>{{eventvs.subject | getSubject}}</p></div>
                                <div class='eventBodyDiv'>
                                    <div style='vertical-align: middle;'>
                                        <div class='eventAuthorDiv'>
                                            <div class='eventAuthorLblDiv'><g:message code='publishedByLbl'/>:</div>
                                            <div class='eventAuthorValueDiv'>{{eventvs.userVS}}</div>
                                        </div>
                                        <div class='eventDateBeginDiv'>
                                            <div class='eventDateBeginLblDiv'><g:message code='dateBeginLbl'/>:</div>
                                            <div class='eventDateBeginValueDiv'>{{eventvs.dateFinishStr}}</div>
                                        </div>
                                        <div class='cancelMessage' style="display: {{(eventvs.state == 'CANCELLED')?'block':'none'}}">
                                            <g:message code='eventCancelledLbl'/></div>
                                    </div>
                                </div>
                                <div class='eventDivFooter'>
                                    <div class='eventRemainingDiv'>{{eventvs.dateFinish | getElapsedTime}}</div>
                                    <div class='eventStateDiv'>{{eventvs.state | getMessage}}</div>
                                </div>
                            </div>
                        </template>
                    </div>
                    <vs-pager on-pager-change="{{pagerChange}}" max="${params.max}" style="margin: 0 0 100px 0;"
                              next="<g:message code="nextLbl"/>" previous="<g:message code="previousLbl"/>"
                              first="<g:message code="firstLbl"/>" last="<g:message code="lastLbl"/>"
                              offset="{{eventsVSMap.offset}}" total="{{eventsVSMap.totalCount}}"></vs-pager>
                </div>
            </section>

            <section id="page2">
                <div cross-fade>
                    <eventvs-election id="eventvsDetails" page="{{subpage}}" subpage vertical layout eventvs="{{eventvs}}"></eventvs-election>
                </div>
            </section>
        </core-animated-pages>

    </template>
    <script>
        Polymer('eventvs-election-list', {
            publish: {
                eventsVSMap: {value: {}}
            },
            eventsVSMapChanged:function() {
                this.loading = false
            },
            ready :  function(e) {
                console.log(this.tagName + " - ready")
                this.loading = true
                this.groupvsData = {}
                this.page = 0;
                this.subpage = 0;
            },
            closeEventVSDetails:function(e, detail, sender) {
                console.log(this.tagName + " - closeEventVSDetails")
                this.page = 0;
            },
            pagerChange:function(e) {
                var optionSelected = this.$.eventVSStateSelect.value
                console.log("eventVSStateSelect: " + optionSelected)
                targetURL = "${createLink(controller: 'eventVSElection')}?menu=" + menuType + "&eventVSState=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
                console.log(this.tagName + " - pagerChange - targetURL: " + targetURL)
                history.pushState(null, null, targetURL);
                this.$.ajax.url = targetURL
                this.$.ajax.go()
            },
            showEventVSDetails :  function(e) {
                console.log(this.tagName + " - showEventVSDetails")
                this.$.eventvsDetails.eventvs = e.target.templateInstance.model.eventvs;
                this.page = 1;
            },
            getRepresentativeName:function(groupvs) {
                return groupvs.representative.firstName + " " + groupvs.representative.lastName
            },
            getSubject:function(eventSubject) {
                return eventSubject.substring(0,50) + ((eventSubject.length > 50)? "...":"");
            },
            getMessage : function (eventVSState) {
                switch (eventVSState) {
                    case EventVS.State.ACTIVE: return "<g:message code='openLbl'/>"
                    case EventVS.State.PENDING: return "<g:message code='pendingLbl'/>"
                    case EventVS.State.TERMINATED: return "<g:message code='closedLbl'/>"
                    case EventVS.State.CANCELLED: return "<g:message code='cancelledLbl'/>"
                }
            },
            getElapsedTime: function(dateStr) {
                return dateStr.getElapsedTime()
            },
            getEventVSClass:function(eventVSState) {
                switch (eventVSState) {
                    case EventVS.State.ACTIVE: return "eventVSActive"
                    case EventVS.State.PENDING: return "eventVSPending"
                    case EventVS.State.TERMINATED: return "eventVSFinished"
                    case EventVS.State.CANCELLED: return "eventVSFinished"
                }
            },
            eventVSStateSelect: function() {
                var optionSelected = this.$.eventVSStateSelect.value
                console.log("eventVSStateSelect: " + optionSelected)
                targetURL = "${createLink(controller: 'eventVSElection')}?menu=" + menuType + "&eventVSState=" + optionSelected
                history.pushState(null, null, targetURL);
                this.$.ajax.url = targetURL
                this.$.ajax.go()
            },
            ajaxComplete:function() {
                this.loading = false
            }
        });
    </script>
</polymer-element>