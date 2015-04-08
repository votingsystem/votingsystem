<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${elementURL}/certificateVS/cert-list.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.certsPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv">
    <cert-list id="certList" url="${restURL}/certificateVS/certs"></cert-list>
</div>
</body>
</html>
<script>
</script>