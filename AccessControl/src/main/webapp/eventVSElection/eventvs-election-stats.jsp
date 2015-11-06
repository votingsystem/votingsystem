<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/google-chart/google-chart.html" rel="import">

<dom-module name="eventvs-election-stats">
    <template>
        <style></style>
        <iron-ajax auto id="ajax" url="{{url}}" handle-as="json" last-response="{{statsDto}}" method="get" content-type="application/json"></iron-ajax>

        <div hidden="{{chartVisible}}" id="messageToUser" vertical layout center center-justified
             style="padding: 10px;font-weight: bold;">
            ${msg.withoutVotesLbl}
        </div>
        <div hidden="{{!chartVisible}}" id="chartDiv" vertical layout center center-justified
             style="margin:0 0 0 10px;">
            <google-chart id="chart" type='pie'
                          cols='[{"label": "option", "type": "string"},{"label": "votes", "type": "number"}]'>
            </google-chart>
        </div>
    </template>
    <script>
        Polymer({
            is:'eventvs-election-stats',
            properties: {
                eventvsId:{type:Number, observer:'loadStats'},
                chartVisible:{type:Boolean, value:false},
                statsDto:{type:Object, observer:'statsDtoChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            loadStats: function() {
                var targetURL = contextURL + "/rest/eventVSElection/id/" + this.eventvsId + "/stats"
                console.log(this.tagName + " - loadStats - targetURL: " + targetURL)
                this.$.ajax.url = targetURL
            },
            statsDtoChanged: function() {
                var rows = []
                var numTotalVotes = 0
                Array.prototype.forEach.call(this.statsDto.fieldsEventVS, function(fieldEvent) {
                    rows.push([fieldEvent.content, fieldEvent.numVotesVS])
                    numTotalVotes += fieldEvent.numVotesVS
                });
                console.log(this.tagName + " - subject: " + this.statsDto.subject + " - numTotalVotes: " + numTotalVotes);
                if(numTotalVotes > 0) {
                    this.chartVisible = true
                    this.$.chart.rows = rows
                } else this.chartVisible = false
            }
        });
    </script>
</dom-module>