<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-search.gsp']"/>">
</head>
<body>
<div class="pageContentDiv">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li class="active"><g:message code="userSearchPageTitle"/></li>
    </ol>

    <div layout vertical center>
        <div id="searchPanel" class="" style="background:#ba0011; padding:10px 10px 10px 10px; width: 320px; margin:0px auto 0px auto;border-radius: 5px;">
            <input id="userSearchInput" type="text" placeholder="<g:message code="userSearchLbl" />" class="form-control"
                   style="width:220px; border-color: #f9f9f9;display:inline; vertical-align: middle;" onKeyPress="searchInputKeyPress(event)">
            <i onclick="processSearch()" class="fa fa-search text-right navBar-vicket-icon"
               style="margin:0px 0px 0px 15px; display:inline;vertical-align: middle;"></i>
        </div>
        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
        <uservs-search id="uservsTable" style="width:800px; margin:10px 0px 0px 0px;"></uservs-search>
    </div>

</div>
</body>

</html>
<asset:script>

    document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
        if(document.querySelector("#navBar") != null) {
            document.querySelector("#navBar").url = "${createLink(controller: 'userVS')}/" + e.detail.id + "?menu=" + menuType
        } else window.location.href = "${createLink(controller: 'userVS')}/" + e.detail.id + "?menu=" + menuType
    });

    function searchInputKeyPress(e){
        var chCode = ('charCode' in e) ? e.charCode : e.keyCode;
        if (chCode == 13) {
            processSearch()
        }
    }

    function processSearch() {
        var textToSearch = document.querySelector("#userSearchInput").value
        if(textToSearch.trim() == "") return
        document.querySelector("#uservsTable").url = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
    }
</asset:script>
<asset:deferredScripts/>