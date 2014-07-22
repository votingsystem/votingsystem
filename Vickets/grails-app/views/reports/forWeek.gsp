<!DOCTYPE html>
<html>
<head>
    <title><g:message code="weekReportsPageTitle"/></title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/week-reports.gsp']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/google-chart', file: 'google-chart.html')}">
</head>
<body>
    <div class="pageContenDiv" style="max-width:1000px; margin: 20px auto 0px auto;">
        <week-reports url="${createLink(controller: 'reports', action: 'forWeek')}?date=${date}"></week-reports>
    </div>
</body>
</html>