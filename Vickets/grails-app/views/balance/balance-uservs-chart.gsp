<link rel="import" href="${resource(dir: '/bower_components/vs-highcharts', file: 'highcharts-import.html')}">

<polymer-element name="balance-uservs-chart" attributes="chart title yAxisTitle xAxisCategories">
    <template>
        <style></style>
        <div id="container" style="padding: 10px;"></div>
    </template>
    <script>
        Polymer('balance-uservs-chart', {
            title:null,
            chart:null,
            yAxisTitle:null,
            publish: {
                xAxisCategories: {value: []},
                series:{}
            },
            options: {
                chart: {
                    margin: [0, 0, 0, 0],
                    spacingBottom: 30,
                    spacingTop: 30,
                    spacingLeft: 50,
                    spacingRight: 30
                },
                title: {},
                xAxis: { categories: [] },
                yAxis: {
                    title: {
                        text: ''
                    }
                },
                tooltip: {
                    formatter: function () {
                        return '<div style="display: inline-block;"><b>' + this.x + '</b><br/>' +
                                this.series.name + ': ' + this.y + '<br/>' +
                                'Total: ' + this.point.stackTotal + "</div>";
                    }
                },
                legend:{ align: 'center', itemDistance:100, floating: false },
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
                this.options.title.text = this.title || 'balance-uservs-chart'
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
