<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/uservs-data']"/>">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/uservs-data-dialog']"/>">
</head>
<body>
<div class="pageContentDiv" style="max-width: 1000px; padding:0px 30px 0px 30px;">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li class="active"><g:message code="usersvsPageLbl"/></li>
    </ol>
    <div class="pageContentDiv" style="max-width: 1000px; padding: 20px;">
        <uservs-data id="userData" uservs="${uservsMap as grails.converters.JSON}"></uservs-data>
    </div>
</div>

</body>
</html>
<asset:script>
</asset:script>
<asset:deferredScripts/>