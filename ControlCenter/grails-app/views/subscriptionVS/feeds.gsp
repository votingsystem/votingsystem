<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<body>
    <div style="margin: 30px auto;">
        <a href="${createLink(controller:'subscriptionVS', action:'elections')}">
            <i class="fa fa-rss-square" style="margin:3px 0 0 10px; color: #f0ad4e;"></i> <g:message code="subscribeToFeedsLbl"/></a>
    </div>
</body>
</html>