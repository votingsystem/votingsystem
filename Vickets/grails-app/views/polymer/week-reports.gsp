<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/user-balance.gsp']"/>">

<polymer-element name="week-reports" attributes="url params-data">
    <template>
        <core-ajax id="ajax" auto url="{{url}}" response="{{weekReport}}" handleAs="json" method="post" contentType="json" on-core-complete="{{ajaxComplete}}"/>
        <h3><g:message code="usersLbl"/></h3>
        <div layout flex horizontal wrap>
            <template repeat="{{user in weekReport.userBalances.userBalanceList}}">
                <user-balance class="_uservs" balance="{{user}}"></user-balance>
            </template>
        </div>


        <h3><g:message code="groupsLbl"/></h3>
        <div layout flex horizontal wrap>
            <template repeat="{{group in weekReport.userBalances.groupBalanceList}}">
                <user-balance class="_group" balance="{{group}}"></user-balance>
            </template>
        </div>

        <h3><g:message code="vicketSourcesLbl"/></h3>
        <div layout flex horizontal wrap>
            <template repeat="{{vicketSource in weekReport.userBalances.vicketSourceBalanceList}}">
                <user-balance class="_vicketSource" balance="{{vicketSource}}"></user-balance>
            </template>
        </div>
    </template>

    <script>
        Polymer('week-reports', {
            ready: function() {
                console.log(this.tagName + " - ready - url: " + this.url)
                this.$.ajax.url = this.url
            },
            weekReportChanged: function() {
                console.log(this.tagName + " - weekReportChanged")

                this.weekReport.userBalances.userBalanceList.forEach(function(entry) {
                    //console.log(" entry: " + JSON.stringify(entry))
                })
                var hostElement = this
                // `async` lets the main loop resume and perform tasks, like DOM updates, then it calls your callback
                this.async(function() { hostElement.calculateResults()});
            },
            ajaxComplete:function() {
                console.log(this.tagName + " - ajaxComplete")
            },
            calculateResults:function() {
                var userBalances = this.shadowRoot.querySelectorAll("user-balance._uservs")
                var vicketSourceBalances = this.shadowRoot.querySelectorAll("user-balance._vicketSource")

            }
        });
    </script>
</polymer-element>