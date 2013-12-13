<%@ page contentType="text/html" %>
<%@page pageEncoding="UTF-8"%>
<html>
    <head>
        <title>${pageTitle}</title>
        <style type="text/css" media="screen">
        </style>
    </head>
    <body>
    <% def bodyParams = [fromUser, requestURL, representative, dateStr, urlDescarga]%>
	<g:message code="representativeAccreditationsMailBody" args="${bodyParams}"/>
    </body>
</html>
