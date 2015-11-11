<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>

<dom-module name="groupvs-list">
    <style is="custom-style">
        .groupvsDiv {
            position: relative;
            display: inline-block;
            width: 300px;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            -moz-border-radius: 3px; border-radius: 4px;
            margin: 10px;
            color: #667;
            bottom: 0px;
            left: 0px;
            right: 0px;
            padding: 3px 7px 3px 7px;
            font-size: 0.9em;
        }

        .groupvsDiv:hover {
            cursor: pointer;
        }

        .groupvsSubjectDiv {
            white-space:nowrap;
            overflow:hidden;
            text-overflow: ellipsis;
            font-weight:bold;
            color: #f2f2f2;
            text-align: center;
            position: relative;
            padding: 5px 10px 5px 10px;
            margin: 0 auto 0 auto;
            text-decoration: underline;
        }

        .numTotalUsersDiv {
            position:absolute;
            bottom: 0px;
            padding: 3px 0 0 10px;
            margin: 0px 20px 0px 0px;
            text-transform:uppercase;
            font-size: 0.8em;
        }

        .groupvsActive {  border: 1px solid #388746; }

        .groupvsActive .groupvsSubjectDiv { color:#388746; }

        .groupvsActive .numTotalUsersDiv { color:#388746; }

        .groupvsPending {  border: 1px solid #fba131; }

        .groupvsPending .groupvsSubjectDiv{ color:#fba131; }

        .groupvsPending .numTotalUsersDiv { color:#fba131; }

        .groupvsFinished { border: 1px solid #cc1606; }

        .groupvsFinished .groupvsSubjectDiv{ color:#cc1606; }

        .groupvsFinished .numTotalUsersDiv { color:#cc1606; }
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
                    <div on-tap="showGroupDetails" class$="{{groupvsClass(groupvs.state)}}" style="height: 65px;">
                        <div class='groupvsSubjectDiv'>{{groupvs.name}}</div>
                        <div hidden="{{!isItemCanceled(groupvs)}}" style="position: relative;">
                            <div class='groupvsMessageCancelled'>${msg.groupvsCancelledLbl}</div>
                        </div>
                        <div class='numTotalUsersDiv text-right'><span>{{groupvs.numActiveUsers}}</span> ${msg.usersLbl}</div>
                        <div>
                            <div style="font-size: 0.7em; color: #888; font-style: italic; margin: 0 0 0 10px;">{{getRepresentativeName(groupvs)}}</div>
                            <div class="flex"></div>
                            <template is="dom-repeat" items="{{groupvs.tags}}" as="tag">
                                <a class="btn btn-default" style="font-size: 0.6em;margin:0px 5px 0px 0px;padding:3px;">
                                    <i class="fa fa-tag" style="color:#888; margin: 0 5px 0 0;"></i><span>{{tag.name}}</span></a>
                            </template>
                        </div>
                    </div>
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
                groupListDto:{type:Object,observer:'groupListDtoChanged'},
                url:{type:String}
            },
            groupListDtoChanged:function() {
                console.log(this.tagName + " - groupListDtoChanged ")
                this.groupList = this.groupListDto.resultList
            },
            ready :  function(e) {
                this.state = getURLParam("state")
                console.log(this.tagName + " - ready - state: " + this.state + " - groupListDto: " + this.groupListDto)
                if(this.state) {
                    this.$.groupvsTypeSelect.value = this.state
                    this.url = contextURL + "/rest/groupVS?state=" + this.state
                } else this.url = contextURL + "/rest/groupVS"
            },
            isTaggedGroup:function(groupvs) {
                return (groupvs.tags && groupvs.tags.length > 0)
            },
            isItemCanceled:function(item) {
                return item.state === 'CANCELED'
            },
            pagerChange:function(e) {
                var optionSelected = this.$.groupvsTypeSelect.value
                targetURL = contextURL + "/rest/groupVS?menu=" + menuType + "&state=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
                console.log(this.tagName + " - pagerChange - targetURL: " + targetURL)
                history.pushState(null, null, targetURL);
                this.$.ajax.url = targetURL
            },
            showGroupDetails :  function(e, details) {
                console.log(this.tagName + " - showGroupDetails")
                app.groupvs = e.model.groupvs;
                page(contextURL + "/rest/groupVS/id/" + app.groupvs.id)
            },
            getRepresentativeName:function(groupvs) {
                return groupvs.representative.firstName + " " + groupvs.representative.lastName
            },
            groupvsClass:function(state) {
                switch (state) {
                    case 'ACTIVE': return "groupvsDiv groupvs groupvsActive"
                    case 'PENDING': return "groupvsDiv groupvs groupvsPending"
                    case 'CANCELED': return "groupvsDiv groupvs groupvsFinished"
                }
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