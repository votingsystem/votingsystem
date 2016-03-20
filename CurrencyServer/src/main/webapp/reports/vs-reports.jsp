<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.currency.messages" var="bundle"/>

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
                    <a href="${contextURL}/spa.xhtml#!/rest/reports${spa.now()}/week">
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
                <div style="margin:0 0 20px 0;">
                    <fmt:message key="nativeClientURLMsg" bundle="${bundle}">
                        <fmt:param value="${contextURL}/tools/NativeClient.zip"/>
                    </fmt:message>
                </div>
                <div style="font-size: 0.8em;">
                    <fmt:message key="javaRequirementsMsg" bundle="${bundle}">
                        <fmt:param value="http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"/>
                    </fmt:message>
                </div>
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
