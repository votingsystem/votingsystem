<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="./groupvs-card.vsp" rel="import"/>

<dom-module name="groupvs-list">
    <style is="custom-style">
    </style>
    <template>
        <iron-ajax auto id="ajax" url="{{url}}" handle-as="json" content-type="application/json" last-response="{{groupListDto}}"></iron-ajax>
        <div id="groupListPage">
            <div class="layout horizontal center center-justified" style="margin:0 0 10px 0;">
                <select id="groupvsTypeSelect" style="font-size: 1.1em; height: 30px; max-width: 400px;"
                        on-change="groupvsTypeSelect" class="form-control">
                    <option value="ACTIVE"  style="color:#388746;"> ${msg.selectActiveGroupvsLbl} </option>
                    <option value="PENDING" style="color:#fba131;"> ${msg.selectPendingGroupvsLbl} </option>
                    <option value="CANCELED" style="color:#cc1606;"> ${msg.selectClosedGroupvsLbl} </option>
                </select>
            </div>
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
                url:{type:String}
            },
            groupListDtoChanged:function() {
                console.log(this.tagName + " - groupListDtoChanged ")
            },
            ready :  function(e) {
                this.state = getURLParam("state")
                console.log(this.tagName + " - ready - state: " + this.state + " - groupListDto: " + this.groupListDto)
                if(this.state) {
                    this.$.groupvsTypeSelect.value = this.state
                    this.url = contextURL + "/rest/groupVS?state=" + this.state
                } else this.url = contextURL + "/rest/groupVS"
            },
            pagerChange:function(e) {
                var optionSelected = this.$.groupvsTypeSelect.value
                targetURL = contextURL + "/rest/groupVS?menu=" + menuType + "&state=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
                console.log(this.tagName + " - pagerChange - targetURL: " + targetURL)
                history.pushState(null, null, targetURL);
                this.$.ajax.url = targetURL
            },
            groupvsTypeSelect: function() {
                var optionSelected = this.$.groupvsTypeSelect.value
                if("" != optionSelected) {
                    targetURL = contextURL + "/rest/groupVS?menu=" + menuType + "&state=" + optionSelected
                    var newURL = setURLParameter(window.location.href, "state",  optionSelected)
                    history.pushState(null, null, newURL);
                    console.log(this.tagName + " - groupvsTypeSelect: " + targetURL)
                    this.url = targetURL
                }
            },
            processSearch:function (textToSearch) {
                app.updateSearchMessage("${msg.searchResultLbl} '" + textToSearch + "'")
                this.url = contextURL + "/rest/search/groupVS?searchText=" + textToSearch
            }
        });
    </script>
</dom-module>