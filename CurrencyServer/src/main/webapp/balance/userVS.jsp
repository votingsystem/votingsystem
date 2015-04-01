<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${config.webURL}/balance/balance-uservs.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.balanceLbl}"></vs-innerpage-signal>
    <div style="margin: 0 auto;">
        <balance-uservs balance="${balanceMap}"></balance-uservs>
    </div>
</body>
</html>