<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${config.webURL}/userVS/uservs-selector.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.userSearchPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div layout vertical center>
        <uservs-selector id="userVSSelector" style="width:800px; margin:10px 0px 0px 0px;" contactSelector></uservs-selector>
    </div>
</div>
</body>
</html>
<script>
    document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
        if(document.querySelector("#navBar") != null) {
            document.querySelector("#navBar").url = "${config.restURL}/userVS/" + e.detail.id + "?menu=" + menuType
        } else window.location.href = "${config.restURL}/userVS/" + e.detail.id + "?menu=" + menuType
    });
</script>
