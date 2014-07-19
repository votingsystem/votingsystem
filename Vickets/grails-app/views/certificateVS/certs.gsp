<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/certificateVS/cert-list.gsp']"/>">
    <style type="text/css" media="screen"></style>
</head>
<body>
<div class="pageContenDiv" style="max-width: 1300px; margin: 0px auto 0px auto;">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="certsPageTitle"/></li>
        </ol>
    </div>

    <cert-list id="certList" url="<g:createLink controller="certificateVS" action="certs"/>"></cert-list>

</div>
</body>
</html>
<asset:script>
</asset:script>