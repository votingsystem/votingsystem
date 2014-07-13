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
        <core-icon-button icon="{{$.pages.selected != 0 ? 'arrow-back' : 'menu'}}" on-tap="{{back}}"></core-icon-button>

        <core-animated-pages id="pages" flex selected="0" on-core-animated-pages-transition-end="{{transitionend}}" transitions="cross-fade-all hero-transition">

            <div id="_groupvsList" class="" style="" flex horizontal wrap around-justified layout hero-p>

                <template repeat="{{groupvs, i in groupvsData.groupvsList}}">
                    <div id="{{groupvs.id}}" groupvs="{{groupvs}}" on-tap="{{selectView}}"
                         class='card groupvsDiv item {{ groupvs.state | groupvsClass }}' hero-p isHero="{{$.pages.selected === i + 1 || $.pages.selected === 0}}" cross-fade>
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

            <template repeat="{{groupvs, i in groupvsData.groupvsList}}">
                <groupvs-details vertical layout groupvs="{{groupvs}}" index="{{i}}" hero-p isHero="{{$.pages.selected === item + 1 || $.pages.selected === 0}}"></groupvs-details>
            </template>

        </core-animated-pages>

    </template>
    <script>
        Polymer('groupvs-list', {
            ready :  function(e) {
                this.groupvsData = {}
            },
            selectView :  function(e) {
                var i = e.target.templateInstance.model.i;
                this.$.pages.selected = i+1;
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