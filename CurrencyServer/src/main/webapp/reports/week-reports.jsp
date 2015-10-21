<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/jsoneditor.html" rel="import"/>

<dom-module name="week-reports">
    <template>
        <iron-ajax auto id="ajax" url="{{url}}" last-response="{{reportsDto}}" handle-as="json" content-type="application/json"></iron-ajax>

        <div class="layout vertical center center-justified" style="max-width:1000px; padding:20px 30px 0px 30px;">
            <div class="pageHeader" style="margin:0px auto; text-align: center;">
            ${msg.periodLbl}: ${spa.formatDate(timePeriod.dateFrom)} - ${spa.formatDate(timePeriod.dateTo)}
            </div>
            <div id="reportsDiv" style="width: 500px; height: 300px;"></div>
        </div>
    </template>
<script>
    Polymer({
        is:'week-reports',
        properties: {
            reportsDto: {type:Object},
            url: {type:String}
        },
        ready: function() {
            var options = { mode: 'code', modes: ['code', 'form', 'text', 'tree', 'view'], // allowed modes
                error: function (err) { alert(err.toString());}
            };
            var reportsDiv = new JSONEditor(this.$.reportsDiv);
            reportsDiv.set(this.reportsDto);
            reportsDiv.expandAll();
        }
    });
</script>
</dom-module>