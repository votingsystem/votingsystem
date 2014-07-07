<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="userSearchPageTitle"/></li>
        </ol>
    </div>

    <g:include view="/include/search-user.gsp"/>
    <div layout vertical center>
        <div id="searchPanel" class="" style="background:#ba0011; padding:10px 10px 10px 10px; width: 300px; margin:0px auto 0px auto;">
            <input id="userSearchInput" type="text" class="form-control" placeholder="<g:message code="userSearchLbl" />"
                   style="width:220px; border-color: #f9f9f9;display:inline; vertical-align: middle;">
            <i id="searchPanelCloseIcon" onclick="processUserSearch()" class="fa fa-search text-right navBar-vicket-icon"
               style="margin:0px 0px 0px 15px; display:inline;vertical-align: middle;"></i>
        </div>
        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
        <search-user id="uservsTable" style="width:1000px;"></search-user>
    </div>

</div>
</body>

</html>
<asset:script>
    document.querySelector("#uservsTable").addEventListener('user-clicked', function(e) {
        window.location.href ="${createLink(controller: 'userVS')}/" + e.detail.userId + "?menu=" + menuType
    });

    document.querySelector("#userSearchInput").onkeypress = function(event){
        var chCode = ('charCode' in event) ? event.charCode : event.keyCode;
        if (chCode == 13) {
            processUserSearch()
        }
    }

    function processUserSearch() {
        var textToSearch = document.querySelector("#userSearchInput").value
        if(textToSearch.trim() == "") return
        document.querySelector("#uservsTable").url = "${createLink(controller: 'userVS', action: 'search')}?searchText=" + textToSearch
    }
</asset:script>