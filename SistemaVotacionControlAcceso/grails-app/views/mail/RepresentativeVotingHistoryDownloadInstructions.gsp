<%@ page contentType="text/html" %>
<%@page pageEncoding="UTF-8"%>
<html>
    <head>
        <title>${pageTitle}</title>
        <style type="text/css" media="screen">
        </style>
    </head>
    <body>
        <% def bodyParams = [solicitante, urlSolicitud, representative, dateFromStr, dateToStr, urlDescarga]%>
		<g:message code="representativeVotingHistoryMailBody" args="${bodyParams}"/>
    </body>
</html>
