<%@ page contentType="text/html; charset=UTF-8" %>

<link rel="import" href="../resources/bower_components/vs-highcharts/highcharts-import.html">

<dom-module name="balance-uservs-chart-donut">
    <template>
        <div class="horizontal layout center center-justified">
            <div id="containerDonut" style="padding: 10px;"></div>
        </div>
    </template>
    <script>
        Polymer({
            is:'balance-uservs-chart-donut',
            properties: {
                xAxisCategories: {type:Array, value: []},
                series:{type:Object, value: {}},
                yAxisTitle:{type:String},
                caption:{type:String}
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
                this.options.title.text = this.caption
            },
            setSeries: function(tagData) {
                this.options.series = [
                    {
                        name: '${msg.expensesLbl}',
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
                        name: '${msg.incomesLbl}',
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
                        name: '${msg.tagsLbl}',
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
                this.options.title.text = this.caption
                var chart = new Highcharts.Chart(this.options);
                console.log(this.tagName + " - setSeries")
            }
        });
    </script>
</dom-module>
