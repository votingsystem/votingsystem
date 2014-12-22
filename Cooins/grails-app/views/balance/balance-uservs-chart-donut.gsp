<link rel="import" href="${resource(dir: '/bower_components/vs-highcharts', file: 'highcharts-import.html')}">

<polymer-element name="balance-uservs-chart-donut" attributes="chart title yAxisTitle xAxisCategories">
    <template>
        <style></style>
        <div horizontal layout center center-justified>
            <div id="containerDonut" style="padding: 10px;"></div>
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
                chart: { type: 'pie'},
                title: { text: '' },
                yAxis: { title: { text: '' } },
                plotOptions: {
                    pie: {
                        shadow: false,
                        center: ['50%', '50%']
                    }
                },
                tooltip: {
                    valueSuffix: ' EUR'
                }
            },
            ready: function() {
                this.options.title.text = this.title
            },
            setSeries: function(tagData) {
                this.options.series = [
                    {
                        name: '<g:message code="expensesLbl"/>',
                        data: tagData[2],
                        size: '60%',
                        innerSize: '50%',
                        dataLabels: {
                            formatter: function () {
                                // display only if larger than 1
                                return this.y > 1 ? '<b>' + this.point.name + ':</b> ' + this.y : null;
                            }
                        }
                    }, {
                        name: '<g:message code="incomesLbl"/>',
                        data: tagData[1],
                        size: '50%',
                        innerSize: '40%',
                        dataLabels: {
                            formatter: function () {
                                return null;
                            },
                            color: 'white',
                            distance: -30
                        }
                    }, {
                        name: '<g:message code="tagsLbl"/>',
                        data: tagData[0],
                        size: '40%',
                        dataLabels: {
                            formatter: function () {
                                return this.y > 1 ? this.point.name : null;
                            },
                            color: '#888',
                            distance: -30
                        }
                    }
                ]
                this.options.chart.renderTo = this.$.containerDonut
                this.options.title.text = this.title
                var chart = new Highcharts.Chart(this.options);
                console.log(this.tagName + " - setSeries")
            }
        });
    </script>
</polymer-element>
