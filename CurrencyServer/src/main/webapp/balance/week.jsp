<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${elementURL}/balance/balance-weekreport.vsp" rel="import"/>
</head>
<body>
    <balance-weekreport id="balanceWeekreport" balances="${balancesJSON}"></balance-weekreport>
</body>
</html>