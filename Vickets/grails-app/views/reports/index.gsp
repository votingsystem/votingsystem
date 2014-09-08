<%@ page import="java.text.DateFormat; java.text.SimpleDateFormat" %>

<%
    DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
%>
<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<body>
    <div class="pageContentDiv" style="max-width:1000px; margin: 0px auto 0px auto;padding:20px 30px 0px 30px;">

        <g:each in="${periods}">
            <div>
                <a href="<g:createLink controller="reports" action="forWeek"/>?date=${formatter.format(it.getDateFrom())}">
                    <g:message code="weekFromLbl" args="${[formatDate(date:it.getDateFrom(), formatName:'webViewDateFormat'),
                                                           formatDate(date:it.getDateTo(), formatName:'webViewDateFormat')]}"/>
                </a>
            </div>
        </g:each>
        <div style="margin:20px 0px;">
            <a href="<g:createLink controller="reports" action="forWeek"/>?date=${formatter.format(Calendar.getInstance().getTime())}">
                <g:message code="currentWeekLbl"/>
            </a>
        </div>
    </div>
</body>
</html>