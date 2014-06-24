<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
    <asset:javascript src="jquery.stickytableheaders.js"/>
    <asset:javascript src="jquery.dynatable.js"/>
    <asset:stylesheet src="jquery.dynatable.css"/>
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
                <li class="active"><g:message code="groupvsUserListLbl"/></li>
            </ol>
        </ol>
    </div>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <h3><div class="pageHeader text-center">
        <g:message code="groupvsUserListPageHeader"/> '${subscriptionMap?.groupName}'</div>
    </h3>
    <g:include view="/include/userList.gsp"/>
</div>
</body>
</html>
<asset:script>
    var userListURL = "${createLink(controller: 'groupVS', action: 'listUsers')}/${subscriptionMap?.id}"
        var userBaseURL = "user"

    $(function() {
        $("#navBarSearchInput").css( "visibility", "visible" );
        $("#advancedSearchButton").css( "display", "none" );

    })

    function processUserSearch(textToSearch) {
        $("#pageInfoPanel").text("<g:message code="searchResultLbl"/> '" + textToSearch + "'")
        $('#pageInfoPanel').css("display", "block")
        dynatable.settings.dataset.ajaxUrl= targetURL + "?searchText=" + textToSearch
        dynatable.paginationPage.set(1);
        dynatable.process();
    }

</asset:script>