<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${config.webURL}/currency/currency-wallet.vsp" rel="import"/>
</head>
<body>
    <vs-innerpage-signal caption="${msg.currencyWalletLbl}"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <currency-wallet></currency-wallet>
    </div>
</body>
</html>