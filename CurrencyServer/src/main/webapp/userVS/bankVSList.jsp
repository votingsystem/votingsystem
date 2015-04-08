<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${elementURL}/userVS/bankVS-list.vsp" rel="import"/>
</head>
<body>
<div class="pageContentDiv">
    <vs-innerpage-signal caption="${msg.bankVSListLbl}"></vs-innerpage-signal>
    <bankVS-list bankVSMap='${bankVSMap}'></bankVS-list>
</div>
</body>
</html>