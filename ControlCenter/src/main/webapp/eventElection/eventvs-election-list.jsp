<%@ page contentType="text/html; charset=UTF-8" %>

<link href="eventvs-election.vsp" rel="import"/>
<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>

<dom-module name="eventvs-election-list">
    <template>
        <style>
            .card { position: relative; display: inline-block; width: 300px; vertical-align: top;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24); margin: 10px;
            }
        </style>
        <div>
            <div class="layout horizontal center center-justified">
                <select id="eventVSStateSelect" style="margin:10px auto 0px auto;color:black; width: 300px;"
                        on-change="eventVSStateSelect" class="form-control">
                    <option value="ACTIVE" style="color:#388746;"> - ${msg.selectOpenPollsLbl} - </option>
                    <option value="PENDING" style="color:#fba131;"> - ${msg.selectPendingPollsLbl} - </option>
                    <option value="TERMINATED" style="color:#cc1606;"> - ${msg.selectClosedPollsLbl} - </option>
                </select>
            </div>
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{eventListDto.resultList}}">
                    <div on-tap="showEventVSDetails" class$='{{getEventVSClass(item.state)}}'>
                        <div  eventvs-id="{{item.id}}" class='eventSubjectDiv' style="text-align: center;">{{getSubject(item.subject)}}</div>
                        <div class="eventBodyDiv flex">
                            <div class='eventDateBeginDiv'>
                                <div class='eventDateBeginLblDiv'>${msg.dateLbl}:</div>
                                <div class='eventDateBeginValueDiv'>{{getDate(item.dateBegin)}}</div>
                            </div>
                            <div class='eventAuthorDiv'>
                                <div class='eventAuthorLblDiv'>${msg.byLbl}:</div>
                                <div class='eventAuthorValueDiv'>{{item.user}}</div>
                            </div>
                            <div hidden="{{!isCanceled(item)}}" class='cancelMessage'>${msg.eventCancelledLbl}</div>
                        </div>
                        <div class='eventDivFooter'>
                            <div class='eventRemainingDiv'>{{getElapsedTime(item.dateFinish)}}</div>
                            <div class='eventStateDiv'>{{getMessage(item.state)}}</div>
                        </div>
                    </div>
                </template>
            </div>
            <vs-pager id="vspager" on-pager-change="pagerChange" max="{{eventListDto.max}}" style="margin: 0 0 100px 0;"
                      next="${msg.nextLbl}" previous="${msg.previousLbl}"
                      first="${msg.firstLbl}" last="${msg.lastLbl}"
                      offset="{{eventListDto.offset}}" total="{{eventListDto.totalCount}}"></vs-pager>
        </div>

    </template>
    <script>
        Polymer({
            is:'eventvs-election-list',
            properties: {
                eventListDto:{type:Object, value:{}, observer:'eventListDtoChanged'},
                url:{type:String, observer:'getHTTP'},
                eventVSState:{type:String}
            },
            ready:function(e) {
                sendSignalVS({caption:"${msg.electionSystemLbl}"})
                this.eventVSState = getURLParam('eventVSState')
                console.log(this.tagName + " - ready - eventVSState: " + this.eventVSState)
                this.loading = true
                if(this.eventVSState) this.$.eventVSStateSelect.value = this.eventVSState
            },
            loadURL:function(path, querystring) {
                console.log(this.tagName + " - loadURL - path: " + path + " - querystring: " + querystring)
                if(querystring) {
                    this.url = vs.contextURL + "/rest/eventElection?" + querystring
                } else this.url = vs.contextURL + "/rest/eventElection"
                this.eventVSState = getURLParam("eventVSState", path)
                if(this.eventVSState === "") this.eventVSState = 'ACTIVE'
                this.$.eventVSStateSelect.value = this.eventVSState
            },
            isCanceled:function(eventvs) {
                eventvs.state === 'CANCELED'
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            eventListDtoChanged:function() {
                if(this.eventListDto == null) return
                console.log(this.tagName + " - eventListDtoChanged - offset: " + this.eventListDto.offset + " - totalCount: " + this.eventListDto.totalCount)
                this.loading = false
                this.$.vspager.style.display = 'block'
            },
            pagerChange:function(e) {
                var optionSelected = this.$.eventVSStateSelect.value
                console.log("eventVSStateSelect: " + optionSelected)
                this.$.vspager.style.display = 'none'
                targetURL = vs.contextURL + "/rest/eventElection?menu=" + menuType + "&eventVSState=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
                console.log(this.tagName + " - pagerChange - targetURL: " + targetURL)
                history.pushState(null, null, targetURL);
                this.url = targetURL
            },
            showEventVSDetails :  function(e) {
                console.log(this.tagName + " - showEventVSDetails")
                vs.eventvs = e.model.item;
                page("/rest/eventElection/id/" + vs.eventvs.id)
            },
            getRepresentativeName:function(group) {
                return group.representative.firstName + " " + group.representative.lastName
            },
            getSubject:function(eventSubject) {
                return eventSubject.substring(0,50) + ((eventSubject.length > 50)? "...":"");
            },
            getMessage : function (eventVSState) {
                switch (eventVSState) {
                    case EventVS.State.ACTIVE: return "${msg.openLbl}"
                    case EventVS.State.PENDING: return "${msg.pendingLbl}"
                    case EventVS.State.TERMINATED: return "${msg.closedLbl}"
                    case EventVS.State.CANCELED: return "${msg.cancelledLbl}"
                }
            },
            getElapsedTime: function(dateStamp) {
                return new Date(dateStamp).getElapsedTime() + " ${msg.toCloseLbl}"
            },
            getEventVSClass:function(eventVSState) {
                switch (eventVSState) {
                    case EventVS.State.ACTIVE: return "card eventDiv eventVSActive"
                    case EventVS.State.PENDING: return "card eventDiv eventVSPending"
                    case EventVS.State.TERMINATED: return "card eventDiv eventVSFinished"
                    case EventVS.State.CANCELED: return "card eventDiv eventVSFinished"
                }
            },
            eventVSStateSelect: function() {
                this.eventVSState = this.$.eventVSStateSelect.value
                console.log("eventVSStateSelect: " + this.eventVSState)
                targetURL = vs.contextURL + "/rest/eventElection?eventVSState=" + this.eventVSState
                var newURL = setURLParameter(window.location.href, "eventVSState",  this.eventVSState)
                history.pushState(null, null, newURL);
                this.url = targetURL
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.eventListDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>