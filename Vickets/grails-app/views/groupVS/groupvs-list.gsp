<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/groupVS/groupvs-details']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-html-echo', file: 'vs-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-pager', file: 'vs-pager.html')}">

<polymer-element name="groupvs-list" attributes="url state">
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
        <g:include view="/include/styles.gsp"/>
        <asset:stylesheet src="vickets_groupvs.css"/>
        <core-ajax id="ajax" url="{{url}}" handleAs="json" contentType="json" on-core-complete="{{ajaxComplete}}"></core-ajax>
        <core-signals on-core-signal-groupvs-details-closed="{{closeGroupDetails}}"></core-signals>
        <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
                             transitions="cross-fade-all" style="display:{{loading?'none':'block'}}">
            <section id="page1">
                <div class="pageContentDiv" cross-fade>
                    <div layout horizontal center center-justified>
                        <select id="groupvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                                on-change="{{groupvsTypeSelect}}" class="form-control">
                            <option value="ACTIVE"  style="color:#59b;"> - <g:message code="selectActiveGroupvsLbl"/> - </option>
                            <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingGroupvsLbl"/> - </option>
                            <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="selectClosedGroupvsLbl"/> - </option>
                        </select>
                    </div>
                    <vs-pager on-pager-change="{{pagerChange}}" max="{{groupVSListMap.max}}"
                              next="<g:message code="nextLbl"/>" previous="<g:message code="previousLbl"/>"
                              first="<g:message code="firstLbl"/>" last="<g:message code="lastLbl"/>"
                              offset="{{groupVSListMap.offset}}" total="{{groupVSListMap.totalCount}}"></vs-pager>

                    <div layout flex horizontal wrap around-justified>
                        <template repeat="{{groupvs in groupvsList}}">
                            <div on-tap="{{showGroupDetails}}" class='card groupvsDiv item {{ groupvs.state | groupvsClass }}' cross-fade>
                                <div class='groupvsSubjectDiv'>{{groupvs.name}}</div>
                                <div class='numTotalUsersDiv text-right'>{{groupvs.numActiveUsers}} <g:message code="usersLbl"/></div>
                                <div class='groupvsInfoDiv'><vs-html-echo html="{{groupvs.description}}"></vs-html-echo></div>
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
                <div class="pageContentDiv" cross-fade>
                    <groupvs-details id="groupDetails" page="{{subpage}}" subpage vertical layout groupvs="{{groupvs}}"></groupvs-details>
                </div>
            </section>
        </core-animated-pages>

    </template>
    <script>
        Polymer('groupvs-list', {
            publish: {
                groupVSListMap: {value: {}}
            },
            groupVSListMapChanged:function() {
                this.groupvsList = this.groupVSListMap.groupvsList
                this.loading = false
            },
            ready :  function(e) {
                console.log(this.tagName + " - ready - state: " + this.state)
                if(this.state) this.$.groupvsTypeSelect.value = this.state
                this.loading = true
                this.page = 0;
                this.subpage = 0;
            },
            pagerChange:function(e) {
                var optionSelected = this.$.groupvsTypeSelect.value
                console.log("groupvsTypeSelect: " + optionSelected)
                targetURL = "${createLink(controller: 'groupVS')}?menu=" + menuType + "&state=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
                console.log(this.tagName + " - pagerChange - targetURL: " + targetURL)
                history.pushState(null, null, targetURL);
                this.$.ajax.url = targetURL
                this.$.ajax.go()
            },
            closeGroupDetails:function(e, detail, sender) {
                console.log(this.tagName + " - closeGroupDetails")
                this.page = 0;
            },
            showGroupDetails :  function(e) {
                console.log(this.tagName + " - showGroupDetails")
                this.$.groupDetails.groupvs = {userVS:e.target.templateInstance.model.groupvs} ;
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
                this.groupVSListMap = this.$.ajax.response
                this.loading = false
            }
        });
    </script>
</polymer-element>