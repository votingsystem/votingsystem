<%@ page contentType="text/html" %>
<%@page pageEncoding="ISO-8859-1"%>
<html>
    <head>
        <title>${pageTitle}</title>
        <style type="text/css" media="screen">
        </style>
    </head>
    <body>
		<g:message code="representativeVotingHistoryMailBody"
                   args="${[fromUser, requestURL, representative, dateFromStr, dateToStr, downloadURL]}"/>
    </body>
</html>
