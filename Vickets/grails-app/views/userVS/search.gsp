<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/userVS/uservs-selector']"/>">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="userSearchPageTitle"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv">
    <div layout vertical center>
        <div id="searchPanel" class="" style="background:#ba0011; padding:10px 10px 10px 10px; width: 320px; margin:0px auto 0px auto;border-radius: 5px;">
            <input id="userSearchInput" type="text" placeholder="<g:message code="userSearchLbl" />" class="form-control"
                   style="width:220px; border-color: #f9f9f9;display:inline; vertical-align: middle;" onKeyPress="searchInputKeyPress(event)">
            <i onclick="processSearch()" class="fa fa-search text-right navBar-vicket-icon"
               style="margin:0px 0px 0px 15px; display:inline;vertical-align: middle;"></i>
        </div>
        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
        <uservs-selector id="userVSSelector" style="width:800px; margin:10px 0px 0px 0px;"></uservs-selector>
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
        document.querySelector("#userVSSelector").url = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
    }
</asset:script>
<asset:deferredScripts/>