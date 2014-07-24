<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/user-balance.gsp']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/balance-details.gsp']"/>">

<link rel="import" href="${resource(dir: '/bower_components/google-chart', file: 'google-chart.html')}">

<link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">

<polymer-element name="week-reports" attributes="url params-data">
    <template>
        <core-ajax id="ajax" auto url="{{url}}" response="{{weekReport}}" handleAs="json" contentType="json"
                   on-core-complete="{{ajaxComplete}}" on-core-response="{{coreResponse}}"/>
        <!--<google-chart id="chart"></google-chart>-->
        <core-signals on-core-signal-user-balance-show-details="{{showBalanceDetails}}"
                      on-core-signal-balance-details-closed="{{closeBalanceDetails}}"></core-signals>

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

            <section>
                <balance-details id="balanceDetails"></balance-details>
            </section>


    </template>

    <script>
        Polymer('week-reports', {
            ready: function() {
                console.log(this.tagName + " - ready - url: " + this.url)
                this.$.ajax.url = this.url
                /*this.$.chart.cols = [{"label": "Data", "type": "string"},{"label": "Value", "type": "number"}]
                this.$.chart.rows = [["Something", 1]]*/
            },
            weekReportChanged: function() {
                console.log(this.tagName + " - weekReportChanged")

                this.weekReport.userBalances.userBalanceList.forEach(function(entry) {
                    //console.log(" entry: " + JSON.stringify(entry))
                })
                // `async` lets the main loop resume and perform tasks, like DOM updates, then it calls your callback
                this.async(function() { this.calculateStatistics()});
            },
            ajaxComplete:function(e) {
                console.log(this.tagName + " - ajaxComplete - " + e.detail.xhr.status + " - id: " + e.target.id)

            },
            coreResponse:function(xhr) {
                console.log(this.tagName + " - coreResponse")
            },
            calculateStatistics:function() {
                var userBalances = this.shadowRoot.querySelectorAll("user-balance._uservs")
                var groupBalances = this.shadowRoot.querySelectorAll("user-balance._group")
                var vicketSourceBalances = this.shadowRoot.querySelectorAll("user-balance._vicketSource")

            },
            showBalanceDetails:function(e, detail, sender) {
                this.$.balanceDetails.balance = detail
                this.$.balanceDetails.tapHandler()

            },
            closeBalanceDetails:function(e, detail, sender) {
                this.page = 0;
            }

        });
    </script>
</polymer-element>