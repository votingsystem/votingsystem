<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-details']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">

<polymer-element name="groupvs-list" attributes="url">
    <template>
        <style>
        .card {
            position: relative;
            display: inline-block;
            width: 300px;
            height: 200px;
            vertical-align: top;
            background-color: #fff;
            box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
            margin: 10px;
        }
        </style>
        <asset:stylesheet src="vickets_groupvs.css"/>
        <core-ajax id="ajax" auto url="{{url}}" response="{{groupvsData}}" handleAs="json" method="get"
                   contentType="json" on-core-complete="{{ajaxComplete}}"></core-ajax>

        <core-animated-pages id="pages" flex selected="0" on-core-animated-pages-transition-end="{{transitionend}}" transitions="cross-fade-all">
            <section>
                <div cross-fade>
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

            <section>
                <div cross-fade>

                    <paper-fab icon="{{$.pages.selected != 0 ? 'arrow-back' : ''}}"  on-tap="{{back}}"
                               style="color:#f9f9f9;visibility:{{$.pages.selected != 0 ? 'visible':'hidden'}}"></paper-fab>
                    <groupvs-details id="groupDetails" vertical layout groupvs="{{groupvs}}"></groupvs-details>
                </div>
            </section>
        </core-animated-pages>

    </template>
    <script>
        Polymer('groupvs-list', {
            ready :  function(e) {
                this.groupvsData = {}
            },
            showGroupDetails :  function(e) {
                this.$.groupDetails.groupvs = e.target.templateInstance.model.groupvs;
                this.$.pages.selected = 1;
            },
            back:function() {
                this.lastSelected = this.$.pages.selected;
                console.log("this.lastSelected: " + this.lastSelected);
                this.$.pages.selected = 0;
            },
            getRepresentativeName:function(groupvs) {
                return groupvs.representative.firstName + " " + groupvs.representative.lastName
            },
            groupvsClicked:function(groupvsURL) {
                window.location.href = groupvsURL
            },
            groupvsClass:function(state) {
                switch (state) {
                    case 'ACTIVE': return "groupvsActive"
                    case 'PENDING': return "groupvsPending"
                    case 'CANCELLED': return "groupvsFinished"
                }
            }
        });
    </script>
</polymer-element>