<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="./groupvs-card.vsp" rel="import"/>

<dom-module name="groupvs-list">
    <style is="custom-style">
    </style>
    <template>
        <div id="groupListPage">
            <div id="groupStateSelector" class="layout horizontal center center-justified" style="margin:0 0 10px 0;">
                <select id="groupvsTypeSelect" style="font-size: 1.1em; height: 30px; max-width: 400px; margin:0 35px 0 35px;"
                        on-change="groupvsTypeSelect" class="form-control">
                    <option value="ACTIVE"  style="color:#388746;"> ${msg.selectActiveGroupvsLbl} </option>
                    <option value="PENDING" style="color:#fba131;"> ${msg.selectPendingGroupvsLbl} </option>
                    <option value="CANCELED" style="color:#cc1606;"> ${msg.selectClosedGroupvsLbl} </option>
                </select>
            </div>
            <div hidden="{{searchMsgHidden}}" class="horizontal layout center center-justified" style="margin: 0 auto">${msg.searchResultLbl}</div>
            <div class="layout flex horizontal center wrap around-justified">
                <template is="dom-repeat" items="{{groupListDto.resultList}}" as="groupvs">
                    <groupvs-card groupvs="[[groupvs]]"></groupvs-card>
                </template>
            </div>
            <vs-pager on-pager-change="pagerChange" max="{{groupListDto.max}}"
                      next="${msg.nextLbl}" previous="${msg.previousLbl}"
                      first="${msg.firstLbl}" last="${msg.lastLbl}"
                      offset="{{groupListDto.offset}}" total="{{groupListDto.totalCount}}"></vs-pager>
        </div>
    </template>
    <script>
        Polymer({
            is:'groupvs-list',
            properties: {
                groupListDto:{type:Object, observer:'groupListDtoChanged'},
                url:{type:String, observer:'getHTTP'}
            },
            groupListDtoChanged:function() {
                console.log(this.tagName + " - groupListDtoChanged ")
            },
            ready :  function(e) {
                this.searchMsgHidden = true
                this.state = getURLParam("state")
                console.log(this.tagName + " - ready - state: " + this.state + " - groupListDto: " + this.groupListDto)
                if(!("admin" == menuType || "superuser" == menuType)) this.$.groupStateSelector.style.display = 'none'
                if(this.state) {
                    this.$.groupvsTypeSelect.value = this.state
                    this.url = vs.contextURL + "/rest/groupVS?state=" + this.state
                } else this.url = vs.contextURL + "/rest/groupVS"
                document.querySelector('#voting_system_page').addEventListener('search-request', function (e) {
                    this.url = vs.contextURL + "/rest/groupVS?menu=" + menuType + "&searchText=" + e.detail.query
                    this.searchMsgHidden = false
                }.bind(this))
            },
            pagerChange:function(e) {
                var optionSelected = this.$.groupvsTypeSelect.value
                this.url = vs.contextURL + "/rest/groupVS?menu=" + menuType + "&state=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
            },
            groupvsTypeSelect: function() {
                var optionSelected = this.$.groupvsTypeSelect.value
                if("" != optionSelected) {
                    targetURL = vs.contextURL + "/rest/groupVS?menu=" + menuType + "&state=" + optionSelected
                    var newURL = setURLParameter(window.location.href, "state",  optionSelected)
                    history.pushState(null, null, newURL);
                    console.log(this.tagName + " - groupvsTypeSelect: " + targetURL)
                    this.url = targetURL
                }
            },
            processSearch:function (textToSearch) {
                vs.updateSearchMessage("${msg.searchResultLbl} '" + textToSearch + "'")
                this.url = vs.contextURL + "/rest/search/groupVS?searchText=" + textToSearch
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                //history.pushState(null, null, targetURL);
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.groupListDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>