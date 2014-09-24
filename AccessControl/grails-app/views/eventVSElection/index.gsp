<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-advanced-search-dialog', file: 'votingsystem-advanced-search-dialog.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/search-info.gsp']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election-list']"/>">
</head>
<body>
<div class="pageContentDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li class="active"><g:message code="electionSystemLbl"/></li>
    </ol>
    <search-info id="searchInfo"></search-info>
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
    background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
    <eventvs-election-list id="eventvsList" url="${createLink(controller: 'eventVSElection', action: 'index')}?menu=${params.menu}&eventVSState=ACTIVE"
                  eventvstype="election"></eventvs-election-list>
</div>
<votingsystem-advanced-search-dialog id="advancedSearchDialog"></votingsystem-advanced-search-dialog>
</body>
</html>
<asset:script>
    function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        var ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=ELECTION"
    }

    function processSearchJSON(jsonData) {
        var ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}";
    }
</asset:script>