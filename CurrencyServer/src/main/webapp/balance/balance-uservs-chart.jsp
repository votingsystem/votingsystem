<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/google-chart/google-chart.html" rel="import">

<dom-module name="balance-user-chart">
    <template>
        <div class="horizontal layout center center-justified">
            <google-chart id="chart" type='column'>
            </google-chart>
        </div>
    </template>
    <script>
        Polymer({
            is:'balance-user-chart',
            properties: {
                xAxisCategories: {type:Array, value: []},
                series:{type:Object, value: {}},
                yAxisTitle:{type:String},
                caption:{type:String}
            },
            ready: function() {
                this.$.chart.options.title = this.caption
            },
            seriesChanged: function() {
                this.options.series = this.series

            }
        });

    </script>
</dom-module>
