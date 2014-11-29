<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/userVS/uservs-selector"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="userSearchPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div layout vertical center>
        <div horizontal layout center center-justified id="searchPanel" class="" style="background:#ba0011;
                padding:10px 10px 10px 10px; width: 320px; margin:0px auto 0px auto;border-radius: 5px;">
            <div flex>
                <input id="userSearchInput" type="text" placeholder="<g:message code="userSearchLbl" />" class="form-control"
                       style="" class="form-control" onKeyPress="searchInputKeyPress(event)">
            </div>
            <div style="margin: 0 10px 0 10px; vertical-align: bottom;">
                <i onclick="processSearch()" class="fa fa-search text-right vs-navbar-icon"
                   style="margin:0 0px 0 15px; vertical-align: middle; font-size: 1.3em;"></i>
            </div>
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