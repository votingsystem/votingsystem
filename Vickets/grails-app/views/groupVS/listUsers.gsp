<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/user-list.gsp']"/>">
</head>
<body>
<div class="pageContenDiv">
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
    <user-list id="userList" url="${createLink(controller: 'groupVS', action: 'listUsers')}/${subscriptionMap?.id}"
               userURLPrefix="user" menuType="${params.menu}"></user-list>
</div>
</body>
</html>
<asset:script>

    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
        $("#advancedSearchButton").css( "display", "none" );

    })

    function processSearch(textToSearch) {
        $("#pageInfoPanel").text("<g:message code="searchResultLbl"/> '" + textToSearch + "'")
        $('#pageInfoPanel').css("display", "block")
        document.querySelector("#userList").url = targetURL + "?searchText=" + textToSearch
    }

</asset:script>