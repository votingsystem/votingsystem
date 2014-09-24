<!DOCTYPE html>
<html>
<head>
    <title><g:message code="weekReportsPageTitle"/></title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/reports/week-reports']"/>">
</head>
<body>
    <div class="pageContentDiv" style="max-width:1000px; margin: 20px auto 0px auto;">
        <week-reports url="${createLink(controller: 'reports', action: 'week')}?date=${date}"></week-reports>
    </div>
</body>
</html>