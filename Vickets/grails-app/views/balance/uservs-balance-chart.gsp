<link rel="import" href="${resource(dir: '/bower_components/votingsystem-highcharts', file: 'highcharts-import.html')}">

<polymer-element name="uservs-balance-chart" attributes="chart title yAxisTitle xAxisCategories">
    <template>
        <style>

        </style>
        <div id="container"></div>
    </template>
    <script>
        Polymer('uservs-balance-chart', {
            title:null,
            chart:null,
            yAxisTitle:null,
            publish: {
                xAxisCategories: {value: []},
                series:{}
            },
            options: {
                chart: {},
                title: {},
                xAxis: { categories: [] },
                yAxis: {
                    title: {
                        text: ''
                    }
                },
                tooltip: {
                    formatter: function () {
                        return '<b>' + this.x + '</b><br/>' +
                                this.series.name + ': ' + this.y + '<br/>' +
                                'Total: ' + this.point.stackTotal;
                    }
                },
                plotOptions: {
                    column: {
                        stacking: 'normal',
                        dataLabels: {
                            enabled: true,
                            color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                            style: {
                                textShadow: '0 0 3px black, 0 0 3px black'
                            }
                        }
                    }
                }
            },
            ready: function() {
                this.options.chart.type = this.chart = this.chart || 'bar'
                this.options.chart.renderTo = this.$.container
                this.options.title.text = this.title || 'uservs-balance-chart'
                this.options.xAxis.categories = this.xAxisCategories
                this.options.yAxis.title.text = this.yAxisTitle

            },
            seriesChanged: function() {
                this.options.series = this.series
                var chart = new Highcharts.Chart(this.options);
            }
        });

    </script>
</polymer-element>
