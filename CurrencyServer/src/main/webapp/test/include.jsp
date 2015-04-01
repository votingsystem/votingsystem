<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="conf" value="${app}" />
<html>
<head>

    <jsp:include page="/include/utils_js.jsp"/>
</head>
<body>
<h2>--- i18N: ${msg.daysLbl}</h2>
<h2>--- params.mode: ${param['mode']}</h2>
</body>
</html>