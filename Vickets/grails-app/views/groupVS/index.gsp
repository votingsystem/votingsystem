<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <asset:stylesheet src="vickets_groupvs.css"/>
    <link rel="import" href="${resource(dir: '/bower_components/core-animated-pages', file: 'core-animated-pages.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-list']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
</head>
<body>
<div class="pageContenDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="groupvsLbl"/></li>
        </ol>
    </div>
    <div style="display: table;width:90%;vertical-align: middle;margin:0px 0 10px 0px;">
        <div style="display:table-cell;margin: auto; vertical-align: top;">
            <select id="groupvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                    class="form-control" onchange="groupvsTypeSelect(this)">
                <option value="ACTIVE"  style="color:#59b;"> - <g:message code="selectActiveGroupvsLbl"/> - </option>
                <option value="PENDING" style="color:#fba131;"> - <g:message code="selectPendingGroupvsLbl"/> - </option>
                <option value="CANCELLED" style="color:#cc1606;"> - <g:message code="selectClosedGroupvsLbl"/> - </option>
            </select>
        </div>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <groupvs-list id="groupvsList" url="${createLink(controller: 'groupVS')}?menu=${params.menu}"></groupvs-list>

</div>
</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        document.querySelector("#navBar").searchVisible('false')
    });

    function groupvsTypeSelect(selected) {
        var optionSelected = selected.value
        console.log("certTypeSelect: " + optionSelected)
        if("" != optionSelected) {
            targetURL = "${createLink(controller: 'groupVS')}?menu=" + menuType;
            history.pushState(null, null, targetURL);
            document.querySelector("#groupvsList").url = targetURL
        }
    }

    function processUserSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = 'block'
        document.querySelector("#groupvsList").url = "${createLink(controller: 'search', action: 'groupVS')}?searchText=" + textToSearch
    }

</asset:script>