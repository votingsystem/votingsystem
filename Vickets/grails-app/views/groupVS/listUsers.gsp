<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/uservs-list']"/>">
</head>
<body>
<div class="pageContentDiv">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
        <li class="active"><g:message code="groupvsUserListLbl"/></li>
    </ol>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <h3><div class="pageHeader text-center">
        <g:message code="groupvsUserListPageHeader"/> '${subscriptionMap?.groupName}'</div>
    </h3>
    <uservs-list id="userList" url="${createLink(controller: 'groupVS', action: 'listUsers')}/${subscriptionMap?.id}"
               userURLPrefix="user" menuType="${params.menu}"></uservs-list>
</div>
</body>
</html>
<asset:script>
</asset:script>