<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-list']"/>">
</head>
<body>
<vs-innerpage-signal title="<g:message code="groupvsUserListLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
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