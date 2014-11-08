<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-highcharts', file: 'highcharts-import.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="eventvs-election-stats" attributes="title eventVSId">
    <template>
        <style></style>
        <core-ajax id="ajax" auto url="{{url}}" handleAs="json" response="{{statsDataMap}}" method="get" contentType="json"
                   on-core-response="{{ajaxResponse}}"></core-ajax>
        <div id="container" style="padding: 10px; width: 200px; height: 200px;"></div>
    </template>
    <script>
        Polymer('eventvs-election-stats', {
            title:null,
            publish: {},
            options: {
                chart: {
                    plotBackgroundColor: null,
                    plotBorderWidth: 1,//null,
                    plotShadow: false
                },
                title: {
                    text: ''
                },
                tooltip: {
                    pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
                },
                plotOptions: {
                    pie: {
                        allowPointSelect: true,
                        cursor: 'pointer',
                        dataLabels: {
                            enabled: true,
                            format: '<b>{point.name}</b>: {point.percentage:.1f} %',
                            style: {
                                color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
                            }
                        }
                    }
                },
                series: [{
                    type: 'pie',
                    name: 'Num votes',
                    data: [ ]
                }]
            },
            ready: function() {
                this.options.chart.renderTo = this.$.container
            },
            ajaxResponse: function() {
                var seriesData = []
                var numTotalVotes = 0
                Array.prototype.forEach.call(this.statsDataMap.fieldsEventVS, function(fieldEvent) {
                    seriesData.push([fieldEvent.content, fieldEvent.numVotesVS])
                    numTotalVotes += fieldEvent.numVotesVS
                });
                this.options.series[0].data = seriesData
                if(numTotalVotes > 0) var chart = new Highcharts.Chart(this.options);
            },
            eventVSIdChanged: function() {
                var targetURL = "${createLink( controller:'eventVSElection', action:" ", absolute:true)}/" +
                        this.eventVSId + "/stats"
                console.log(this.tagName + "- targetURL: " + targetURL)
                this.$.ajax.url = targetURL
            }
        });

    </script>
</polymer-element>