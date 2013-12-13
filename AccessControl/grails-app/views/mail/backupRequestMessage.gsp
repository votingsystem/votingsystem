<%@ page contentType="text/html" %>
<%@page pageEncoding="ISO-8859-1"%>
<html>
    <head>
        <title><g:message code="backupRequestCaption"/></title>
        <style type="text/css" media="screen"></style>
    </head>
    <body>
        <g:message code="downloadEventVSBackupMsg" args="${[fromUser, requestURL, subject]}"/>
		<br/><br/>
        <g:message code="downloadFileLinkMsg" args="${[downloadURL]}"/>
    </body>
</html>
