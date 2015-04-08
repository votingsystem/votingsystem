<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <meta name="layout" content="main"/>
    <title>${msg.appTitle}</title>
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContent" style="position:relative;">
    <div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">
        <div style="font-size: 1.5em;margin-bottom: 30px;">${msg.selectVotingAppAndroidMsg}</div>
        <a href="" onclick="return goBack();" style="font-size: 1.5em;">${msg.retryLbl}</a>
    </div>
</div>
</body>
<script>
    function goBack() {
        window.history.back()
        return false
    }
</script>
</html>