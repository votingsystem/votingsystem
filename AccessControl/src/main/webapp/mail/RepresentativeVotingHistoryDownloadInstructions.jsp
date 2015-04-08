<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.accesscontrol.messages" var="bundle"/>
<html>
    <head>
        <title>${pageTitle}</title>
        <style type="text/css" media="screen">
        </style>
    </head>
    <body>
    <fmt:message key="representativeVotingHistoryMailBody" bundle="${bundle}">
        <fmt:param value="${fromUser}"/>
        <fmt:param value="${requestURL}"/>
        <fmt:param value="${representative}"/>
        <fmt:param value="${dateFromStr}"/>
        <fmt:param value="${dateToStr}"/>
        <fmt:param value="${downloadURL}"/>
    </fmt:message>
    </body>
</html>
