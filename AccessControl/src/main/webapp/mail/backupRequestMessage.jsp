<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.accesscontrol.messages" var="bundle"/>
<html>
    <head>
        <title>${msg.backupRequestCaption}</title>
        <style type="text/css" media="screen"></style>
    </head>
    <body>
        <fmt:message key="downloadEventVSBackupMsg" bundle="${bundle}">
            <fmt:param value="${fromUser}"/>
            <fmt:param value="${requestURL}"/>
            <fmt:param value="${subject}"/>
        </fmt:message>
		<br/><br/>
        <fmt:message key="downloadFileLinkMsg" bundle="${bundle}">
            <fmt:param value="${downloadURL}"/>
        </fmt:message>
    </body>
</html>
