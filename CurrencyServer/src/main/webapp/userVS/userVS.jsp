<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${config.webURL}/userVS/uservs-data.vsp" rel="import"/>
</head>
<body>
    <div class="pageContentDiv" style="max-width: 900px;">
        <uservs-data userVSData='${uservsMap}' messageToUser="${messageToUser}"></uservs-data>
    </div>
</body>
</html>