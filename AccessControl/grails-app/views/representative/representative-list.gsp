<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/representative/representative-info']"/>">

<polymer-element name="representative-list" attributes="url">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style no-shim>
        .card {
            position: relative;
            display: inline-block;
            width: 300px;
            vertical-align: top;
            background-color: #fff;
            box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            margin: 10px;
            padding: 5px 10px;
        }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{representativeData}}" handleAs="json"
                   contentType="json" on-core-complete="{{ajaxComplete}}"></core-ajax>
        <core-signals on-core-signal-representative-closed="{{closeRepresentativeDetails}}"></core-signals>
        <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
                             transitions="cross-fade-all" style="display:{{loading?'none':'block'}}">
            <section id="page1">
                <div cross-fade>
                    <div layout flex horizontal wrap around-justified>
                        <template repeat="{{representative in representativeData.representatives}}">
                            <div on-tap="{{showRepresentativeDetails}}" class='card representativeDiv'>
                                <div class='representativeImgCol'>
                                    <img src='{{representative.imageURL}}'></img>
                                </div>
                                <div class='representativeDataCol'>
                                    <p class='representativeName'>{{representative.name}}</p>
                                    <div class='numDelegationsData'>
                                        {{representative.numRepresentations}} <g:message code='numDelegationsPartMsg'/>
                                    </div>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
            </section>

            <section id="page2">
                <div cross-fade>
                    <representative-info id="representativeDetails" page="{{subpage}}" subpage vertical layout></representative-info>
                </div>
            </section>
        </core-animated-pages>

    </template>
    <script>
        Polymer('representative-list', {
            ready :  function(e) {
                console.log(this.tagName + " - ready")
                this.loading = true
                this.page = 0;
                this.subpage = 0;
            },
            closeRepresentativeDetails:function(e, detail, sender) {
                console.log(this.tagName + " - closeRepresentativeDetails")
                this.page = 0;
            },
            showRepresentativeDetails :  function(e) {
                console.log(this.tagName + " - showRepresentativeDetails")
                this.$.representativeDetails.representative = e.target.templateInstance.model.representative;
                this.page = 1;
            },
            getRepresentativeName:function(groupvs) {
                return groupvs.representative.firstName + " " + groupvs.representative.lastName
            },
            getSubject:function(eventSubject) {
                return eventSubject.substring(0,50) + ((eventSubject.length > 50)? "...":"");
            },
            ajaxComplete:function() {
                this.loading = false
            }
        });
    </script>
</polymer-element>