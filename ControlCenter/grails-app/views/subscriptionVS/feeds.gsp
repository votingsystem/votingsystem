<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
    <div style="margin: 30px auto;"><votingSystem:feed href="${createLink(controller:'subscriptionVS', action:'elections')}">
        <g:message code="subscribeToFeedsLbl"/></votingSystem:feed>
    </div>
</body>
</html>