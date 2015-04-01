<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${config.webURL}/app/uservs-dashboard.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.dashBoardLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width: 1300px;">
    <uservs-dashboard dataMap='${dataMap}'></uservs-dashboard>
</div>
</body>
</html>
