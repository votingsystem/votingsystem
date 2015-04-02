<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.currency.messages" var="bundle"/>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%
    DateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd");
%>
<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>

</head>
<body>
    <vs-innerpage-signal caption="${msg.reportsPageTitle}"></vs-innerpage-signal>
    <div horizontal layout>
        <div vertical layout class="pageContentDiv" style="max-width:1000px; padding:20px 30px 0px 30px;">
            <div style="margin:20px 0px;">
                <a href="${config.restURL}/userVS/bankVSList">
                    ${msg.bankVSListLbl}
                </a>
            </div>
            <c:forEach var="it" items="${periods}" varStatus="counter">
                <div>
                    <a href="${config.restURL}/reports/week${formatter.format(it.getDateFrom())}">
                        <fmt:message key="transactionsCurrentWeekPeriodMsg" bundle="${bundle}">
                            <fmt:param value="${formatter.format(it.getDateFrom())}"/>
                        </fmt:message>
                    </a>
                </div>
            </c:forEach>
            <div style="margin:20px 0px;">
                <a href="${config.restURL}/reports/week${formatter.format(Calendar.getInstance().getTime())}">
                    ${msg.currentWeekLbl}
                </a>
            </div>
        </div>
    </div>
</body>
</html>