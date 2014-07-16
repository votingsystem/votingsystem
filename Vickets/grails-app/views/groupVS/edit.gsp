<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-editor']"/>">
</head>
<body>
    <div id="contentDiv" class="pageContenDiv" style="min-height: 1000px; margin:0px auto 0px auto; padding:0px 30px 0px 30px;">
        <div class="row">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
                <li class="active"><g:message code="editGroupVSLbl"/></li>
            </ol>
        </div>
        <groupvs-editor groupvs='${groupvsMap as grails.converters.JSON}'></groupvs-editor>
    </div>
</body>
</html>
<asset:script>

</asset:script>