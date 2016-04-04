<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vs-reports">
    <template>
        <div class="pagevs vertical layout center">
            <div style="margin: 0 auto;">
                <h3 class="sectionHeader">${msg.reportsLbl}</h3>
                <div style="margin:0px 0px;">
                    <a class="buttonvs" style="display: block;width: 150px;" href="/user/bankList">
                        ${msg.bankListLbl}
                    </a>
                </div>
                <div style="margin:20px 0px;">
                    <a class="buttonvs" style="display: block;width: 150px;" href="${contextURL}/spa.xhtml#!/currencyIssued?currencyCode=EUR">
                        ${msg.currencyIssued}
                    </a>
                </div>
                <div style="margin:20px 0px;">
                    <a class="buttonvs" style="display: block;width: 150px;" href="${contextURL}/spa.xhtml#!/currencyAccount/system">
                        ${msg.systemBalanceLbl}
                    </a>
                </div>

                <div style="margin:20px 0px;">
                    <a class="buttonvs"  style="display: block;width: 150px;"
                        href="${contextURL}/spa.xhtml#!/reports/week?url=/rest/reports${spa.now()}/week">
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
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            vs.getHTTPJSON(targetURL, function(responseText){
                this.reportsInfoDto = toJSON(responseText)
                this.historyMsgHidden = (this.reportsInfoDto.length === 0)
            }.bind(this))
        }
    });
</script>
</dom-module>
