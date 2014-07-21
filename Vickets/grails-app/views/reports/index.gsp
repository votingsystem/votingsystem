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
    <div class="pageContenDiv" style="max-width:1000px; margin: 0px auto 0px auto;padding:20px 30px 0px 30px;">
        <g:each in="${periods}">
            <a href="<g:createLink controller="reports" action="forWeek"/>?date=${formatter.format(it.getDateFrom())}">
                <g:message code="weekFromLbl"/> ${formatDate(date:it.getDateFrom(), formatName:'webViewDateFormat')} <g:message code="to_Lbl"/>
                ${formatDate(date:it.getDateTo(), formatName:'webViewDateFormat')}</a>
        </g:each>
    </div>
</body>
</html>