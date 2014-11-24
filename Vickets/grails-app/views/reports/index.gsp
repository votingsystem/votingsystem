<%@ page import="java.text.DateFormat; java.text.SimpleDateFormat" %>

<%
    DateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd");
%>
<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="reportsPageTitle"/>"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width:1000px; margin: 0px auto 0px auto;padding:20px 30px 0px 30px;">
        <div style="margin:20px 0px;">
            <a href="<g:createLink controller="userVS" action="bankVSList"/>">
                <g:message code="bankVSListLbl"/>
            </a>
        </div>
        <g:each in="${periods}">
            <div>
                <a href="<g:createLink controller="reports" action="week"/>${formatter.format(it.getDateFrom())}">
                    <g:message code="transactionsCurrentWeekPeriodMsg"
                               args="${[formatDate(date:it.getDateFrom(), formatName:'webViewDateFormat')]}"/>
                </a>
            </div>
        </g:each>
        <div style="margin:20px 0px;">
            <a href="<g:createLink controller="reports" action="week"/>${formatter.format(Calendar.getInstance().getTime())}">
                <g:message code="currentWeekLbl"/>
            </a>
        </div>
    </div>
</body>
</html>