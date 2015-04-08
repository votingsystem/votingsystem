<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>

    <link rel="import" href="${resourceURL}/vs-highcharts/highcharts-import.html">
</head>
<body>
<balance-uservs-chart></balance-uservs-chart>
<polymer-element name="balance-uservs-chart" attributes="chart title yAxisTitle xAxisCategories">
    <template>
        <style></style>
        <div id="container" style="width: 600px; height: 400px; margin: 0 auto"></div>
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
                    type: 'pie'
                },
                title: {
                    text: 'Browser market share, April, 2011'
                },
                yAxis: {
                    title: {
                        text: 'Total percent market share'
                    }
                },
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
                var colors = Highcharts.getOptions().colors,
                categories = ['MSIE', 'Firefox', 'Chrome', 'Safari', 'Opera'],
                data = [{
                    y: 55.11,
                    color: colors[0],
                    drilldown: {
                        name: 'MSIE versions',
                        categories: ['MSIE 6.0', 'MSIE 7.0', 'MSIE 8.0', 'MSIE 9.0'],
                        data: [10.85, 7.35, 33.06, 2.81],
                        color: colors[0]
                    }
                }, {
                    y: 21.63,
                    color: colors[1],
                    drilldown: {
                        name: 'Firefox versions',
                        categories: ['Firefox 2.0', 'Firefox 3.0', 'Firefox 3.5', 'Firefox 3.6', 'Firefox 4.0'],
                        data: [0.20, 0.83, 1.58, 13.12, 5.43],
                        color: colors[1]
                    }
                }, {
                    y: 11.94,
                    color: colors[2],
                    drilldown: {
                        name: 'Chrome versions',
                        categories: ['Chrome 5.0', 'Chrome 6.0', 'Chrome 7.0', 'Chrome 8.0', 'Chrome 9.0',
                            'Chrome 10.0', 'Chrome 11.0', 'Chrome 12.0'],
                        data: [0.12, 0.19, 0.12, 0.36, 0.32, 9.91, 0.50, 0.22],
                        color: colors[2]
                    }
                }, {
                    y: 7.15,
                    color: colors[3],
                    drilldown: {
                        name: 'Safari versions',
                        categories: ['Safari 5.0', 'Safari 4.0', 'Safari Win 5.0', 'Safari 4.1', 'Safari/Maxthon',
                            'Safari 3.1', 'Safari 4.1'],
                        data: [4.55, 1.42, 0.23, 0.21, 0.20, 0.19, 0.14],
                        color: colors[3]
                    }
                }, {
                    y: 2.14,
                    color: colors[4],
                    drilldown: {
                        name: 'Opera versions',
                        categories: ['Opera 9.x', 'Opera 10.x', 'Opera 11.x'],
                        data: [ 0.12, 0.37, 1.65],
                        color: colors[4]
                    }
                }],
                browserData = [],
                versionsData = [],
                i, j,
                dataLen = data.length,
                drillDataLen,
                brightness;
                for (i = 0; i < dataLen; i += 1) {
                    // add browser data
                    browserData.push({
                        name: categories[i],
                        y: data[i].y,
                        color: data[i].color
                    });
                    // add version data
                    drillDataLen = data[i].drilldown.data.length;
                    for (j = 0; j < drillDataLen; j += 1) {
                        brightness = 0.2 - (j / drillDataLen) / 5;
                        versionsData.push({
                            name: data[i].drilldown.categories[j],
                            y: data[i].drilldown.data[j],
                            color: Highcharts.Color(data[i].color).brighten(brightness).get()
                        });
                    }
                }
                this.options.series = [
                    {
                        name: 'Versions',
                        data: versionsData,
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
                        data: browserData,
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
                        data: browserData,
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
            },
            seriesChanged: function() {
                this.options.series = this.series
                var chart = new Highcharts.Chart(this.options);
            }
        });
    </script>
</polymer-element>
</body>
</html>