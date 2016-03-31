<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vs-reports">
    <template>
        <div class="layout vertical center center-justified" style="max-width:1000px; padding:20px 30px 0px 30px; margin: 0 auto">
            <div>
                <h2 style="text-decoration: underline;">${msg.reportsLbl}</h2>
                <div style="margin:20px 0px;">
                    <a href="/user/bankList">
                        ${msg.bankListLbl}
                    </a>
                </div>
                <div style="margin:20px 0px;">
                    <a href="${contextURL}/spa.xhtml#!/currencyIssued?currencyCode=EUR">
                        ${msg.currencyIssued}
                    </a>
                </div>
                <div style="margin:20px 0px;">
                    <a href="${contextURL}/spa.xhtml#!/currencyAccount/system">
                        ${msg.systemBalanceLbl}
                    </a>
                </div>

                <div style="margin:20px 0px;">
                    <a href="${contextURL}/spa.xhtml#!/reports/week?url=${contextURL}/rest/reports${spa.now()}/week">
                        ${msg.currentWeekLbl}
                    </a>
                </div>
                <div hidden="{{historyMsgHidden}}"><b>${msg.historyLbl}:</b></div>
                <template is="dom-repeat" items="{{reportsInfoDto}}" as="timePeriod">
                    <div>
                        <a href="{{getPeriodReportURL(timePeriod)}}">{{getPeriodDescription(timePeriod)}}</a>
                    </div>
                </template>
            </div>
            <div class="layout vertical center center-justified" style="margin:30px auto;">
                <h2 style="text-decoration: underline;">${msg.toolsLbl}</h2>
                <a class="buttonvs" style="width: 280px; margin: 10px 0 0 0; font-size: 1.1em;" href="${contextURL}/tools/NativeClient.zip">
                    <i class="fa fa-download"></i> ${msg.validationToolMsg}
                </a>
                <a href="http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk">
                    ${msg.javaRequirementsMsg}
                </a>
            </div>
        </div>
    </template>
<script>
    Polymer({
        is:'vs-reports',
        properties: {
            url:{type:String, value: vs.contextURL + '/rest/reports', observer:'getHTTP'},
            reportsInfoDto: {type:Object}
        },
        ready: function() { },
        getPeriodDescription: function(timePeriod) {
            return new Date(timePeriod.dateFrom).format()
        },
        getPeriodReportURL: function(timePeriod) {
            var dateFrom = new Date(timePeriod.dateFrom)
            return vs.contextURL + "/rest/reports/" + dateFrom.format() + "/week"
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                this.reportsInfoDto = toJSON(rawData.response)
                this.historyMsgHidden = (this.reportsInfoDto.length === 0)
            }.bind(this));
        }
    });
</script>
</dom-module>
