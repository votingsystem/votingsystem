<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <asset:stylesheet src="vickets_groupvs.css"/>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-list']"/>">
</head>
<body>
<div class="pageContenDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="groupvsLbl"/></li>
        </ol>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <groupvs-list id="groupvsList" url="${createLink(controller: 'groupVS')}?menu=${params.menu}"></groupvs-list>

</div>
</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        if(document.querySelector("#navBar") != null) document.querySelector("#navBar").searchVisible('false')
    });

    function processUserSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = 'block'
        document.querySelector("#groupvsList").url = "${createLink(controller: 'search', action: 'groupVS')}?searchText=" + textToSearch
    }
</asset:script>