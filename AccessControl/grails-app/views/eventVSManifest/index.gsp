<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/search-info.gsp']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-advanced-search-dialog', file: 'votingsystem-advanced-search-dialog.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSManifest/eventvs-manifest-list']"/>">
</head>
<body>
    <votingsystem-innerpage-signal title="<g:message code="manifestSystemLbl"/>"></votingsystem-innerpage-signal>
    <div class="pageContentDiv">
        <search-info id="searchInfo"></search-info>
        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
        <eventvs-manifest-list id="eventvsList" url="${createLink(controller: 'eventVSClaim', action: 'index')}?menu=${params.menu}&eventVSState=ACTIVE"
                               eventvstype="claim"></eventvs-manifest-list>
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