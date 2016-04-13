<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vs-reports">
    <template>
        <div class="pagevs vertical layout center">
            <div class="layout vertical center" style="margin: 0 auto;">
                <h3 class="sectionHeader">${msg.reportsLbl}</h3>
                <div style="margin:0 0 0 0;">
                    <a class="buttonvs" style="display: block;width: 150px;" href="${contextURL}#!/transactionsDashboard">
                        <i class="fa fa-tachometer"></i> ${msg.dashBoardLbl}
                    </a>
                </div>
                <div style="margin:10px 0 0 0;">
                    <a class="buttonvs" style="display: block;width: 150px;" href="${contextURL}#!/user/bankList">
                        ${msg.bankListLbl}
                    </a>
                </div>
                <div style="margin:10px 0 0 0;">
                    <a class="buttonvs" style="display: block;width: 150px;" href="${contextURL}#!/currencyIssued">
                        ${msg.currencyIssued}
                    </a>
                </div>
                <div style="margin:10px 0 0 0;">
                    <a class="buttonvs" style="display: block;width: 150px;" href="${contextURL}#!/currencyAccount/system">
                        ${msg.systemBalanceLbl}
                    </a>
                </div>
                <div style="margin:10px 0 15px 0;">
                    <a class="buttonvs" style="display: block;width: 150px;"
                       href="${contextURL}#!/reports/week?url=/rest/reports${spa.now()}/week">
                        ${msg.currentWeekLbl}
                    </a>
                </div>
                <h3 hidden="{{historyMsgHidden}}" class="sectionHeader">${msg.previousWeeksLbl}</h3>
                <div class="layout flex horizontal wrap around-justified">
                    <template is="dom-repeat" items="{{reportsInfoDto}}" as="timePeriod">
                        <a class="buttonvs" style="display: block;width: 110px;margin:5px 5px;" target="_blank"
                           href="{{getPeriodReportURL(timePeriod)}}">{{getPeriodDescription(timePeriod)}}</a>
                    </template>
                </div>
            </div>
            <div class="layout vertical center center-justified" style="margin:30px auto;">
                <h3 class="sectionHeader">${msg.toolsLbl}</h3>
                <a class="buttonvs" style="width: 280px;" href="${contextURL}/tools/NativeClient.zip">
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
            console.log("getHTTP - targetURL: " + targetURL)
            vs.getHTTPJSON(targetURL, function(responseText){
                this.reportsInfoDto = toJSON(responseText)
                this.historyMsgHidden = (this.reportsInfoDto.length === 0)
            }.bind(this))
        }
    });
</script>
</dom-module>
