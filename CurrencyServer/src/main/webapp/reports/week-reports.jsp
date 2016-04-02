<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="week-reports">
    <template>
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
            url:{type:String, observer:'getHTTP'}
        },
        ready: function() {
            var options = { mode: 'code', modes: ['code', 'form', 'text', 'tree', 'view'], // allowed modes
                error: function (err) { alert(err.toString());}
            };
            console.log("ready - reportsDto: ", this.reportsDto)
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            vs.getHTTPJSON(targetURL, function(responseText){
                this.reportsDto = toJSON(responseText)
            }.bind(this))
        }
    });
</script>
</dom-module>