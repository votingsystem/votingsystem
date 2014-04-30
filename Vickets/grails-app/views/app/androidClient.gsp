<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="appTitle"/></title>
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContent" style="position:relative;">
    <div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">
        <div style="font-size: 1.5em;margin-bottom: 30px;"><g:message code="selectVotingAppAndroidMsg"/></div>
        <a href="" onclick="return goBack();" style="font-size: 1.5em;"><g:message code="retryLbl"/></a>
    </div>
</div>
</body>
<r:script>
    function goBack() {
        window.history.back()
        return false
    }
</r:script>
</html>