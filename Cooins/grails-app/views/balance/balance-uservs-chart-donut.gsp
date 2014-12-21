<link rel="import" href="${resource(dir: '/bower_components/vs-highcharts', file: 'highcharts-import.html')}">

<polymer-element name="balance-uservs-chart-donut" attributes="chart title yAxisTitle xAxisCategories">
    <template>
        <style></style>
        <div horizontal layout center center-justified>
            <div id="container" style="padding: 10px;"></div>
        </div>
    </template>
    <script>
        Polymer('balance-uservs-chart-donut', {
            title:null,
            chart:null,
            yAxisTitle:null,
            publish: {
                xAxisCategories: {value: []},
                series:{}
            },
            options: {
                chart: { type: 'pie' },
                title: { text: '' },
                yAxis: { title: { text: '' } },
                plotOptions: {
                    pie: {
                        shadow: false,
                        center: ['50%', '50%']
                    }
                },
                tooltip: {
                    valueSuffix: '%'
                }
            },
            ready: function() {
                this.options.chart.type = this.chart = this.chart || 'bar'
                this.options.chart.renderTo = this.$.container
                this.options.title.text = this.title
                this.options.xAxis.categories = this.xAxisCategories
                this.options.yAxis.title.text = this.yAxisTitle

            },
            setSeries: function(tagData) {
                this.options.series = [
                    {
                        name: 'Versions',
                        data: tagData[2],
                        size: '90%',
                        innerSize: '80%',
                        dataLabels: {
                            formatter: function () {
                                // display only if larger than 1
                                return this.y > 1 ? '<b>' + this.point.name + ':</b> ' + this.y + '%'  : null;
                            }
                        }
                    }, {
                        name: 'gastos',
                        data: tagData[1],
                        size: '80%',
                        innerSize: '60%',
                        dataLabels: {
                            formatter: function () {
                                return this.y > 5 ? this.point.name : null;
                            },
                            color: 'white',
                            distance: -50
                        }
                    }, {
                        name: 'Browsers',
                        data: tagData[0],
                        size: '60%',
                        dataLabels: {
                            formatter: function () {
                                return this.y > 5 ? this.point.name : null;
                            },
                            color: 'white',
                            distance: -80
                        }
                    }
                ]
                this.options.chart.renderTo = this.$.container
                this.options.title.text = this.title
                var chart = new Highcharts.Chart(this.options);
                console.log(this.tagName + " - ready")
            }
        });
    </script>
</polymer-element>
