<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="./group-card.vsp" rel="import"/>

<dom-module name="group-list">
    <style is="custom-style">
    </style>
    <template>
        <div id="groupListPage">
            <div id="groupStateSelector" class="layout horizontal center center-justified" style="margin:0 0 10px 0;">
                <select id="groupTypeSelect" style="font-size: 1.1em; height: 30px; max-width: 400px; margin:0 35px 0 35px;"
                        on-change="groupTypeSelect" class="form-control">
                    <option value="ACTIVE"  style="color:#388746;"> ${msg.selectActiveGroupLbl} </option>
                    <option value="PENDING" style="color:#fba131;"> ${msg.selectPendingGroupLbl} </option>
                    <option value="CANCELED" style="color:#cc1606;"> ${msg.selectClosedGroupLbl} </option>
                </select>
            </div>
            <div hidden="{{searchMsgHidden}}" class="horizontal layout center center-justified" style="margin: 0 auto">${msg.searchResultLbl}</div>
            <div class="layout flex horizontal center wrap around-justified">
                <template is="dom-repeat" items="{{groupListDto.resultList}}" as="group">
                    <group-card group="[[group]]"></group-card>
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
            is:'group-list',
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
                    this.$.groupTypeSelect.value = this.state
                    this.url = vs.contextURL + "/rest/group?state=" + this.state
                } else this.url = vs.contextURL + "/rest/group"
                document.querySelector('#voting_system_page').addEventListener('search-request', function (e) {
                    this.url = vs.contextURL + "/rest/group?menu=" + menuType + "&searchText=" + e.detail.query
                    this.searchMsgHidden = false
                }.bind(this))
            },
            pagerChange:function(e) {
                var optionSelected = this.$.groupTypeSelect.value
                this.url = vs.contextURL + "/rest/group?menu=" + menuType + "&state=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
            },
            groupTypeSelect: function() {
                var optionSelected = this.$.groupTypeSelect.value
                if("" != optionSelected) {
                    targetURL = vs.contextURL + "/rest/group?menu=" + menuType + "&state=" + optionSelected
                    var newURL = setURLParameter(window.location.href, "state",  optionSelected)
                    history.pushState(null, null, newURL);
                    console.log(this.tagName + " - groupTypeSelect: " + targetURL)
                    this.url = targetURL
                }
            },
            processSearch:function (textToSearch) {
                vs.updateSearchMessage("${msg.searchResultLbl} '" + textToSearch + "'")
                this.url = vs.contextURL + "/rest/search/group?searchText=" + textToSearch
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