<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../eventElection/eventvs-election.vsp" rel="import"/>
<link href="../element/search-info.vsp" rel="import"/>
<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="../resources/bower_components/vs-advanced-search-dialog/vs-advanced-search-dialog.html" rel="import"/>

<dom-module name="eventvs-election-list">
    <template>
        <style>
            .card { position: relative; display: inline-block; width: 300px; vertical-align: top;
                box-shadow: 1px 2px 1px 0px rgba(0, 0, 0, 0.24); margin: 10px;
            }
            .eventAuthorValueDiv {
                width:150px;
                overflow:hidden;
                display:inline;
                position: relative;
            }
            .eventDateBeginDiv {
                margin: 0px 7px 0px 0;
                text-align: center;
                color: #888;
                font-weight: bold;
                border-bottom: 1px dotted #888;
            }
            .eventBodyDiv {
                width: 100%;
                font-size: 0.8em;
                vertical-align:middle;
                padding: 0px 5px 0px 5px;
            }
            .eventBodyDiv .cancelMessage {
                transform:rotate(15deg);
                -ms-transform:rotate(15deg);
                -webkit-transform:rotate(15deg);
                -moz-transform:rotate(15deg);
                padding:3px 5px 3px 5px;
                margin: 0 0 0 20px;
                opacity:0.4;
                text-transform:uppercase;
                font-weight:bold;
                font-size: 1.9em;
            }
            .eventSubjectDiv {
                font-size: 1.1em;
                display: block;
                font-weight:bold;
                padding: 0px 5px 5px 5px;
            }
            .eventDiv {
                display:inline;
                width:280px;
                background-color: #f2f2f2;
                margin: 10px 5px 5px 10px;
                -moz-border-radius: 5px; border-radius: 3px;
                cursor: pointer; cursor: hand;
                overflow:hidden;
                float:left;
            }
            .eventDivFooter{
                position: relative;
                padding: 5px 5px 0px 5px;
            }
            .eventRemainingDiv {
                display:inline;
                font-weight:bold;
                font-size:0.7em;
                position: relative;
            }
            .eventAuthorDiv {
                position: relative;
                white-space:nowrap;
                text-overflow: ellipsis;
            }
            .eventStateDiv {
                font-size: 0.9em;
                display:inline;
                font-weight:bold;
                float:right;
                position: relative;
            }
            .eventVSActive { border: 1px solid #388746; }
            .eventVSActive .eventSubjectDiv{ color:#388746; }
            .eventVSActive .eventStateDiv{ color:#388746; }
            .eventVSPending { border: 1px solid #fba131; }
            .eventVSPending .eventSubjectDiv{ color:#fba131; }
            .eventVSPending .eventStateDiv{ color:#fba131; }
            .eventVSFinished .eventRemainingDiv{ display: none; }
            .eventVSFinished { border: 1px solid #cc1606; }
            .eventVSFinished .eventSubjectDiv{ color:#cc1606; }
            .eventVSFinished .eventStateDiv{ color:#cc1606; }
        </style>
        <vs-advanced-search-dialog id="advancedSearchDialog"></vs-advanced-search-dialog>
        <search-info id="searchInfo"></search-info>
        <div>
            <div class="layout horizontal center center-justified">
                <select id="eventStateSelect" style="margin:10px auto 0px auto;color:black; width: 300px;"
                        on-change="eventStateSelect" class="form-control" value="{{eventVSState}}">
                    <option value="ACTIVE" style="color:#388746;"> - ${msg.selectOpenPollsLbl} - </option>
                    <option value="PENDING" style="color:#fba131;"> - ${msg.selectPendingPollsLbl} - </option>
                    <option value="TERMINATED" style="color:#cc1606;"> - ${msg.selectClosedPollsLbl} - </option>
                </select>
            </div>
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{eventListDto.resultList}}">
                    <div on-tap="showEventVSDetails" class$='{{getEventVSClass(item.state)}}'>
                        <div class='eventDateBeginDiv'>{{getDate(item.dateBegin)}}</div>
                        <div  eventvs-id="{{item.id}}" class='eventSubjectDiv' style="text-align: center;">{{getSubject(item.subject)}}</div>
                        <div class="eventBodyDiv flex">

                            <div class='eventAuthorDiv'>
                                <div class='eventAuthorValueDiv'>{{item.user}}</div>
                            </div>
                            <div hidden="{{!isCanceled(item)}}" class='cancelMessage'>${msg.eventCancelledLbl}</div>
                        </div>
                        <div class='eventDivFooter'>
                            <div class='eventRemainingDiv'>{{getElapsedTime(item)}}</div>
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
                eventListDto:{type:Object, observer:'eventListDtoChanged'},
                url:{type:String, observer:'getHTTP'},
                eventVSState:{type:String, value:'ACTIVE'}
            },
            ready:function(e) {
                console.log(this.tagName + " - ready")
                this.loading = true
            },
            loadURL:function(path, querystring) {
                console.log(this.tagName + " - loadURL - path: " + path + " - querystring: " + querystring)
                if(querystring) {
                    this.url = vs.contextURL + "/rest/eventElection?" + querystring
                } else this.url = vs.contextURL + "/rest/eventElection"
                this.eventVSState = getURLParam("eventVSState", path)
                if(!this.eventVSState) this.eventVSState = 'ACTIVE'
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
                console.log("eventStateSelect: " + this.eventVSState)
                this.$.vspager.style.display = 'none'
                targetURL = vs.contextURL + "/rest/eventElection?eventVSState=" + this.eventVSState +
                        "&max=" + e.detail.max + "&offset=" + e.detail.offset
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
                    case EventVS.State.ACTIVE:
                    case EventVS.State.PENDING: return ""
                    case EventVS.State.TERMINATED: return "${msg.closedLbl}"
                    case EventVS.State.CANCELED: return "${msg.cancelledLbl}"
                }
            },
            getElapsedTime: function(election) {
                switch (election.state) {
                    case EventVS.State.ACTIVE: return new Date(election.dateFinish).getElapsedTime() + " ${msg.toCloseLbl}"
                    case EventVS.State.PENDING:
                        console.log(new Date(election.dateBegin), "elapsedTime: ", new Date(election.dateBegin).getElapsedTime())
                        return new Date(election.dateBegin).getElapsedTime() + " ${msg.toOpenLbl}"
                }
            },
            getEventVSClass:function(eventVSState) {
                switch (eventVSState) {
                    case EventVS.State.ACTIVE: return "card eventDiv eventVSActive"
                    case EventVS.State.PENDING: return "card eventDiv eventVSPending"
                    case EventVS.State.TERMINATED: return "card eventDiv eventVSFinished"
                    case EventVS.State.CANCELED: return "card eventDiv eventVSFinished"
                }
            },
            eventStateSelect: function(e) {
                this.eventVSState = e.target.value
                console.log("eventStateSelect - state: " + this.eventVSState)
                targetURL = vs.contextURL + "/rest/eventElection?eventVSState=" + this.eventVSState
                var newURL = setURLParameter(window.location.href, "eventVSState",  this.eventVSState)
                history.pushState(null, null, newURL);
                this.url = targetURL
            },
            processSearch:function (textToSearch, dateBeginFrom, dateBeginTo) {
                this.url = vs.contextURL + "/rest/search/eventVS?searchText=" +
                        textToSearch + "&amp;dateBeginFrom=" + dateBeginFrom + "&amp;dateBeginTo=" + dateBeginTo + "&amp;eventvsType=ELECTION"
            },
            processSearchJSON: function (dataJSON) {
                this.url = vs.contextURL + "/rest/search/eventVS";
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                new XMLHttpRequest().header("Content-Type", "application/json").get(targetURL, function(responseText){
                    this.eventListDto = toJSON(responseText)
                }.bind(this));
            }
        });
    </script>
</dom-module>