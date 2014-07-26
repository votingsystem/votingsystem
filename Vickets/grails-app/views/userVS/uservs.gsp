<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/uservs-data.gsp']"/>">
</head>
<body>
<div class="pageContenDiv" style="max-width: 1000px; padding:0px 30px 0px 30px;">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li class="active"><g:message code="usersvsPageLbl"/></li>
    </ol>
    <div class="pageContenDiv" style="max-width: 1000px; padding: 20px;">
        <uservs-data id="userData" uservs="${uservsMap as grails.converters.JSON}"></uservs-data>
    </div>
</div>

</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        console.log("main.gsp - polymer-ready")
        document.querySelector("#userData").opened = true
    });
</asset:script>
<asset:deferredScripts/>