<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vs-reports">
    <template>
        <div class="layout vertical center center-justified" style="max-width:1000px; padding:20px 30px 0px 30px;">
            <div>
                <div style="margin:20px 0px;">
                    <a href="/userVS/bankVSList">
                        ${msg.bankVSListLbl}
                    </a>
                </div>
                <div style="margin:20px 0px;">
                    <a href="#" onclick="javascript:page.show(contextURL + '/rest/currency/issued/currencyCode/EUR'); return false;">
                        ${msg.currencyIssued}
                    </a>
                </div>
                <div style="margin:20px 0px;">
                    <a href="#" onclick="javascript:page.show('/currencyAccount/system'); return false;">
                        ${msg.systemBalanceLbl}
                    </a>
                </div>

                <div style="margin:20px 0px;">
                    <a href="#"  onclick="javascript:page.show(contextURL + '/rest/reports${spa.now()}/week'); return false;">
                        ${msg.currentWeekLbl}
                    </a>
                </div>
                <div><b>${msg.historyLbl}:</b></div>
                <template is="dom-repeat" items="{{reportsInfoDto}}" as="timePeriod">
                    <div>
                        <a href="{{getPeriodReportURL(timePeriod)}}">{{getPeriodDescription(timePeriod)}}</a>
                    </div>
                </template>
            </div>
        </div>
    </template>
<script>
    Polymer({
        is:'vs-reports',
        properties: {
            url:{type:String, value: contextURL + '/rest/reports', observer:'getHTTP'},
            reportsInfoDto: {type:Object}
        },
        ready: function() { },
        getPeriodDescription: function(timePeriod) {
            return new Date(timePeriod.dateFrom).format()
        },
        getPeriodReportURL: function(timePeriod) {
            var dateFrom = new Date(timePeriod.dateFrom)
            return contextURL + "/rest/reports/" + dateFrom.format() + "/week"
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.reportsInfoDto = toJSON(rawData.response)
            }.bind(this));
        }
    });
</script>
</dom-module>
