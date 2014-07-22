<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-details']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">


<polymer-element name="groupvs-list" attributes="url">
    <template>
        <style no-shim>
        .card {
            position: relative;
            display: inline-block;
            width: 300px;
            vertical-align: top;
            background-color: #fff;
            box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            margin: 10px;
        }
        </style>
        <asset:stylesheet src="vickets_groupvs.css"/>
        <core-ajax id="ajax" auto url="{{url}}" response="{{groupvsData}}" handleAs="json" method="get"
                   contentType="json" on-core-complete="{{ajaxComplete}}"></core-ajax>
        <core-signals on-core-signal-uservs-details-closed="{{closeUserDetails}}"
                      on-core-signal-groupvs-details-closed="{{closeGroupDetails}}"></core-signals>
        <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
                             transitions="cross-fade-all" style="display:{{loading?'none':'block'}}">
            <section id="page1">
                <div cross-fade>
                    <div layout horizontal center center-justified>
                        <select id="groupvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                                class="form-control" on-change="{{groupvsTypeSelect}}">
                            <option value="ACTIVE"  style="color:#59b;"> - <g:message code="selectActiveGroupvsLbl"/> - </option>
                            <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingGroupvsLbl"/> - </option>
                            <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="selectClosedGroupvsLbl"/> - </option>
                        </select>
                    </div>
                    <div layout flex horizontal wrap around-justified>
                        <template repeat="{{groupvs in groupvsData.groupvsList}}">
                            <div on-tap="{{showGroupDetails}}" class='card groupvsDiv item {{ groupvs.state | groupvsClass }}' cross-fade>
                                <div class='groupvsSubjectDiv'>{{groupvs.name}}</div>
                                <div class='numTotalUsersDiv text-right'>{{groupvs.numActiveUsers}} <g:message code="usersLbl"/></div>
                                <div class='groupvsInfoDiv'><votingsystem-html-echo html="{{groupvs.description}}"></votingsystem-html-echo></div>
                                <div style="position: relative;display: {{(groupvs.state == 'CANCELLED')?'block':'none'}};">
                                    <div class='groupvsMessageCancelled' style=""><g:message code="groupvsCancelledLbl"/></div>
                                </div>
                                <div class='groupvsRepresentativeDiv text-right'>{{groupvs | getRepresentativeName}}</div>
                            </div>
                        </template>
                    </div>
                </div>
            </section>

            <section id="page2">
                <div cross-fade>
                    <groupvs-details id="groupDetails" page="{{subpage}}" subpage vertical layout groupvs="{{groupvs}}"></groupvs-details>
                </div>
            </section>
        </core-animated-pages>

    </template>
    <script>
        Polymer('groupvs-list', {
            ready :  function(e) {
                console.log(this.tagName + " - ready")
                this.loading = true
                this.groupvsData = {}
                this.page = 0;
                this.subpage = 0;
            },
            closeUserDetails:function(e, detail, sender) {
                this.subpage = 0;
                this.page = 1;
            },
            closeGroupDetails:function(e, detail, sender) {
                console.log(this.tagName + " - closeGroupDetails")
                this.page = 0;
            },
            showGroupDetails :  function(e) {
                console.log(this.tagName + " - showGroupDetails")
                this.$.groupDetails.groupvs = e.target.templateInstance.model.groupvs;
                this.page = 1;
            },
            getRepresentativeName:function(groupvs) {
                return groupvs.representative.firstName + " " + groupvs.representative.lastName
            },
            groupvsClass:function(state) {
                switch (state) {
                    case 'ACTIVE': return "groupvsActive"
                    case 'PENDING': return "groupvsPending"
                    case 'CANCELLED': return "groupvsFinished"
                }
            },
            groupvsTypeSelect: function() {
                var optionSelected = this.$.groupvsTypeSelect.value
                console.log("groupvsTypeSelect: " + optionSelected + " - menuType: " + menuType)
                if("" != optionSelected) {
                    targetURL = "${createLink(controller: 'groupVS')}?menu=" + menuType + "&state=" + optionSelected
                    history.pushState(null, null, targetURL);
                    this.$.ajax.url = targetURL
                }
            },
            ajaxComplete:function() {
                this.loading = false
            }
        });
    </script>
</polymer-element>